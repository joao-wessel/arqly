#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/staging-common.sh"

load_environment

max_age_hours="${ARQLY_BACKUP_MAX_AGE_HOURS:-26}"
latest="$(find "$ARQLY_BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name '20??-??-??_??????' -printf '%f\n' 2>/dev/null | sort -r | head -n 1 || true)"
[[ -n "$latest" ]] || fail "Nenhum backup encontrado em $ARQLY_BACKUP_DIR"
directory="$ARQLY_BACKUP_DIR/$latest"
grep -qx 'status=COMPLETE' "$directory/manifest.txt" || fail "Último backup não está completo: $directory"
backup_epoch="$(backup_epoch_from_name "$latest")"
age_seconds=$(( $(date +%s) - backup_epoch ))
size="$(du -sh "$directory" | awk '{print $1}')"
printf 'latest=%s\nage_hours=%s\nsize=%s\nstatus=COMPLETE\n' "$directory" "$((age_seconds / 3600))" "$size"
(( age_seconds <= max_age_hours * 3600 )) || fail "Último backup excede ${max_age_hours}h"
