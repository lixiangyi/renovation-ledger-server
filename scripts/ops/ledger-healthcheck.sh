#!/bin/bash
# Probe local APIs. On failure, notify Feishu webhook and/or email (once per cooldown).
set -euo pipefail

ENV_FILE="${LEDGER_ALERT_ENV:-/etc/renovation-ledger-alerts.env}"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

STATE_DIR="${STATE_DIR:-/var/lib/renovation-ledger}"
STATE="$STATE_DIR/health-fail"
COOLDOWN_SEC="${COOLDOWN_SEC:-1800}"
LOG="${LOG_FILE:-/var/log/ledger-health.log}"
mkdir -p "$STATE_DIR"
touch "$LOG"

fail=""
check() {
  local name="$1" url="$2"
  local body
  if ! body="$(curl -fsS --max-time 8 "$url" 2>/dev/null)"; then
    fail="${fail}${name} 无响应 (${url})"$'\n'
    return
  fi
  if ! echo "$body" | grep -q '"ok"'; then
    fail="${fail}${name} 异常 body=${body}"$'\n'
  fi
}

check "正式 :8080" "http://127.0.0.1:8080/health"
check "测试 :8081" "http://127.0.0.1:8081/health"
check "Nginx /health" "http://127.0.0.1/health"
check "Nginx /test/health" "http://127.0.0.1/test/health"

ts="$(date '+%F %T')"
if [[ -z "$fail" ]]; then
  echo "$ts ok" >>"$LOG"
  rm -f "$STATE"
  exit 0
fi

echo "$ts FAIL $fail" >>"$LOG"

now="$(date +%s)"
if [[ -f "$STATE" ]]; then
  last="$(cat "$STATE" || echo 0)"
  if (( now - last < COOLDOWN_SEC )); then
    exit 1
  fi
fi
echo "$now" >"$STATE"

text="装修记账健康检查失败 ${ts}
${fail}主机 $(hostname) 111.229.202.28"

if [[ -n "${FEISHU_WEBHOOK:-}" ]]; then
  payload="$(python3 -c 'import json,sys; print(json.dumps({"msg_type":"text","content":{"text":sys.argv[1]}}))' "$text")"
  curl -fsS -X POST -H 'Content-Type: application/json' -d "$payload" "$FEISHU_WEBHOOK" >/dev/null || true
fi

if [[ -n "${ALERT_EMAIL:-}" && -n "${MAILGUN_API_KEY:-}" && -n "${MAILGUN_DOMAIN:-}" ]]; then
  curl -fsS --user "api:${MAILGUN_API_KEY}" \
    "https://api.mailgun.net/v3/${MAILGUN_DOMAIN}/messages" \
    -F from="ledger-alert@${MAILGUN_DOMAIN}" \
    -F to="$ALERT_EMAIL" \
    -F subject="装修记账健康检查失败" \
    -F text="$text" >/dev/null || true
fi

exit 1
