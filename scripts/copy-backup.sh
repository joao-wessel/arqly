#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/staging-common.sh"

provided_remote="${ARQLY_BACKUP_REMOTE:-}"
load_environment
[[ -n "$provided_remote" ]] && ARQLY_BACKUP_REMOTE="$provided_remote"

[[ $# -eq 1 ]] || { printf 'Uso: %s <diretório-do-backup>\n' "$0" >&2; exit 2; }
backup_dir="$1"
[[ -d "$backup_dir" && -f "$backup_dir/manifest.txt" ]] || fail "Backup inválido: $backup_dir"
grep -qx 'status=COMPLETE' "$backup_dir/manifest.txt" || fail "Backup não está completo"
: "${ARQLY_BACKUP_REMOTE:?Defina ARQLY_BACKUP_REMOTE, por exemplo backup@host:/srv/arqly-backups}"
require_command rsync

rsync -a --protect-args --chmod=Du=rwx,Dgo=,Fu=rw,Fgo= "$backup_dir/" "${ARQLY_BACKUP_REMOTE%/}/$(basename "$backup_dir")/"
printf 'Backup copiado para %s\n' "$ARQLY_BACKUP_REMOTE"
