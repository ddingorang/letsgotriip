# 데모/테스트 데이터 시드

평상시 실행에서는 별도 시드 스크립트가 필요 없다. 루트 `docker compose up`은
빈 MySQL 볼륨을 처음 만들 때 `db/init/01-seed.sql`을 자동 로드한다.

수동 스크립트는 시드 코드나 TourAPI 배치 결과를 다시 만들고, 그 결과를 다음
기동용 덤프로 갱신할 때만 사용한다. 부팅 시드(`DataSeeder`/`HotplaceSeeder`/`NoticeSeeder`)는
덤프 기반 기동에서는 `SEED_BOOT_ENABLED=false`로 꺼져 있으며, 켜져도 멱등 시드로 동작한다.

## 기본 실행 흐름

```sh
docker compose up -d --build          # 전체 스택 기동, 빈 MySQL 볼륨이면 덤프 자동 로드
docker compose down -v                # 볼륨까지 삭제
docker compose up -d                  # 빈 볼륨에 db/init/01-seed.sql 재로드
```

이미 MySQL 볼륨이 있으면 Docker entrypoint는 `db/init/*.sql`을 다시 실행하지 않는다.
커밋된 덤프 상태로 되돌리려면 `docker compose down -v`로 볼륨을 지운 뒤 다시 올린다.

## 수동 갱신 스크립트

전제: 백엔드가 떠 있고 로컬 compose처럼 `SEED_API_ENABLED=true`,
`SEED_API_SECRET=triip-local-seed`가 설정되어 있어야 한다.

```sh
sh scripts/seed.sh demo --reset       # demo 시드 재생성
sh scripts/seed.sh test --reset       # test 픽스처(엣지케이스 포함) 재생성
sh scripts/seed.sh                    # demo, reset=false 멱등 호출
sh scripts/batch.sh 2000              # 관광지 배치 재수집/갱신
sh scripts/db-dump.sh                 # 현재 DB를 db/init/01-seed.sql로 덤프 갱신
```

`seed.sh`는 `POST http://localhost:9090/api/dev/seed?profile=<demo|test>&reset=<true|false>`를
`X-Seed-Secret` 헤더와 함께 호출하고, 엔티티별 삽입 건수 JSON을 출력한다.
`batch.sh`는 `POST /api/dev/attractions/batch?total=<total>`를 같은 헤더로 호출한다.
`db-dump.sh`는 `db/init/01-seed.sql`을 덮어쓰므로 공유할 데이터 상태를 의도적으로 만든 뒤에만 실행한다.

- 환경변수 `SEED_API_SECRET`(기본 `triip-local-seed`), `SEED_BASE_URL`(기본 `http://localhost:9090`)
- `batch.sh`는 기존 호환을 위해 `BASE_URL`도 읽지만, 새 문서/예시는 `SEED_BASE_URL`을 기준으로 한다.
- `seed.sh`는 `jq`가 있으면 결과를 보기 좋게 출력한다(없어도 동작).
- 저장소의 모든 스크립트가 실행 비트를 갖고 있지는 않으므로 예시는 `sh scripts/...` 형식을 사용한다.

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
