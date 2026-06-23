# 관광지 좋아요(하트) + 큐레이션

## 좋아요(Like) vs 찜(Scrap/Favorite) 분리

- **찜(Favorite, 기존)** — 개인 저장 목록(personal save list). 이 기능은 그대로 두며 변경하지 않는다.
- **좋아요(Like, 신규)** — 관광지에 대한 **공개 인기 카운터**. 사용자별 토글이며 "추천(인기)순" 정렬을 구동한다.
  - `Attraction.likeCount` 에 비정규화 저장(정렬용).
  - `attraction_like` 테이블에 사용자별 상태 저장: `(userId, contentId, contentType)` 유니크.

## 태그 키(고정)

| 키 | 의미 | TourAPI 소스 |
| --- | --- | --- |
| `food` | 맛집 | areaBasedList2 contentTypeId=39, arrange=B(조회순) |
| `culture` | 문화·역사 | areaBasedList2 contentTypeId=14 |
| `activity` | 액티비티 | areaBasedList2 contentTypeId=28 |
| `night` | 야경 | searchKeyword2 키워드 "야경"/"전망대" |

- 저장 형태는 구분자 포함 `,food,night,` (앞뒤 콤마) — `LIKE '%,food,%'` 가 정확히 매칭된다.
- 한 관광지가 두 태그에 걸리면 두 태그를 모두 누적 보유한다.
- **초기 인기도 = 가짜 좋아요 수**(데모). 배치 시 `likeCount` 가 0인 행에만 부여(실제 좋아요 보존).
  - 기본 `10~999`, 약 10%는 `1000~2000` 부스트.

## 엔드포인트

### 좋아요
- `POST /api/attractions/{contentId}/like?contentType=12&name=...` — 토글(**인증 필수**) → `{ "liked": bool, "likeCount": int }`
  - Attraction 행이 없으면 TourAPI 호출 없이 최소 스냅샷을 직접 생성(오프라인 안전).
- `GET /api/attractions/{contentId}/like?contentType=12` — 상태 조회(비로그인 허용 → `liked=false`) → `{ "liked": bool, "likeCount": int }`

### 큐레이션 조회(공개)
- `GET /api/attractions/curated?tag=food&sort=like&page=0&size=20`
  - `tag` — `food|culture|activity|night` 중 하나, 또는 빈 값(=전체 큐레이션)
  - `sort` — `like`(기본, 좋아요순) | `latest`(최신순)
  - 응답 — Spring `Page<CuratedAttractionResponse>`
    `{ contentId, contentType, title, addr, imageUrl, latitude, longitude, likeCount, tags }`
  - **"큐레이션" = 태그가 부여된(배치된) 행만** 대상. 태그 없는 행은 제외.

## 배치(개발용, dev-gated)

시드와 동일한 게이트로 보호된다:
- `app.seed.api.enabled=false` → `/api/dev/attractions/batch` 는 **404**
- `X-Seed-Secret` 헤더가 `app.seed.api.secret` 와 불일치 → **403**

실행:

```sh
./scripts/batch.sh 2000
```

- 환경변수: `SEED_API_SECRET`(기본 `triip-local-seed`), `BASE_URL`(기본 `http://localhost:9090`).
- 내부적으로 `POST /api/dev/attractions/batch?total=2000` 를 호출하고 JSON 결과 + HTTP 상태를 출력한다.
- 응답 예: `{ "food": 500, "culture": 500, "activity": 500, "night": 500, "total": 2000 }`
- `total` 을 태그 4종에 ~균등 분배하며, 여러 지역코드·페이지를 순회한다. 각 TourAPI 호출 실패는 건너뛰고 배치는 계속된다.
