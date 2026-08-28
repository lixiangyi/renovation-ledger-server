#!/bin/zsh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

JAR="$ROOT/build/libs/renovation-ledger-server-0.1.0.jar"
if [[ ! -f "$JAR" ]]; then
  echo "missing $JAR — run: ./gradlew bootJar" >&2
  exit 1
fi

mkdir -p "$ROOT/logs"
exec "$JAVA_HOME/bin/java" -jar "$JAR" \
  --spring.profiles.active=local \
  --server.address=0.0.0.0
