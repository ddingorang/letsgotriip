# 데모/테스트 데이터 시드

스크립트 한 번으로 갓 `docker compose` 한 DB에 화면·챗봇 검증용 엔드투엔드 데이터를 채운다.
부팅 시드(`DataSeeder`/`HotplaceSeeder`/`NoticeSeeder`)와는 별개이며, 이들은 그대로 둔다.

## 실행 방법

```sh
docker compose up -d                 # 백엔드 + 인프라 기동 (compose 에 SEED_API_* 기본값 포함)
./scripts/seed.sh demo --reset       # demo 데이터로 초기화 후 삽입
# 또는
./scripts/seed.sh test --reset       # test 픽스처(엣지케이스)
./scripts/seed.sh                     # demo, 멱등(이미 있으면 skip)
```

스크립트는 `POST http://localhost:9090/api/dev/seed?profile=<demo|test>&reset=<true|false>` 를
`X-Seed-Secret` 헤더와 함께 호출하고, 엔티티별 삽입 건수 JSON 을 출력한다.

- 환경변수 `SEED_API_SECRET`(기본 `triip-local-seed`), `SEED_BASE_URL`(기본 `http://localhost:9090`)
- `jq` 가 있으면 결과를 보기 좋게 출력한다(없어도 동작).

## 프로필별 내용

### `demo` — 스크린샷 친화적 풍부한 데이터
- 사용자 4명(제주러버/부산갈매기/서울나들이/맛집헌터) — 팔로우 그래프 구성용
- 관광지 스냅샷 5건(경복궁·N서울타워·성산일출봉·협재·해운대) — TourAPI 미사용, 직접 insert
- 여행 계획 3개(서울/제주/부산) + TripDay 4 + TripPlace 5
- 찜 4, 리뷰 3, 여행 스토리 2, 팔로우 5, 체크리스트 4
- 커뮤니티 글 2, 핫플 1(자동 승인), 동행 모집글 1(+ 채팅방 자동 생성)

### `test` — 결정적 픽스처 + 엣지케이스
- 사용자 2명(테스트유저/테스트버디)
- 관광지 스냅샷 3건(경복궁·해운대 + **비라우팅 지점 설악산 대청봉**)
- **빈 계획**(day/place 없음), **최대 길이 계획**(제목 100자·여러 day/place),
  **비라우팅 동선 폴백 계획**(라우팅 불가 좌표로 동선 fallback 경로 검증)
- 찜/리뷰/스토리/팔로우/체크리스트/커뮤니티/동행 각 1건(결정적)

## 운영 안전 게이트

엔드포인트는 이중으로 보호된다.

1. `app.seed.api.enabled`(env `SEED_API_ENABLED`, 기본 `false`) 가 `true` 가 아니면 **404**.
2. 헤더 `X-Seed-Secret` 가 `app.seed.api.secret`(env `SEED_API_SECRET`) 와 불일치/미설정이면 **403**.

`SecurityConfig` 는 `/api/dev/**` 를 `permitAll` 로 열되, 실제 게이팅은 컨트롤러가 직접 수행한다.

> ⚠ **운영 배포 시 `SEED_API_ENABLED` 를 설정하지 말 것**(기본 false → 항상 404).
> 로컬 `docker-compose.yml` 에만 `SEED_API_ENABLED: "true"`, `SEED_API_SECRET: "triip-local-seed"` 가 들어 있다.

## reset 범위 (안전)

`reset=true` 는 **마커 계정이 소유한 데이터만** 정리한다 — 이메일이 `@seed.triip` 로 끝나는 계정.
실제 사용자나 부팅 시드(`demo@triip.com` 등)의 데이터는 절대 건드리지 않는다.

정리 순서(FK 의존): 찜·리뷰·팔로우·스토리·체크리스트 → 계획(days/places cascade) →
커뮤니티 글(이미지/좋아요/댓글) → 핫플(사진) → 동행글(신청/채팅방 멤버십/채팅방) → 마커 사용자.

`reset=false` 는 멱등 — 마커 계정이 이미 있으면 아무것도 하지 않는다.
