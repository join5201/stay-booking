#!/bin/sh
# 서버에서 처음 한 번 https 인증서를 받는다 (이슈 250 넷째 항목)
#
# nginx는 인증서 파일이 없으면 뜨지 못하고, 인증서는 nginx가 80에서 확인 파일을 보여 줘야 받는다.
# 그래서 하루짜리 자체 서명 인증서로 먼저 띄우고, Let's Encrypt 인증서를 받아 바꾼 뒤 reload한다.
# 이 스크립트를 돌리면 Let's Encrypt 가입 약관에 동의하는 것이다(--agree-tos). 이메일은 등록하지 않는다.
#
# 실행 예시 (서버의 /opt/o2o에서)
#   sh infra/certbot/init.sh
#   O2O_CERT_STAGING=1 sh infra/certbot/init.sh     시험 발급. 발급 한도를 쓰지 않고 브라우저는 경고한다
#
# 실패 시 출력 예시
#   이미 인증서가 있다. 갱신은 infra/certbot/renew.sh
#     두 번 돌렸다. 정상이다
#   Certbot failed to authenticate some domains ... Timeout during connect (likely firewall problem)
#     80이 밖에서 안 닿는다. 보안 그룹의 80, DuckDNS의 IP가 Elastic IP와 같은지 본다
#   too many certificates (5) already issued for this exact set of identifiers
#     같은 이름으로 일주일에 5번을 넘겼다. 시험은 O2O_CERT_STAGING=1로 한다
set -eu

cd "$(dirname "$0")/../.."
NAME=o2o-stay.duckdns.org
LIVE=/etc/letsencrypt/live/$NAME

if docker compose run --rm --entrypoint test certbot -f "/etc/letsencrypt/renewal/$NAME.conf"; then
  echo "이미 인증서가 있다. 갱신은 infra/certbot/renew.sh"
  exit 0
fi

echo "1/4 임시 인증서"
docker compose run --rm --entrypoint sh certbot -c \
  "mkdir -p $LIVE && openssl req -x509 -nodes -newkey rsa:2048 -days 1 -subj /CN=localhost \
   -keyout $LIVE/privkey.pem -out $LIVE/fullchain.pem 2>/dev/null"

echo "2/4 앱과 nginx 기동"
docker compose up -d --wait

echo "3/4 Let's Encrypt 발급"
# 임시 인증서 폴더가 남아 있으면 certbot이 $NAME-0001이라는 새 폴더에 받아 nginx 경로와 어긋난다
docker compose run --rm --entrypoint rm certbot -rf "$LIVE"
STAGING=""
[ "${O2O_CERT_STAGING:-0}" = "1" ] && STAGING="--staging"
docker compose run --rm certbot certonly --webroot -w /var/www/certbot -d "$NAME" \
  --register-unsafely-without-email --agree-tos --non-interactive $STAGING

echo "4/4 nginx reload"
docker compose exec nginx nginx -s reload
echo "끝. https://$NAME"
