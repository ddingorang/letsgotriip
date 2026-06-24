#!/usr/bin/env sh
# Attraction curation batch trigger for the dev-gated HTTP endpoint.
#
# Normal startup does not need this script: root `docker compose up` loads the
# committed db/init/01-seed.sql dump automatically into an empty MySQL volume.
# Use this only when refreshing attraction batch data before creating a new dump.
#
# Usage:
#   sh scripts/batch.sh [total]
#   SEED_API_SECRET=... SEED_BASE_URL=... sh scripts/batch.sh 2000
#
# Environment:
#   SEED_API_SECRET  dev API secret (default: triip-local-seed)
#   SEED_BASE_URL    server base URL (default: http://localhost:9090)
#   BASE_URL         legacy alias for SEED_BASE_URL
set -eu

case "${1:-}" in
  -h|--help)
    echo "Usage: sh scripts/batch.sh [total]"
    exit 0
    ;;
esac

TOTAL="${1:-2000}"
SECRET="${SEED_API_SECRET:-triip-local-seed}"
BASE_URL="${SEED_BASE_URL:-${BASE_URL:-http://localhost:9090}}"

case "$TOTAL" in
  ''|*[!0-9]*)
    echo "total must be a positive integer: ${TOTAL}" >&2
    exit 1
    ;;
esac

if [ "$TOTAL" -le 0 ]; then
  echo "total must be greater than zero" >&2
  exit 1
fi

URL="${BASE_URL}/api/dev/attractions/batch?total=${TOTAL}"

echo "POST ${URL}"

# Print the response body plus HTTP status.
RESP=$(curl -sS -w "\n%{http_code}" -X POST "${URL}" -H "X-Seed-Secret: ${SECRET}")

BODY=$(printf '%s\n' "${RESP}" | sed '$d')
CODE=$(printf '%s\n' "${RESP}" | tail -n1)

echo "HTTP ${CODE}"
echo "${BODY}"

case "$CODE" in
  2??) exit 0 ;;
  403) echo "Forbidden: check SEED_API_SECRET / X-Seed-Secret" >&2; exit 1 ;;
  404) echo "Batch API disabled: check SEED_API_ENABLED=true" >&2; exit 1 ;;
  *)   echo "Unexpected response (HTTP ${CODE})" >&2; exit 1 ;;
esac
