# Created: 2026-06-26 03:18:54
#!/bin/sh
# 축제 동기화 배치 수동 실행 스크립트.
#
# 처음 프로젝트를 실행할 때 사용한다. 01-seed.sql에는 축제 데이터가 없으므로
# 이 스크립트를 통해 공공데이터 API에서 축제 정보를 가져온다.
#
# Usage:
#   sh scripts/run-festival-sync.sh
#
# Environment:
#   SEED_API_SECRET  backend X-Seed-Secret (default: triip-local-seed)
#   SEED_BASE_URL    backend base URL (default: http://localhost:9090)
#
# Prerequisite: docker compose up 후 backend가 완전히 기동된 상태여야 한다.
#               SEED_API_ENABLED=true 환경 변수가 설정되어 있어야 한다.

set -eu

BASE_URL="${SEED_BASE_URL:-http://localhost:9090}"
SECRET="${SEED_API_SECRET:-triip-local-seed}"
URL="${BASE_URL}/api/dev/festivals/sync"

# 백엔드가 준비될 때까지 최대 60초 대기
echo "백엔드 기동 대기 중..."
MAX_WAIT=60
WAITED=0
until curl -sf "${BASE_URL}/api/festivals" > /dev/null 2>&1; do
  if [ "$WAITED" -ge "$MAX_WAIT" ]; then
    echo "오류: 백엔드가 ${MAX_WAIT}초 내에 응답하지 않습니다." >&2
    exit 1
  fi
  printf '.'
  sleep 2
  WAITED=$((WAITED + 2))
done
echo ""
echo "백엔드 준비 완료."

echo "축제 동기화 배치 실행 중..."
echo "  POST ${URL}"

RESPONSE=$(curl -sS -X POST "$URL" \
  -H "X-Seed-Secret: ${SECRET}" \
  -H "Content-Type: application/json" \
  -w '\n__HTTP_STATUS__:%{http_code}')

BODY=$(printf '%s' "$RESPONSE" | sed -e 's/__HTTP_STATUS__:[0-9]*$//')
STATUS=$(printf '%s' "$RESPONSE" | tr -d '\n' | sed -e 's/.*__HTTP_STATUS__://')

echo "HTTP ${STATUS}"
if command -v jq >/dev/null 2>&1; then
  printf '%s' "$BODY" | jq . 2>/dev/null || printf '%s\n' "$BODY"
else
  printf '%s\n' "$BODY"
fi

case "$STATUS" in
  200) echo "축제 동기화 배치가 시작되었습니다. (완료까지 수 분 소요됩니다)"; exit 0 ;;
  403) echo "오류: X-Seed-Secret 불일치. SEED_API_SECRET 환경 변수를 확인하세요." >&2; exit 1 ;;
  404) echo "오류: SEED_API_ENABLED=true 설정이 필요합니다." >&2; exit 1 ;;
  *)   echo "예상치 못한 응답 (HTTP ${STATUS})" >&2; exit 1 ;;
esac
