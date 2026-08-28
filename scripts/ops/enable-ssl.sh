#!/bin/bash
# Issue/renew Let's Encrypt certs via webroot; enable 443 if certs exist.
# Does not force-redirect HTTP (ICP 未过时 443 可能被拦).
set -euo pipefail

OPS_SRC="${1:-/opt/renovation-ledger/ops}"
LIVE=/etc/letsencrypt/live/zhuangxiujizhang.site
DOMAINS=(
  zhuangxiujizhang.site
  www.zhuangxiujizhang.site
  api.zhuangxiujizhang.site
  test.zhuangxiujizhang.site
)

mkdir -p /var/www/letsencrypt
args=()
for d in "${DOMAINS[@]}"; do
  args+=(-d "$d")
done

set +e
certbot certonly --webroot -w /var/www/letsencrypt \
  --agree-tos --register-unsafely-without-email --non-interactive \
  --keep-until-expiring \
  --deploy-hook "systemctl reload nginx" \
  "${args[@]}"
cb=$?
set -e

if [[ "$cb" -ne 0 || ! -f "$LIVE/fullchain.pem" ]]; then
  echo "certbot failed or cert missing (ICP/DNS?). HTTP stays. exit=$cb"
  exit "$cb"
fi

install -m 644 "$OPS_SRC/nginx-renovation-ledger-ssl.conf" \
  /etc/nginx/sites-available/renovation-ledger-ssl
ln -sfn /etc/nginx/sites-available/renovation-ledger-ssl /etc/nginx/sites-enabled/renovation-ledger-ssl
nginx -t
systemctl reload nginx
echo "ssl enabled"
