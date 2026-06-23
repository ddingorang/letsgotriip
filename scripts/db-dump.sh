#!/bin/sh
# 현재 trip-mysql DB(시드 + 2천개 배치까지 완료된 상태)를 db/init/01-seed.sql 로 덤프한다.
# 이 파일은 docker-compose 의 mysql 서비스가 /docker-entrypoint-initdb.d 로 마운트해,
# '빈 볼륨'으로 처음 뜰 때 한 번 bulk 로드한다 → ddl-auto 스키마 생성 + 런타임 시드를 건너뛰어 빠르게 기동.
#
# 사용: 데이터가 원하는 상태일 때 한 번 실행 → 커밋. 스키마(엔티티) 바뀌면 다시 떠야 한다.
#   ./scripts/db-dump.sh
#
# 주의: 덤프엔 데모 계정 비밀번호 해시 등 '데모용' 데이터만 들어가야 한다(실데이터 금지).
set -eu

OUT="db/init/01-seed.sql"
mkdir -p db/init

echo "→ mysqldump trip_chat → ${OUT}"
# 컨테이너 안에서 mysqldump 실행(비밀번호는 컨테이너 env 사용). 호스트 파일로 리다이렉트.
# --no-create-db: CREATE DATABASE 제외(MYSQL_DATABASE 가 이미 생성). utf8mb4 로 한글 보존.
docker compose exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
     --no-create-db --default-character-set=utf8mb4 \
     --single-transaction --skip-comments --routines \
     trip_chat' > "${OUT}"

BYTES=$(wc -c < "${OUT}" 2>/dev/null || echo 0)
echo "✓ ${OUT} (${BYTES} bytes)"
echo "  이제 docker compose down -v && docker compose up -d 하면 이 덤프에서 즉시 로드됩니다."
