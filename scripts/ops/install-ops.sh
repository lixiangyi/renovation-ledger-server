#!/bin/bash
# Run on the Shanghai host as a sudoer. Idempotent.
set -euo pipefail

OPS_SRC="${1:-/tmp/ledger-ops}"
OPS_DST=/opt/renovation-ledger/ops
WEB_ROOT=/var/www/renovation-ledger-website
ACME_ROOT=/var/www/letsencrypt

install -d -m 755 "$OPS_DST" "$WEB_ROOT/downloads" "$ACME_ROOT" \
  /var/backups/renovation-ledger /var/lib/renovation-ledger
chmod 700 /var/backups/renovation-ledger

install -m 755 "$OPS_SRC"/ledger-pg-backup.sh "$OPS_DST/"
install -m 755 "$OPS_SRC"/ledger-pg-restore.sh "$OPS_DST/"
install -m 755 "$OPS_SRC"/ledger-healthcheck.sh "$OPS_DST/"
install -m 755 "$OPS_SRC"/ledger-cos-put.py "$OPS_DST/"
install -m 755 "$OPS_SRC"/enable-ssl.sh "$OPS_DST/"
install -m 644 "$OPS_SRC"/nginx-renovation-ledger-ssl.conf "$OPS_DST/"
install -m 644 "$OPS_SRC"/logrotate-ledger /etc/logrotate.d/renovation-ledger
install -m 644 "$OPS_SRC"/cron-renovation-ledger /etc/cron.d/renovation-ledger
install -m 644 "$OPS_SRC"/nginx-renovation-ledger.conf /etc/nginx/sites-available/renovation-ledger
ln -sfn /etc/nginx/sites-available/renovation-ledger /etc/nginx/sites-enabled/renovation-ledger
rm -f /etc/nginx/sites-enabled/default

if [[ ! -f /etc/renovation-ledger-backup.env ]]; then
  install -m 600 "$OPS_SRC"/ledger-backup.env.example /etc/renovation-ledger-backup.env
fi
if [[ ! -f /etc/renovation-ledger-alerts.env ]]; then
  install -m 600 "$OPS_SRC"/ledger-alerts.env.example /etc/renovation-ledger-alerts.env
fi

touch /var/log/ledger-health.log /var/log/ledger-backup.log
chmod 640 /var/log/ledger-health.log /var/log/ledger-backup.log

install -d -m 755 /etc/letsencrypt/renewal-hooks/deploy
cat >/etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh <<'HOOK'
#!/bin/bash
systemctl reload nginx
HOOK
chmod 755 /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh

nginx -t
systemctl reload nginx

echo "ops installed"
