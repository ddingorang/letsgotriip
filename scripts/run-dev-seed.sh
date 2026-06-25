#!/bin/sh
# Triip demo/test seed trigger.
#
# Normal startup does not need this script: root `docker compose up` loads
# db/init/01-seed.sql automatically into an empty MySQL volume. Use this only
# when regenerating or refreshing seed data through the dev API.
#
# Usage:
#   sh scripts/seed.sh [demo|test] [--reset]
#
# Examples:
#   sh scripts/seed.sh                # demo, reset=false (idempotent)
#   sh scripts/seed.sh demo --reset   # recreate demo seed data
#   sh scripts/seed.sh test --reset   # recreate test fixtures
#
# Environment:
#   SEED_API_SECRET  backend X-Seed-Secret (default: triip-local-seed)
#   SEED_BASE_URL    backend base URL (default: http://localhost:9090)
#
# Prerequisite: backend is running with SEED_API_ENABLED=true.

set -eu

PROFILE="demo"
RESET="false"

for arg in "$@"; do
  case "$arg" in
    demo|test) PROFILE="$arg" ;;
    --reset)   RESET="true" ;;
    -h|--help)
      echo "Usage: sh scripts/seed.sh [demo|test] [--reset]"
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      echo "Usage: sh scripts/seed.sh [demo|test] [--reset]" >&2
      exit 1
      ;;
  esac
done

BASE_URL="${SEED_BASE_URL:-http://localhost:9090}"
SECRET="${SEED_API_SECRET:-triip-local-seed}"
URL="${BASE_URL}/api/dev/seed?profile=${PROFILE}&reset=${RESET}"

echo "Seed request: profile=${PROFILE} reset=${RESET}"
echo "  ${URL}"

# -s stays quiet, -S shows curl errors, -w appends the HTTP status marker.
RESPONSE=$(curl -sS -X POST "$URL" \
  -H "X-Seed-Secret: ${SECRET}" \
  -H "Content-Type: application/json" \
  -w '\n__HTTP_STATUS__:%{http_code}')

BODY=$(printf '%s' "$RESPONSE" | sed -e 's/__HTTP_STATUS__:[0-9]*$//')
STATUS=$(printf '%s' "$RESPONSE" | tr -d '\n' | sed -e 's/.*__HTTP_STATUS__://')

echo "HTTP ${STATUS}"
# Pretty-print with jq when available; otherwise print the raw response body.
if command -v jq >/dev/null 2>&1; then
  printf '%s' "$BODY" | jq . 2>/dev/null || printf '%s\n' "$BODY"
else
  printf '%s\n' "$BODY"
fi

case "$STATUS" in
  200) echo "Seed complete"; exit 0 ;;
  403) echo "Forbidden: check SEED_API_SECRET / X-Seed-Secret" >&2; exit 1 ;;
  404) echo "Seed API disabled: check SEED_API_ENABLED=true" >&2; exit 1 ;;
  *)   echo "Unexpected response (HTTP ${STATUS})" >&2; exit 1 ;;
esac
