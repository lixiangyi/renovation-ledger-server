#!/bin/bash
# Restore a custom-format dump into an existing database (overwrites objects).
# Example: sudo LEDGER_RESTORE_DB=renovation_ledger \
#   /opt/renovation-ledger/ops/ledger-pg-restore.sh /var/backups/renovation-ledger/STAMP/renovation_ledger.dump
set -euo pipefail
DUMP="${1:?dump file}"
DB="${LEDGER_RESTORE_DB:?set LEDGER_RESTORE_DB}"
sudo -u postgres pg_restore --clean --if-exists --no-owner -d "$DB" "$DUMP"
echo "restored $DUMP -> $DB"
