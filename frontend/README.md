# EnjoyTrip Vue

Vue Router, Pinia, Axios를 사용하는 국내 여행 UI 프로토타입입니다. 장소 탐색, 일정 관리, 취향 추천, 핫플 검수, 여행 커뮤니티, 내 여행 기록 화면을 포함합니다.

## 기술 선택 이유

현재 화면 규모만 보면 Vue 단일 컴포넌트 상태로도 동작하지만, 과제/협업 기준에서 `vue-router`, `pinia`, `axios` 사용 여부가 확인될 수 있어 세 계층을 명확히 분리했습니다.

- `vue-router`: 메뉴 선택을 내부 변수만 바꾸는 방식에서 `/explore`, `/plan`, `/recommend` 같은 URL 기반 화면 전환으로 바꿔 직접 진입과 뒤로가기를 확인할 수 있게 했습니다.
- `pinia`: 현재 메뉴, 배경 장면, 자세히 패널, 장소 목록처럼 여러 컴포넌트가 공유하는 UI 상태를 한 store에서 관리하게 했습니다.
- `axios`: 아직 실제 백엔드가 없어도 API 연동 구조를 보여주기 위해 public mock JSON을 axios client로 읽고, 실패하면 로컬 데이터로 fallback하게 했습니다.

## 화면별 유즈케이스와 API

현재 Vue 화면은 7개 메뉴로 구성되어 있고, 실제 백엔드 연동 전에는 `public/mocks/tourism-places.json`만 Axios로 직접 읽습니다. 아래 API는 화면이 완성형으로 가기 위해 필요한 계약 기준입니다.

| 화면 | 주요 유즈케이스 | 필요한 API | 현재 mock |
|---|---|---|---|
| 홈 `/` | 최근 본 장소 확인, 저장 장소 확인, 다음 작업으로 이동 | `GET /api/v1/users/me`, `GET /api/v1/tourism/places`, `GET /api/v1/trip-plans/recent` | 정적 화면 데이터 |
| 장소탐색 `/explore` | 지역/유형/검색어로 장소 찾기, 장소 상세 확인, 여행 노트에 담기 | `GET /api/v1/tourism/areas`, `GET /api/v1/tourism/content-types`, `GET /api/v1/tourism/places`, `GET /api/v1/tourism/places/{placeId}`, `POST /api/v1/trip-plans/{planId}/items` | `public/mocks/tourism-places.json` |
| 일정관리 `/plan` | 일정 목록 확인, 장소 추가, 동선 재정렬, 공유 링크 생성 | `GET /api/v1/trip-plans`, `GET /api/v1/trip-plans/{planId}`, `POST /api/v1/trip-plans`, `POST /api/v1/trip-plans/{planId}/items`, `PATCH /api/v1/trip-plans/{planId}/items/reorder`, `POST /api/v1/trip-plans/{planId}/share` | `mocks/trip-plans.json`, `mocks/trip-plan-detail.json` |
| 취향추천 `/recommend` | 취향 조건 조정, 추천 후보 확인, 추천 근거 확인, 일정으로 복사 | `POST /api/v1/recommendations`, `GET /api/v1/recommendations/{recommendationId}`, `POST /api/v1/recommendations/{recommendationId}/copy-to-plan`, `GET /api/v1/trip-plans/{planId}/compare` | `mocks/recommendation.json`, `mocks/plan-compare.json` |
| 핫플검수 `/hotplace` | 신규 제보 확인, 사진/좌표 검수, 중복 제보 병합 | `GET /api/v1/hotplaces`, `GET /api/v1/hotplaces/{hotplaceId}`, `POST /api/v1/hotplaces`, `POST /api/v1/hotplaces/{hotplaceId}/photos`, `PATCH /api/v1/hotplaces/{hotplaceId}/moderation` | `mocks/hotplaces.json` |
| 여행톡 `/community` | 인기 글 확인, 여행글 작성, 공유 일정 확인, 댓글 관리 | `GET /api/v1/board/posts`, `GET /api/v1/board/posts/{postId}`, `POST /api/v1/board/posts`, `PATCH /api/v1/board/posts/{postId}`, `DELETE /api/v1/board/posts/{postId}`, `GET /api/v1/notices` | `mocks/board-posts.json`, `mocks/notices.json` |
| 내여행 `/mypage` | 내 정보 확인, 취향 태그 수정, 저장 장소 정리, 내 일정 열기 | `POST /api/v1/auth/login`, `GET /api/v1/users/me`, `PATCH /api/v1/users/me`, `GET /api/v1/users/me/saved-places`, `GET /api/v1/users/me/trip-plans` | `mocks/user-me.json`, `mocks/auth-login.json`, `mocks/trip-plans.json` |

## API 연동 우선순위

1. `장소탐색`: 검색/필터/상세가 앱의 기본 흐름이라 `tourism` API를 가장 먼저 연결합니다.
2. `일정관리`: 장소를 담은 뒤 실제 사용 흐름이 이어지려면 `trip-plans` CRUD와 정렬 API가 필요합니다.
3. `내여행/auth`: 로그인 사용자 기준 저장 장소와 내 일정을 보여주기 위해 인증과 `me` API를 연결합니다.
4. `취향추천`, `핫플검수`, `여행톡`: MVP 이후 추천/제보/커뮤니티 기능으로 확장합니다.

## 실행

```bash
npm install
npm run dev
```

## 빌드

```bash
npm run build
```

## 포함하지 않는 파일

대외비/개인/비밀 이름 패턴, env/키 파일, `node_modules/`, `dist/`는 `.gitignore`로 제외합니다.
