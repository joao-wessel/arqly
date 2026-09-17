#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/staging-common.sh"

usage() {
  printf 'Uso: %s <diretório-do-backup> [--yes]\n' "$0" >&2
  exit 2
}

[[ $# -ge 1 ]] || usage
BACKUP_DIR="$1"
shift
ASSUME_YES=false
[[ "${1:-}" == "--yes" ]] && ASSUME_YES=true
[[ $# -le 1 ]] || usage

load_environment
require_command docker
require_command sha256sum
require_command tar
configure_compose
if ! docker volume inspect "$(uploads_volume)" >/dev/null 2>&1; then
  compose_project="${ARQLY_COMPOSE_PROJECT:-$(basename "$ARQLY_ROOT" | tr '[:upper:]' '[:lower:]')}"
  docker volume create \
    --label "com.docker.compose.project=$compose_project" \
    --label "com.docker.compose.volume=uploads" \
    "$(uploads_volume)" >/dev/null
fi
[[ -d "$BACKUP_DIR" ]] || fail "Diretório de backup não encontrado: $BACKUP_DIR"
[[ -f "$BACKUP_DIR/manifest.txt" ]] || fail "Manifesto não encontrado"
grep -qx 'status=COMPLETE' "$BACKUP_DIR/manifest.txt" || fail "Backup não está marcado como completo"
[[ -s "$BACKUP_DIR/database.dump" && -s "$BACKUP_DIR/uploads.tar.gz" ]] || fail "Artefatos de backup ausentes"
[[ -f "$BACKUP_DIR/database.dump.sha256" && -f "$BACKUP_DIR/uploads.tar.gz.sha256" ]] || fail "Checksums ausentes"

(cd "$BACKUP_DIR" && sha256sum -c database.dump.sha256 && sha256sum -c uploads.tar.gz.sha256)
if docker_run --rm -v "$BACKUP_DIR:/backup:ro" alpine:3.20 sh -c "tar -tzf /backup/uploads.tar.gz" \
  | grep -Eq '(^/|(^|/)\.\.(/|$))'; then
  fail "Arquivo de uploads contém caminho inseguro"
fi

if [[ "$ASSUME_YES" != "true" ]]; then
  printf 'Este processo substituirá o banco e os uploads atuais. Digite RESTORE para continuar: '
  read -r confirmation
  [[ "$confirmation" == "RESTORE" ]] || fail "Restore cancelado"
fi

restore_log="$BACKUP_DIR/restore-$(date +%Y-%m-%d_%H%M%S).log"
log() { printf '%s %s\n' "$(date -Is)" "$*" | tee -a "$restore_log" >&2; }
start_epoch="$(date +%s)"
log "START database=$POSTGRES_DB uploads_volume=$(uploads_volume)"

compose up -d postgres >/dev/null
wait_for_postgres
compose stop backend frontend reverse-proxy >/dev/null 2>&1 || true

compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres \
  -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$POSTGRES_DB' AND pid <> pg_backend_pid();" >/dev/null
compose exec -T postgres dropdb -U "$POSTGRES_USER" --if-exists "$POSTGRES_DB"
compose exec -T postgres createdb -U "$POSTGRES_USER" "$POSTGRES_DB"
cat "$BACKUP_DIR/database.dump" | compose exec -T postgres pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --exit-on-error

docker_run --rm \
  -v "$(uploads_volume):/target" \
  -v "$BACKUP_DIR:/backup:ro" \
  alpine:3.20 sh -c 'find /target -mindepth 1 -maxdepth 1 -exec rm -rf -- {} + && tar -xzf /backup/uploads.tar.gz -C /target'

flyway_rows="$(compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc 'select count(*) from flyway_schema_history')"
[[ "$flyway_rows" =~ ^[0-9]+$ && "$flyway_rows" -gt 0 ]] || fail "Histórico Flyway não foi restaurado"

missing_keys="$(compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc "select storage_key from file_resources where status <> 'DELETED'" \
  | docker_run --rm -i -v "$(uploads_volume):/storage:ro" alpine:3.20 sh -c '
      missing=0
      while IFS= read -r key; do
        [ -z "$key" ] && continue
        case "$key" in /*|*".."*) echo "$key"; missing=1;; *) [ -f "/storage/$key" ] || { echo "$key"; missing=1; };; esac
      done
      exit "$missing"')" || fail "Há arquivos referenciados no banco que não foram restaurados: ${missing_keys:-desconhecido}"

compose up -d >/dev/null
log "COMPLETE duration_seconds=$(( $(date +%s) - start_epoch ))"
