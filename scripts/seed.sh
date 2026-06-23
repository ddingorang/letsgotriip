#!/bin/sh
# Triip 데모/테스트 데이터 시드 트리거.
#
# 사용법:
#   ./scripts/seed.sh [demo|test] [--reset]
#
# 예시:
#   ./scripts/seed.sh                # demo, reset 없음(멱등)
#   ./scripts/seed.sh demo --reset   # demo, 기존 시드 데이터 정리 후 재삽입
#   ./scripts/seed.sh test --reset   # test 픽스처(엣지케이스 포함) 재삽입
#
# 환경변수:
#   SEED_API_SECRET  백엔드 X-Seed-Secret (기본: triip-local-seed — compose 기본값과 일치)
#   SEED_BASE_URL    백엔드 베이스 URL (기본: http://localhost:9090)
#
# 사전 조건: `docker compose up -d` 로 백엔드가 떠 있고 SEED_API_ENABLED=true 일 것.

set -eu

PROFILE="demo"
RESET="false"

for arg in "$@"; do
  case "$arg" in
    demo|test) PROFILE="$arg" ;;
    --reset)   RESET="true" ;;
    -h|--help)
      echo "Usage: ./scripts/seed.sh [demo|test] [--reset]"
      exit 0
      ;;
    *)
      echo "알 수 없는 인자: $arg" >&2
      echo "Usage: ./scripts/seed.sh [demo|test] [--reset]" >&2
      exit 1
      ;;
  esac
done

BASE_URL="${SEED_BASE_URL:-http://localhost:9090}"
SECRET="${SEED_API_SECRET:-triip-local-seed}"
URL="${BASE_URL}/api/dev/seed?profile=${PROFILE}&reset=${RESET}"

echo "→ 시드 요청: profile=${PROFILE} reset=${RESET}"
echo "  ${URL}"

# -s 조용히, -S 에러는 표시, -w 마지막 줄에 HTTP 상태코드
RESPONSE=$(curl -sS -X POST "$URL" \
  -H "X-Seed-Secret: ${SECRET}" \
  -H "Content-Type: application/json" \
  -w '\n__HTTP_STATUS__:%{http_code}')

BODY=$(printf '%s' "$RESPONSE" | sed -e 's/__HTTP_STATUS__:[0-9]*$//')
STATUS=$(printf '%s' "$RESPONSE" | tr -d '\n' | sed -e 's/.*__HTTP_STATUS__://')

echo "← HTTP ${STATUS}"
# jq 가 있으면 예쁘게, 없으면 원문 출력
if command -v jq >/dev/null 2>&1; then
  printf '%s' "$BODY" | jq . 2>/dev/null || printf '%s\n' "$BODY"
else
  printf '%s\n' "$BODY"
fi

case "$STATUS" in
  200) echo "✅ 시드 완료"; exit 0 ;;
  403) echo "⛔ 거부됨 — X-Seed-Secret 불일치 (SEED_API_SECRET 확인)" >&2; exit 1 ;;
  404) echo "⛔ 비활성 — SEED_API_ENABLED=true 인지 확인" >&2; exit 1 ;;
  *)   echo "⚠ 예기치 못한 응답 (HTTP ${STATUS})" >&2; exit 1 ;;
esac
