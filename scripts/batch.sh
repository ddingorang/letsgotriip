#!/usr/bin/env sh
# 관광지 큐레이션 배치 트리거 — 개발용(dev-gated) HTTP 엔드포인트 호출.
# 시드 게이트와 동일: X-Seed-Secret 헤더로 보호되며, 서버측 app.seed.api.enabled=true 여야 한다.
#
# 사용법:
#   ./scripts/batch.sh [total]
#   SEED_API_SECRET=... BASE_URL=... ./scripts/batch.sh 2000
#
# 환경변수:
#   SEED_API_SECRET  배치 시크릿 (기본 triip-local-seed)
#   BASE_URL         서버 베이스 URL (기본 http://localhost:9090)
set -eu

TOTAL="${1:-2000}"
SECRET="${SEED_API_SECRET:-triip-local-seed}"
BASE_URL="${BASE_URL:-http://localhost:9090}"

URL="${BASE_URL}/api/dev/attractions/batch?total=${TOTAL}"

echo "POST ${URL}"

# 본문과 HTTP 상태코드를 함께 출력
RESP=$(curl -sS -w "\n%{http_code}" -X POST "${URL}" -H "X-Seed-Secret: ${SECRET}")

BODY=$(printf '%s\n' "${RESP}" | sed '$d')
CODE=$(printf '%s\n' "${RESP}" | tail -n1)

echo "HTTP ${CODE}"
echo "${BODY}"
