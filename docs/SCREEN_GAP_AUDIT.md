# 관통 여행(Triip) — 화면별 스펙-구현 갭 진단

> 작성일: 2026-06-18
> 목적: 각 화면에 **있어야 할 것(스펙) vs 현재 구현 vs 갭**을 정리한 작업 기준 문서.
> 방법: 병렬 read-only 조사(Sonnet 에이전트 4종) + 코드 직접 대조 + BE grep 검증.
> 후속 세션은 이 문서만으로 작업 착수 가능하도록 file:line·근본원인·수정범위를 명시함.

## "있어야 할 것"의 기준 자료
- `docs/domain-architecture.md` — 12개 도메인 기능명세(47중분류·84소분류)
- `docs/design/design-system.md` — 14화면 ↔ 도메인 매핑 + 디자인 토큰
- `docs/design/handoff-chat.md` — 화면별 구성요소(특히 AI 입력/결과)
- `WORKFLOW_STATUS.md` — 45화면 IA 상태(Figma `tree/WORKFLOW.md` 기준, 원본은 repo에 없음)

## 이번 세션에서 이미 처리된 것
- `frontend/.env`에 `VITE_KAKAO_MAP_KEY=d12115bcb02411a6add202f9d467bb96` 추가(누락돼 있어 탐색 지도가 "키 누락"으로 빈 화면이었음). Vite 재시작으로 주입 확인 완료.
- 참고: FE dev 서버는 IPv6 `localhost:5173`에만 바인딩됨(`127.0.0.1` 불가) — 접속은 반드시 `http://localhost:5173`.

## 범례
🔴 작동 안 함/없음 · 🟡 불완전·오해소지·목데이터 · 🟢 정상

---

## 0. 두 줄 결론
1. **사용자가 "미구현"이라던 4종 중 탐색지도는 사실 정상**(카카오키 문제, 해결됨). 나머지 3종(챌린지·동선리포트·공지) + 알림·뱃지는 **FE 화면 껍데기만 있고 BE 도메인이 통째로 없음**(`gamification`/`trip-evaluation`/`notice`/`notification` grep 0건) → 실작동시키려면 BE 신규 개발 필수.
2. **즉시 가능한 FE-only 갭**(홈 이미지·축제칩·정렬·현재위치 지도중심·AI 뒤로가기/마감·죽은 버튼·설문저장)은 BE 없이 한 번에 처리 가능.

## 0-1. 교차 발견(중요)
- **디자인 프로토타입 14종이 repo에 없음**: `frontend/public/design/`에는 `tokens/{colors,spacing,typography}.css`만 있고, `design-system.md`가 안내하는 `ui_kits/app/index.html`·`components/`·`guidelines/`·`styles.css`는 부재 → 그 경로는 404. 시각 기준은 Figma 바이너리(`docs/design/figma/관통 여행 UI.fig`)와 handoff 설명에만 존재.
- **BE 부재 도메인**: 도메인5(평가·최적화), 도메인10(게임화: 뱃지/퀘스트/챌린지), 도메인12(공지/운영), 알림(notification) 컨트롤러·엔티티가 BE에 없음.

---

## 1. 탐색 (ExploreView) — 도메인 2
파일: `frontend/src/views/ExploreView.vue`, 스토어 `stores/attraction.js`·`stores/festival.js`, 지도 `components/common/TripMap.vue`

| 있어야 할 것(도메인2) | 현재 | 갭 | file:line |
|---|---|---|---|
| 콘텐츠 유형 필터(관광지/**축제**/문화/음식/숙박) | 칩 5종에 축제 없음, 문화시설(14)만 | 🔴 | `ExploreView.vue:177-183`(CATEGORIES), `:180` 문화시설=14 |
| 목록 **정렬** | "거리순" 버튼에 `@click` 없음 + computed에 정렬로직 0 | 🔴 무동작 | `ExploreView.vue:57`(버튼), `:186-197`(displayedPlaces) |
| 현재 위치 기반 기본화면(요청) | `navigator.geolocation` 전무, `store.list({})` 고정, 지도중심 서울 하드코딩 | 🔴 | `ExploreView.vue:261`, `TripMap.vue:14` |
| 지도 핀 | TripMap 정상 배선(displayedPlaces 바인딩) | 🟢 키만 필요(해결) | `ExploreView.vue:42-47` |
| 지역 선택·페이지네이션 | `loadAreas()` 호출만 UI 없음, 페이지네이션 없음 | 🟡 | `ExploreView.vue:264` |
| 데드코드 | 미사용 `PIN_POSITIONS`/`pinPosition()` 잔재 | 🟡 삭제무방 | `ExploreView.vue:200-211` |

### 수정 방향
- **축제칩**: `CONTENT_TYPE_MAP`에 `15:'축제행사'` 이미 존재(`attraction.js:73`). CATEGORIES 항목을 `{key:'15',label:'축제',contentTypeId:15}`로 교체/추가하면 기존 BE `/api/attractions?contentTypeId=15` 경로로 조회됨.
  - ⚠️ 단 areaBasedList(15)는 **종료된 축제도 섞임**. "진행중"만 원하면 기존 `festival.js` 스토어(날짜필터 TourAPI `searchFestival2`)를 칩에 연결 + 데이터 shape 매핑 필요(중간 작업).
- **정렬**: 이름순은 FE만(버튼 `@click`+computed `.slice().sort`). "거리순"은 사용자 좌표 필요(아래 항목과 묶임).
- **현재 위치**: ①지도중심만 = FE에서 `getCurrentPosition()`로 center ref 갱신(쉬움). ②목록도 근처 = BE에 좌표·반경 기반 `locationBasedList2` 엔드포인트 신규 필요(현재 BE는 areaCode 기반만).

---

## 2. AI 플로우 — 도메인 4 (생성 자체는 실제 BE 연동, mock 아님)
라우트: `/plan`→`/ai/plan`(조건입력)→`/ai`(확인·생성)→`/ai/result`(결과)→저장 시 `/plan`.
스토어 `stores/recommend.js`(`POST /api/recommendations` 40s 타임아웃).

| 우선 | file:line | 결함 |
|---|---|---|
| 🔴1 | `AiPlanInputView.vue:3-13` | **뒤로가기 버튼 없음** — AI 플로우 첫 화면인데 in-app 이탈 불가(dead-end). AiInputView `:7-11` 패턴 복사하면 됨 |
| 🔴2 | `AiResultView.vue:33` | 뒤로가기 버튼이 `v-else` 안 → 스켈레톤 로딩(`:4-11`) 중엔 사라짐 |
| 🟡3 | `AiInputView.vue:117-126` | 생성 로딩 오버레이(최대 30s)에 취소 버튼 없음 |
| 🟡4 | `AiResultView.vue:38,139` | "재생성"이 `/ai`로 이동→조건 비어 요약 전부 `-`. `{name:'ai-plan-input'}`로 보내야 함 |
| 🟡5 | `AiResultView.vue:97-103` | 장소 썸네일이 전부 동일 placeholder 아이콘(실제 이미지 미사용) |
| 🟡6 | `AiInputView.vue:141-146` | `history.state` 조건이 새로고침/직접진입 시 소실→요약 전부 `-` |

### 디자인 스펙(handoff 08/09)과의 갭
- 입력(08) 스펙: 지역8버튼·날짜+숙박일·동행인유형·예산·테마복수 → 대부분 구현됨.
- 결과(09) 스펙: AI점수배너·일자별탭·타임라인(장소+**이동수단**)·**축제카드(기간내 근처)**·일정수정/담기 → 이동수단·축제카드 표현 미흡.

---

## 3. 홈 (HomeView) — 도메인 2/배너
- 스펙(handoff 01): 배너·카테고리·추천. 대부분 구현.
- 🔴 **내 프로필 이미지가 정적 div**: `HomeView.vue:15` `<div class="profile-avatar" />` — authStore를 import조차 안 함. BE는 `profileImageUrl` 정상 반환(MyPage/ProfileEdit는 올바르게 사용). 홈에 auth store 연결 + `<img :src="authStore.user.profileImageUrl">` 바인딩만 하면 됨. **FE only.**

---

## 4. 마이페이지·기록·게임화 — 도메인 10

| 있어야 할 것(도메인10) | 현재 | 갭 | file:line / BE |
|---|---|---|---|
| 내정보/프로필수정 | MyPage / ProfileEditView | 🟢 (ProfileEdit PATCH 자동호출 여부 확인필요) | — |
| 앨범 생성/공유 | AlbumDetailView | 🟢 `GET/POST /users/me/albums` | — |
| **알림** | 벨 버튼 장식용 | 🔴 | `MyPageView.vue:8`(`@click` 없음), `:12`(notif-dot CSS 상시), store/api/BE 전무 |
| 뱃지 목록/상세 | BadgesView | 🔴 정적 | `BadgesView.vue` script 비어있음, 배열 하드코딩(`:124-190`), BE 없음 |
| 퀘스트/**챌린지** 진행갱신 | ChallengeDetailView | 🔴 하드코딩 | `ChallengeDetailView.vue:100-101`(빈 script), `:41`(70% 고정), MyPage `:60`서 진입, `:68` "준비중·Phase 2" 라벨 |
| **공지 조회** | 없음 | 🔴 NOT BUILT | view·route·store·api·BE 전부 부재 |

### 알림/공지 구현 옵션
- 1차(FE만): 정적 목 드로어/리스트로 흉내.
- 실데이터: BE에 `notification` 도메인(이벤트 기반: 신청·댓글·승인 등) + 도메인12 `Notice` CRUD 신설 필요.

---

## 5. "미구현 4종" 정밀 분류

| 기능 | 도메인 스펙(있어야 할 것) | 분류 | 근거 file:line / BE |
|---|---|---|---|
| 탐색지도 | 도메인2: 지도핀·필터·정렬 | 🟢 **정상** | TripMap 완성, 카카오키 문제였음(해결) |
| 동선리포트 | 도메인5: 예산산출·이동시간추정·비효율탐지·순서대체제안·일자별리포트 | 🟡 PARTIAL | `PlanReportView.vue` plan 실데이터 O(`stores/plan.js loadPlan`), 거리=가짜추정(`:183` "3km 평균 가정"), 적용버튼 no-op(`:212`). **BE 평가도메인 전무** |
| 챌린지 | 도메인10: 챌린지 참여·활동기반 진행갱신 | 🟡 PARTIAL | view·라우팅(`router:42`)·진입(MyPage`:60`) O, 전부 하드코딩, BE 없음 |
| 공지 | 도메인12 CRUD + 도메인10 조회 | 🔴 NOT BUILT | 전 스택 부재 |

**공통 근본원인**: BE에 `gamification`(뱃지/챌린지), `trip-evaluation`(동선 최적화), `notice/notification` 컨트롤러·엔티티가 없음(grep 0건). FE 화면만 정적으로 존재해 "있는 듯 안 되는" 상태.

---

## 6. 전체 IA 커버리지(나머지 화면, 참고)

| 화면 | 있어야 할 것 | 상태 | 비고 |
|---|---|---|---|
| 장소상세 PlaceDetailView | 히어로·편의시설·사진갤러리·일정담기 | 🟢 | 담기 BE연동 |
| 계획 PlanView | 타임라인·지도미리보기·장소순서변경 | 🟡 | **planStore 배선됨**(status문서의 "mock 로컬배열"은 outdated). 단 동행섹션 하드코딩(`:150-154`), 전체보기(`:113`)·참여하기(`:128`) 죽은버튼, `plan.spots`(`:54`)·`plan.destination`(`:52`)가 BE PlanDetail에 없어 빈값 가능, 순서변경 UI 없음 |
| 커뮤니티/상세/글쓰기 | 게시판CRUD·댓글·좋아요·핫플 | 🟡 | API O, **표시필드 깨짐(작성자/이미지/카테고리)** — 사용자 지시로 이번 범위 제외(parked). 매핑표는 `WORKFLOW_STATUS.md §4.A` |
| 핫플 등록/상세/중복확인 | 지도위치선택·승인 | 🔴 | 등록 500(`category` NOT NULL, parked), 중복확인 화면 없음 |
| 동행 목록/상세/작성/신청자/채팅 | 모집·신청·승인·그룹·할인 | 🔴 | 작성 500(`chat_room_id` NOT NULL, parked), 신청 로컬만, 목록 store 목, 신청자관리 BE는 있으나 FE 미배선 |
| 뱃지 BadgesView | 레벨·퀘스트·뱃지그리드 | 🔴 | 정적 |
| 체크리스트 ChecklistView | D-day·카테고리·템플릿·일자/장소연결·알림 | 🟡 | 로컬상태, 계획연결 없음 |
| 취향설문 PreferenceSurveyView | 설문→저장 | 🟡 | 저장 안 됨(skip/next 모두 `/home`로 폐기) |
| 결제/예약확정 PaymentView/ConfirmationView | (확장 도메인) | 🟡 | FE고유 전부 목, Confirmation `:13/:97-109` 하드코딩, 공유버튼 `:63-72` `@click` 없음 |
| 인증(가입/로그인/refresh/내정보/탈퇴) | 도메인1 | 🟢 | 라이브 검증됨 |

---

## 7. 알려진 BE 버그(이번 범위 외, parked) — `WORKFLOW_STATUS.md §2`
- `POST /companion/posts` 500: `chat_room_id` NOT NULL인데 저장 전 채팅방 미생성 → 서비스에서 ChatRoom 먼저 생성·연결 또는 컬럼 nullable.
- `POST /community/hotplaces` 500: `category` NOT NULL인데 `HotPlaceCreateRequest`에 category 필드 없음 → DTO에 추가.
- `POST /api/festivals/sync` 403: Spring Security 차단 → permit/권한 정책 정리(그래서 `/api/festivals` 빈 배열).

---

## 8. 권장 실행 계획

### 그룹 A — FE only, 즉시(저위험, 병렬 처리 가능) — ✅ 2026-06-18 전부 완료
1. ✅ 홈 프로필 이미지 바인딩 (`HomeView.vue`) — authStore 연결 + 기본깨짐 경로(`/images/default-profile.png`) placeholder 처리
2. ✅ 탐색 축제 칩 — CATEGORIES `문화시설(14)`→`축제(15)` 교체(`ExploreView.vue`)
3. ✅ 탐색 정렬 — `거리순↔기본순` 토글 작동 + 항목별 거리(km) 표시
4. ✅ 탐색 지도중심 현재위치 — `getCurrentPosition`→`mapCenter`/거리순 자동, 지도엔 가까운 8곳만 핀
5. ✅ AI 마감 — AiPlanInput 뒤로가기 추가, AiInput 생성취소(store `cancelGenerate` AbortController), AiResult 빈상태 뒤로가기 + 재생성→`/ai/plan`(조건입력)로 수정
6. ✅ 죽은 버튼/데드코드 — PlanView 동행 전체보기·참여하기→`/community?tab=companion`, CommunityView가 `?tab` 쿼리로 초기탭 선택, ConfirmationView 공유하기→Web Share+클립보드, ExploreView `PIN_POSITIONS` 삭제
7. ✅ 취향설문 저장 — `PreferenceSurveyView` 다음 시 localStorage(`triip.preferences`) 저장 + 재진입 복원 (BE 취향 엔드포인트 없음 확인)

> 추가 처리(그룹 A 범위 밖이나 함께): 마이페이지/커뮤니티 벨→`/notifications` 신규 화면(알림·공지 2탭, FE 정적 시드 `stores/notification.js`), 미읽음 점 조건부.

### 그룹 B — BE 신규 개발 동반(큰 작업)
1. ✅ 2026-06-18 완료 — 탐색 "내 위치 근처 목록": TourAPI `locationBasedList2`(거리순 arrange=E) + BE `mapX/mapY/radius` 파라미터. `AttractionTourApiClient.fetchLocationBased`, `AttractionSearchRequestDto`(mapX/mapY/radius + hasCoords/clampedRadius), `AttractionService.search` 분기, 컨트롤러 파라미터, 캐시키 좌표 포함. FE `ExploreView`가 위치 권한 시 좌표 전송. **검증**: 서울시청 좌표→서울도서관 등 거리순 반환.
2. ✅ 2026-06-18 완료 — 알림 `notification` 도메인(이벤트 기반): `Notification` 엔티티(`notifications_user`)/repo/dto/service/controller(`/api/notifications` GET·unread-count·read-all·{id}/read, 인증). **이벤트 방식** = `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)`. 발행 지점 2곳: 동행 신청→모집글 작성자(`CompanionService.apply`), 댓글→게시글 작성자(`CommunityService.createComment`, 본인 제외). FE `notificationApi` + `stores/notification.js`가 실데이터 연동(상대시간 매핑, 미로그인 시 시드 폴백), 벨 미읽음 점·모두읽음 BE 연동. **검증**: 댓글/동행 신청→수신자 알림 적재, unread-count 2→0(read-all). ⚠️ **함정**: AFTER_COMMIT 리스너의 DB 쓰기는 새 트랜잭션 필요 — `NotificationService.create`에 `@Transactional(REQUIRES_NEW)` 없으면 조용히 미커밋(빈 결과)됨. 새 도메인 repo는 `RepositoryConfig`에도 등록.
3. 챌린지/뱃지 — `gamification` 도메인(활동이벤트→진행갱신) — 미착수
4. 동선리포트 — `trip-evaluation` 도메인(이동시간·비효율·대체동선) — 미착수
5. ✅ 2026-06-18 완료 — 공지 `Notice` CRUD 조회: `notice` 도메인 신설(entity/repository/dto/service/controller + `NoticeSeeder` 3건 + `GeneralException`/`NOTICE404`). `SecurityConfig` GET `/api/notices/**` permit, `RepositoryConfig`에 `com.trip.notice.repository` 등록(⚠️ 이 프로젝트는 `@EnableJpaRepositories` basePackages 명시형 — 새 도메인 repo는 반드시 여기 추가). FE `noticeApi` + `stores/notification.js`가 실데이터 연동(시드 폴백). **검증**: `/api/notices` 200, 필독 pinned 우선·최신순, FE 프록시 도달 확인.

> 남은 ②③④는 모두 **이벤트 기반/외부연동**이라 설계 결정(어떤 이벤트가 알림을 트리거할지, 거리매트릭스 소스 등)이 필요 — 착수 전 범위 합의 권장.

---

## 참조 파일 빠른 색인
- 라우터: `frontend/src/router/index.js`
- 탐색: `frontend/src/views/ExploreView.vue`, `stores/attraction.js`, `stores/festival.js`, `api/festival.js`, `components/common/TripMap.vue`
- AI: `views/AiPlanInputView.vue`, `AiInputView.vue`, `AiResultView.vue`, `stores/recommend.js`
- 계획: `views/PlanView.vue`, `views/PlanReportView.vue`, `stores/plan.js`
- 마이/게임화: `views/MyPageView.vue`, `BadgesView.vue`, `ChallengeDetailView.vue`
- 홈: `views/HomeView.vue`, `stores/auth.js`
- 디자인: `docs/design/design-system.md`, `docs/design/handoff-chat.md`, `frontend/public/design/tokens/`
