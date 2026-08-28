#!/bin/bash
# Dump both ledger databases. Keep local copies, optionally upload to Tencent COS.
set -euo pipefail

ENV_FILE="${LEDGER_BACKUP_ENV:-/etc/renovation-ledger-backup.env}"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

DEST="${BACKUP_DIR:-/var/backups/renovation-ledger}"
KEEP_DAYS="${KEEP_DAYS:-14}"
STAMP="$(date +%Y%m%d-%H%M%S)"
DIR="$DEST/$STAMP"

umask 077
mkdir -p "$DIR"
chmod 700 "$DEST"

dump_one() {
  local db="$1"
  sudo -u postgres pg_dump -Fc "$db" >"$DIR/${db}.dump"
}

notify_fail() {
  local text="装修记账 Postgres 备份失败 $(date '+%F %T')
$*
主机 $(hostname) 111.229.202.28"
  local alert="${LEDGER_ALERT_ENV:-/etc/renovation-ledger-alerts.env}"
  if [[ -f "$alert" ]]; then
    # shellcheck disable=SC1090
    source "$alert"
  fi
  if [[ -n "${FEISHU_WEBHOOK:-}" ]]; then
    local payload
    payload="$(python3 -c 'import json,sys; print(json.dumps({"msg_type":"text","content":{"text":sys.argv[1]}}))' "$text")"
    curl -fsS -X POST -H 'Content-Type: application/json' -d "$payload" "$FEISHU_WEBHOOK" >/dev/null || true
  fi
}

trap 'notify_fail "见 /var/log/ledger-backup.log"' ERR

dump_one renovation_ledger
dump_one renovation_ledger_test
echo "$STAMP" >"$DIR/OK"
find "$DEST" -mindepth 1 -maxdepth 1 -type d -mtime "+${KEEP_DAYS}" -exec rm -rf {} +

if [[ -n "${COS_BUCKET:-}" && -n "${COS_SECRET_ID:-}" && -n "${COS_SECRET_KEY:-}" ]]; then
  PUT="${PUT_BIN:-/opt/renovation-ledger/ops/ledger-cos-put.py}"
  prefix="${COS_PREFIX:-ledger-pg}/${STAMP}"
  python3 "$PUT" "$DIR/renovation_ledger.dump" "$prefix/renovation_ledger.dump"
  python3 "$PUT" "$DIR/renovation_ledger_test.dump" "$prefix/renovation_ledger_test.dump"
  python3 "$PUT" "$DIR/OK" "$prefix/OK"
else
  echo "COS_* not set; local dump only $DIR"
fi
echo "backup ok $DIR"
