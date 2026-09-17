#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARQLY_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ARQLY_ENV_FILE="${ARQLY_ENV_FILE:-$ARQLY_ROOT/.env}"
ARQLY_BACKUP_DIR="${ARQLY_BACKUP_DIR:-$ARQLY_ROOT/backups}"
ARQLY_STAGING_HTTPS="${ARQLY_STAGING_HTTPS:-true}"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Comando obrigatório não encontrado: $1"
}

load_environment() {
  [[ -f "$ARQLY_ENV_FILE" ]] || fail "Arquivo de ambiente não encontrado: $ARQLY_ENV_FILE"
  set -a
  # shellcheck disable=SC1090
  source "$ARQLY_ENV_FILE"
  set +a

  : "${POSTGRES_DB:?POSTGRES_DB é obrigatório}"
  : "${POSTGRES_USER:?POSTGRES_USER é obrigatório}"
  : "${ARQLY_VOLUME_PREFIX:=arqly_staging}"

  [[ "$POSTGRES_DB" != "postgres" ]] || fail "POSTGRES_DB não pode ser o banco administrativo postgres"
}

configure_compose() {
  local env_file="$ARQLY_ENV_FILE"
  local staging_file="$ARQLY_ROOT/docker-compose.staging.yml"
  local https_file="$ARQLY_ROOT/docker-compose.staging.https.yml"
  if [[ "${OSTYPE:-}" == msys* || "${OSTYPE:-}" == mingw* ]]; then
    env_file="$(cygpath -w "$env_file")"
    staging_file="$(cygpath -w "$staging_file")"
    https_file="$(cygpath -w "$https_file")"
  fi
  COMPOSE=(docker compose --env-file "$env_file")
  [[ -n "${ARQLY_COMPOSE_PROJECT:-}" ]] && COMPOSE+=(-p "$ARQLY_COMPOSE_PROJECT")
  COMPOSE+=(-f "$staging_file")
  if [[ "$ARQLY_STAGING_HTTPS" == "true" ]]; then
    COMPOSE+=(-f "$https_file")
  fi
}

compose() {
  if [[ "${OSTYPE:-}" == msys* || "${OSTYPE:-}" == mingw* ]]; then
    MSYS_NO_PATHCONV=1 "${COMPOSE[@]}" "$@"
  else
    "${COMPOSE[@]}" "$@"
  fi
}

docker_run() {
  # Git Bash rewrites container paths such as /backup unless conversion is disabled.
  if [[ "${OSTYPE:-}" == msys* || "${OSTYPE:-}" == mingw* ]]; then
    MSYS_NO_PATHCONV=1 docker run "$@"
  else
    docker run "$@"
  fi
}

uploads_volume() {
  printf '%s_uploads\n' "$ARQLY_VOLUME_PREFIX"
}

ensure_staging_dependencies() {
  require_command docker
  require_command sha256sum
  require_command tar
  require_command date
  configure_compose
  docker volume inspect "$(uploads_volume)" >/dev/null 2>&1 || fail "Volume de uploads não encontrado: $(uploads_volume)"
}

wait_for_postgres() {
  local attempt
  for attempt in $(seq 1 30); do
    if compose exec -T postgres pg_isready -U "$POSTGRES_USER" -d postgres >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  fail "PostgreSQL não ficou disponível a tempo"
}

file_size() {
  stat -c '%s' "$1"
}

backup_epoch_from_name() {
  local name="$1"
  [[ "$name" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{6}$ ]] || return 1
  date -d "${name:0:10} ${name:11:2}:${name:13:2}:${name:15:2}" +%s
}
