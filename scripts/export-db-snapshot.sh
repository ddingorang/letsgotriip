#!/bin/sh
# Dump the current trip-mysql database to db/init/01-seed.sql.
#
# Normal startup does not need this script: root `docker compose up` loads the
# committed dump automatically into an empty MySQL volume. Run this only after
# intentionally regenerating local seed/batch data that should become the next
# shared startup snapshot.
#
# Usage:
#   sh scripts/db-dump.sh
#
# Warning: the dump must contain demo data only, never real user data.
set -eu

OUT="db/init/01-seed.sql"
mkdir -p db/init

echo "mysqldump trip_chat -> ${OUT}"
# Run mysqldump inside the container and redirect the result to the host file.
# --no-create-db omits CREATE DATABASE because MYSQL_DATABASE creates it first.
docker compose exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
     --no-create-db --default-character-set=utf8mb4 \
     --single-transaction --skip-comments --routines \
     trip_chat' > "${OUT}"

BYTES=$(wc -c < "${OUT}" 2>/dev/null || echo 0)
echo "${OUT} (${BYTES} bytes)"
echo "A fresh volume will load this dump on: docker compose down -v && docker compose up -d"
