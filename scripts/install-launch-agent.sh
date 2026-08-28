#!/bin/zsh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LABEL="com.renovation.ledger.server"
DOMAIN="gui/$(id -u)"
PLIST_SRC="$ROOT/scripts/${LABEL}.plist"
PLIST_DST="$HOME/Library/LaunchAgents/${LABEL}.plist"
JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"

export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

cd "$ROOT"
mkdir -p "$ROOT/logs" "$HOME/Library/LaunchAgents"

echo "building boot jar..."
./gradlew bootJar -q

UID_DOMAIN="$DOMAIN"
if launchctl print "$UID_DOMAIN/$LABEL" >/dev/null 2>&1; then
  launchctl bootout "$UID_DOMAIN/$LABEL" || true
  sleep 1
fi

LISTEN_PID="$(lsof -tiTCP:8080 -sTCP:LISTEN 2>/dev/null || true)"
if [[ -n "$LISTEN_PID" ]]; then
  echo "stopping pid on 8080: $LISTEN_PID"
  kill $LISTEN_PID 2>/dev/null || true
  sleep 2
  LISTEN_PID="$(lsof -tiTCP:8080 -sTCP:LISTEN 2>/dev/null || true)"
  if [[ -n "$LISTEN_PID" ]]; then
    kill -9 $LISTEN_PID 2>/dev/null || true
    sleep 1
  fi
fi

cp "$PLIST_SRC" "$PLIST_DST"
launchctl bootstrap "$UID_DOMAIN" "$PLIST_DST"
launchctl enable "$UID_DOMAIN/$LABEL" 2>/dev/null || true
launchctl kickstart -k "$UID_DOMAIN/$LABEL"

echo "installed $LABEL — waiting for /health"
for i in {1..30}; do
  if curl -sf -m 2 http://127.0.0.1:8080/health >/dev/null; then
    echo "ok: $(curl -sS -m 2 http://127.0.0.1:8080/health)"
    launchctl print "$UID_DOMAIN/$LABEL" | awk '/pid = |state = |path = /'
    exit 0
  fi
  sleep 1
done

echo "service did not become healthy; last logs:" >&2
tail -n 40 "$ROOT/logs/launchd.err.log" "$ROOT/logs/launchd.out.log" 2>/dev/null || true
exit 1
