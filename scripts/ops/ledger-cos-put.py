#!/usr/bin/env python3
"""PUT a local file to Tencent COS using the XML API (stdlib only)."""
import hashlib
import hmac
import os
import sys
import time
import urllib.error
import urllib.request


def authorization(secret_id, secret_key, method, host, path):
    now = int(time.time())
    key_time = "%d;%d" % (now, now + 3600)
    sign_key = hmac.new(secret_key.encode(), key_time.encode(), hashlib.sha1).hexdigest()
    http_string = "%s\n%s\n\nhost=%s\n" % (method.lower(), path, host.lower())
    sha_http = hashlib.sha1(http_string.encode()).hexdigest()
    string_to_sign = "sha1\n%s\n%s\n" % (key_time, sha_http)
    signature = hmac.new(sign_key.encode(), string_to_sign.encode(), hashlib.sha1).hexdigest()
    return (
        "q-sign-algorithm=sha1&q-ak=%s&q-sign-time=%s&q-key-time=%s"
        "&q-header-list=host&q-url-param-list=&q-signature=%s"
        % (secret_id, key_time, key_time, signature)
    )


def put_file(local_path, bucket, region, key, secret_id, secret_key, endpoint=""):
    host = endpoint or ("%s.cos.%s.myqcloud.com" % (bucket, region))
    path = "/" + key.lstrip("/")
    url = "https://%s%s" % (host, path)
    data = open(local_path, "rb").read()
    auth = authorization(secret_id, secret_key, "PUT", host, path)
    req = urllib.request.Request(url, data=data, method="PUT")
    req.add_header("Host", host)
    req.add_header("Authorization", auth)
    req.add_header("Content-Length", str(len(data)))
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            if resp.status not in (200, 204):
                raise SystemExit("COS PUT HTTP %s" % resp.status)
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", "replace")
        raise SystemExit("COS PUT failed %s: %s" % (exc.code, body[:500]))


def main():
    if len(sys.argv) != 3:
        raise SystemExit("usage: ledger-cos-put.py LOCAL_FILE OBJECT_KEY")
    secret_id = os.environ["COS_SECRET_ID"]
    secret_key = os.environ["COS_SECRET_KEY"]
    bucket = os.environ["COS_BUCKET"]
    region = os.environ.get("COS_REGION", "ap-shanghai")
    endpoint = os.environ.get("COS_ENDPOINT", "")
    put_file(sys.argv[1], bucket, region, sys.argv[2], secret_id, secret_key, endpoint)


if __name__ == "__main__":
    main()
