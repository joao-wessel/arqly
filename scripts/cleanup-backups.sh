#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/staging-common.sh"

load_environment

MODE="dry-run"
[[ "${1:-}" == "--apply" ]] && MODE="apply"
[[ $# -le 1 ]] || { printf 'Uso: %s [--apply]\n' "$0" >&2; exit 2; }

mkdir -p "$ARQLY_BACKUP_DIR"
mapfile -t backups < <(find "$ARQLY_BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name '20??-??-??_??????' -printf '%f\n' | sort -r)
declare -A kept_week kept_month
weekly=0
monthly=0
now_epoch="$(date +%s)"

for name in "${backups[@]}"; do
  directory="$ARQLY_BACKUP_DIR/$name"
  grep -qx 'status=COMPLETE' "$directory/manifest.txt" 2>/dev/null || continue
  timestamp="${name:0:10} ${name:11:2}:${name:13:2}:${name:15:2}"
  epoch="$(backup_epoch_from_name "$name" 2>/dev/null || printf 0)"
  (( epoch > 0 )) || continue
  age_days=$(( (now_epoch - epoch) / 86400 ))
  week="$(date -d "$timestamp" +%G-%V)"
  month="$(date -d "$timestamp" +%Y-%m)"
  keep=false

  if (( age_days <= 7 )); then
    keep=true
  elif [[ -z "${kept_week[$week]:-}" && $weekly -lt 4 ]]; then
    kept_week[$week]=1
    weekly=$((weekly + 1))
    keep=true
  elif [[ -z "${kept_month[$month]:-}" && $monthly -lt 3 ]]; then
    kept_month[$month]=1
    monthly=$((monthly + 1))
    keep=true
  fi

  if [[ "$keep" == true ]]; then
    printf 'KEEP %s\n' "$directory"
  elif [[ "$MODE" == "apply" ]]; then
    printf 'DELETE %s\n' "$directory"
    rm -rf -- "$directory"
  else
    printf 'WOULD_DELETE %s\n' "$directory"
  fi
done
