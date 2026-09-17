#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/staging-common.sh"

BACKUP_LOG=""
WORK_DIR=""
START_EPOCH="$(date +%s)"

log() {
  local message="$(date -Is) $*"
  printf '%s\n' "$message" >&2
  [[ -n "$BACKUP_LOG" ]] && printf '%s\n' "$message" >> "$BACKUP_LOG"
}

on_error() {
  local code=$?
  log "FAILED duration_seconds=$(( $(date +%s) - START_EPOCH )) exit_code=$code"
  [[ -n "$WORK_DIR" && -d "$WORK_DIR" ]] && rm -rf -- "$WORK_DIR"
  exit "$code"
}
trap on_error ERR

load_environment
ensure_staging_dependencies
require_command git
require_command df

mkdir -p "$ARQLY_BACKUP_DIR"
chmod 700 "$ARQLY_BACKUP_DIR"
BACKUP_LOG="$ARQLY_BACKUP_DIR/backup.log"
touch "$BACKUP_LOG"
chmod 600 "$BACKUP_LOG"

available_kb="$(df -Pk "$ARQLY_BACKUP_DIR" | awk 'NR==2 {print $4}')"
minimum_kb="${ARQLY_BACKUP_MIN_FREE_KB:-1048576}"
[[ "$available_kb" =~ ^[0-9]+$ ]] || fail "Não foi possível verificar o espaço livre"
(( available_kb >= minimum_kb )) || fail "Espaço livre insuficiente para backup: ${available_kb}KB disponíveis, ${minimum_kb}KB exigidos"

compose up -d postgres >/dev/null
wait_for_postgres

timestamp="$(date +%Y-%m-%d_%H%M%S)"
WORK_DIR="$ARQLY_BACKUP_DIR/.${timestamp}.partial"
FINAL_DIR="$ARQLY_BACKUP_DIR/$timestamp"
[[ ! -e "$FINAL_DIR" ]] || fail "Diretório de backup já existe: $FINAL_DIR"
mkdir -p "$WORK_DIR"
chmod 700 "$WORK_DIR"

log "START database=$POSTGRES_DB uploads_volume=$(uploads_volume)"
compose exec -T postgres pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc > "$WORK_DIR/database.dump"

docker_run --rm --user "$(id -u):$(id -g)" \
  -v "$(uploads_volume):/source:ro" \
  -v "$WORK_DIR:/backup" \
  alpine:3.20 tar -C /source -czf /backup/uploads.tar.gz .

[[ -s "$WORK_DIR/database.dump" ]] || fail "Dump PostgreSQL não foi criado"
[[ -s "$WORK_DIR/uploads.tar.gz" ]] || fail "Backup de uploads não foi criado"

(cd "$WORK_DIR" && sha256sum database.dump > database.dump.sha256 && sha256sum uploads.tar.gz > uploads.tar.gz.sha256)
(cd "$WORK_DIR" && sha256sum -c database.dump.sha256 >/dev/null && sha256sum -c uploads.tar.gz.sha256 >/dev/null)

finished_at="$(date -Is)"
duration="$(( $(date +%s) - START_EPOCH ))"
commit="$(git -C "$ARQLY_ROOT" rev-parse --short HEAD 2>/dev/null || printf 'unknown')"
{
  printf 'status=COMPLETE\n'
  printf 'started_at=%s\n' "$(date -d "@$START_EPOCH" -Is)"
  printf 'finished_at=%s\n' "$finished_at"
  printf 'duration_seconds=%s\n' "$duration"
  printf 'application_commit=%s\n' "$commit"
  printf 'database=%s\n' "$POSTGRES_DB"
  printf 'database_dump_bytes=%s\n' "$(file_size "$WORK_DIR/database.dump")"
  printf 'uploads_archive_bytes=%s\n' "$(file_size "$WORK_DIR/uploads.tar.gz")"
  printf 'uploads_volume=%s\n' "$(uploads_volume)"
  printf 'database_dump_sha256=%s\n' "$(awk '{print $1}' "$WORK_DIR/database.dump.sha256")"
  printf 'uploads_archive_sha256=%s\n' "$(awk '{print $1}' "$WORK_DIR/uploads.tar.gz.sha256")"
} > "$WORK_DIR/manifest.txt"
chmod 600 "$WORK_DIR"/*
mv "$WORK_DIR" "$FINAL_DIR"
WORK_DIR=""
log "COMPLETE directory=$FINAL_DIR duration_seconds=$duration"

if [[ "${ARQLY_SKIP_BACKUP_CLEANUP:-false}" != "true" ]]; then
  "$SCRIPT_DIR/cleanup-backups.sh" --apply
fi
