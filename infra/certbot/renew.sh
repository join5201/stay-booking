#!/bin/sh
# 인증서 갱신 (이슈 250 넷째 항목). 서버의 cron이 하루 한 번 부른다.
# certbot은 만료 30일 전부터만 실제로 갱신하므로 매일 불러도 된다. nginx는 reload해야 새 인증서를 읽는다.
#
# 실행 예시 (서버의 /opt/o2o에서)
#   sh infra/certbot/renew.sh
#   sh infra/certbot/renew.sh --dry-run      예행. 발급 한도를 쓰지 않는다
#   cron:  17 4 * * * cd /opt/o2o && sh infra/certbot/renew.sh >> /var/log/o2o-renew.log 2>&1
#
# 실패 시 출력 예시
#   No renewals were attempted.
#     init.sh를 아직 안 돌렸다
#   Certbot failed to authenticate some domains
#     80이 밖에서 안 닿는다. init.sh의 실패 예시와 같다
set -eu

cd "$(dirname "$0")/../.."
docker compose run --rm certbot renew "$@"
docker compose exec nginx nginx -s reload
