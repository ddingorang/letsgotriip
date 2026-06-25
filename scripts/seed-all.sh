#!/bin/sh
# 초기 데이터 시딩 통합 오케스트레이터.
#
# scripts/SEEDING.txt 의 "시나리오 B" STEP 2~5 를 한 번에, 빠짐없이 실행한다.
# 각 단계는 기존 개별 스크립트를 그대로 호출하므로 로직이 중복되지 않는다.
#
#   STEP 3 run-attraction-batch.sh          TourAPI 관광지 배치 수집
#   STEP 2 run-dev-seed.sh                   데모/테스트 시드 데이터
#   STEP 4 refresh-community-image-urls.py   커뮤니티 이미지 URL 갱신
#   STEP 5 export-db-snapshot.sh             DB → db/init/01-seed.sql 덤프
#
# 기본 동작은 위 STEP 을 전부 실행한다(선택 단계 구분 없음).
# 특정 단계를 건너뛰려면 --no-* 플래그를 쓴다.
#
# Usage:
#   sh scripts/seed-all.sh [options]
#
# Options:
#   --profile <demo|test>  시드 프로파일 (기본: demo)
#   --reset                기존 시드 데이터 초기화 후 재삽입
#   --batch <total>        관광지 수집 건수 (기본: 2000)
#   --no-batch             STEP 3(관광지 수집) 건너뛰기
#   --no-images            STEP 4(이미지 갱신) 건너뛰기
#   --no-export            STEP 5(DB 덤프) 건너뛰기
#   -h, --help             도움말 출력
#
# Examples:
#   sh scripts/seed-all.sh                   # STEP 2~5 전부 실행
#   sh scripts/seed-all.sh --reset           # 초기화 후 전체 재구성
#   sh scripts/seed-all.sh --no-export       # 덤프만 빼고 전부 실행
#
# Environment (하위 스크립트로 그대로 전달됨):
#   SEED_API_SECRET  backend X-Seed-Secret (기본: triip-local-seed)
#   SEED_BASE_URL    backend base URL (기본: http://localhost:9090)
#
# Prerequisite: backend 가 SEED_API_ENABLED=true 로 실행 중이어야 한다.

set -eu

# 이 스크립트가 위치한 디렉터리 — 어디서 실행하든 형제 스크립트를 찾는다.
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

PROFILE="demo"
RESET=""
BATCH_TOTAL="2000"
DO_BATCH="true"
DO_IMAGES="true"
DO_EXPORT="true"

usage() {
  # 헤더 주석 블록(2번째 줄부터 첫 비주석 줄 직전까지)만 출력한다.
  awk 'NR==1{next} /^#/{sub(/^# ?/,""); print; next} {exit}' "$0"
  exit "${1:-0}"
}

# 실제로 동작하는 python 인터프리터를 찾는다.
# Windows 에선 python/python3 가 스토어 스텁일 수 있으므로 -c 로 실행 검증한다.
find_python() {
  for cand in py python3 python; do
    if command -v "$cand" >/dev/null 2>&1 && "$cand" -c 'import sys' >/dev/null 2>&1; then
      echo "$cand"; return 0
    fi
  done
  return 1
}

# 인자 파싱
while [ $# -gt 0 ]; do
  case "$1" in
    --profile)
      [ $# -ge 2 ] || { echo "--profile 값이 필요합니다" >&2; exit 1; }
      PROFILE="$2"; shift 2 ;;
    --reset)     RESET="--reset"; shift ;;
    --batch)
      [ $# -ge 2 ] || { echo "--batch 값이 필요합니다" >&2; exit 1; }
      case "$2" in
        ''|*[!0-9]*) echo "--batch 값은 양의 정수여야 합니다: $2" >&2; exit 1 ;;
        *) BATCH_TOTAL="$2"; shift 2 ;;
      esac ;;
    --no-batch)  DO_BATCH="false"; shift ;;
    --no-images) DO_IMAGES="false"; shift ;;
    --no-export) DO_EXPORT="false"; shift ;;
    -h|--help)   usage 0 ;;
    *) echo "알 수 없는 옵션: $1" >&2; usage 1 ;;
  esac
done

step() { printf '\n========== %s ==========\n' "$1"; }

# ── STEP 3 (시드보다 먼저) — 관광지 배치 수집 ──────────────────────────────────
# 관광지 스냅샷이 먼저 채워져 있어야 시드/추천이 풍부해지므로 STEP 2 앞에 둔다.
if [ "$DO_BATCH" = "true" ]; then
  step "STEP 3 │ 관광지 배치 수집 (total=${BATCH_TOTAL})"
  sh "${SCRIPT_DIR}/run-attraction-batch.sh" "${BATCH_TOTAL}"
fi

# ── STEP 2 — 데모/테스트 시드 ──────────────────────────────────────────────────
step "STEP 2 │ 시드 데이터 삽입 (profile=${PROFILE}${RESET:+, reset})"
# run-dev-seed.sh [demo|test] [--reset]
sh "${SCRIPT_DIR}/run-dev-seed.sh" "${PROFILE}" ${RESET}

# ── STEP 4 — 커뮤니티 이미지 URL 갱신 ──────────────────────────────────────────
if [ "$DO_IMAGES" = "true" ]; then
  step "STEP 4 │ 커뮤니티 이미지 URL 갱신"
  if PY=$(find_python); then
    "$PY" "${SCRIPT_DIR}/refresh-community-image-urls.py"
  else
    echo "python 인터프리터를 찾을 수 없어 STEP 4 를 건너뜁니다." >&2
    echo "Python 설치 후 다시 실행하거나 --no-images 로 생략하세요." >&2
  fi
fi

# ── STEP 5 — DB 스냅샷 덤프 ────────────────────────────────────────────────────
if [ "$DO_EXPORT" = "true" ]; then
  step "STEP 5 │ DB 스냅샷 덤프 (db/init/01-seed.sql)"
  sh "${SCRIPT_DIR}/export-db-snapshot.sh"
fi

step "시딩 완료"
