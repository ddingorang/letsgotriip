# Triip 프로젝트 종합 분석 리포트

> 작성일: 2026-06-19 · 작성: 테크리드 종합(영역별 분석 + 적대적 검증 기반)
> 근거 표기 규칙: 사실은 `파일경로:라인` 으로 근거를 붙이고, 추정은 "추정"으로 명시. 적대적 검증에서 반증(refuted)된 주장은 "결함"이 아니라 "검증 결과 반증됨"으로 분류.

---

## 1. 프로젝트 개요 & 기술 스택

Triip은 여행 계획·탐색·동행·커뮤니티를 묶은 모바일 웹 앱(SPA)이다. AI 일정 추천, 관광지/축제 탐색, 동행 모집·채팅, 커뮤니티/핫플 공유가 핵심 도메인이다.

### 백엔드 (BE/)
- **언어/런타임**: Java 21, Spring Boot 3.5.x (`BE/build.gradle:1-14`)
- **빌드**: Gradle
- **패키지 루트**: `com.trip` (도메인별 패키지: attraction, chat, community, companion, festival, plan, preprocessing, recommend, user, global)
- **데이터 영속**: JPA(MySQL), MongoDB(채팅 메시지)
- **캐시/세션**: Redis (세션 회전, 추천 캐시, 조회수)
- **메시징**: RabbitMQ(STOMP 릴레이) + WebSocket/STOMP
- **AI**: Spring AI ChatClient (gpt-4o-mini, SSAFY GMS 프록시), BeanOutputConverter 구조화 출력
- **외부 API**: 한국관광공사 TourAPI KorService2 (관광지/축제)
- **보안**: JWT(AccessToken) + Refresh 쿠키(HttpOnly) + Redis 세션, BCrypt(strength 12), OAuth2(카카오/구글)
- **서버 포트**: 9090 (`BE/src/main/resources/application.yaml:85`)

### 프론트엔드 (frontend/)
- **프레임워크**: Vue 3 (`<script setup>`), Vue Router, Pinia
- **HTTP**: axios 단일 인스턴스 + 401 single-flight refresh 인터셉터 (`frontend/src/api/http.js`)
- **지도**: Kakao Maps SDK(CDN 동적 로드) — 단, package.json에 미사용 leaflet 의존성 잔존(`frontend/package.json:13`)
- **빌드/개발**: Vite (dev proxy로 `/api`→:9090, `/api/tour`→data.go.kr)

### 인프라
- docker-compose(`BE/trip-docker/docker-compose.yml`): MySQL(3307:3306), MongoDB(27017), Redis, RabbitMQ — **인프라 4종만 정의, 앱 컨테이너 미포함**

---

## 2. 아키텍처

### BE 도메인 구조
`com.trip` 단일 모듈 아래 도메인별 패키지(controller/service/repository/entity/dto). 도메인 9개가 실제 컨트롤러까지 구현됨: attraction, chat, community, companion, festival, plan, recommend, user, (+ preprocessing). `global` 패키지에 config/security/util/예외처리 횡단 관심사 집약.

### FE 구조
`frontend/src/` 아래 views(화면), components(common/community), stores(Pinia), api(http 인스턴스 + 도메인별 래퍼), router. main.js가 `auth.bootstrap()`(silent refresh) 완료 후 라우터 마운트 → 새로고침 시 인증 유지.

### 인프라: 실제 사용 vs 선언만
| 인프라 | 상태 | 근거 |
|---|---|---|
| MySQL | **실사용** (JPA 9개 도메인 리포지토리) | `application.yaml:19` |
| MongoDB | **실사용** (채팅 메시지 저장) | `ChatMessageRepository.java` |
| Redis | **실사용** (세션 회전/추천 캐시/조회수) | `application.yaml:39` |
| RabbitMQ | **부분 사용** — STOMP 브로커 릴레이는 동작하나, `convertAndSend` 단일인자 발행은 dead code | `ChatService.java:48` |
| Dockerfile(앱) | **부재** — 앱 자체 컨테이너화 안 됨 | `BE/trip-docker/docker-compose.yml:1-48` |
| 프로파일 분리 | **부재** — 호스트 localhost 하드코딩, prod 분리 불가 | `application.yaml:19,36,39,44` |

---

## 3. 도메인별 완성도 상태표

| 영역 | status | 핵심 요약 | End-to-End 동작 |
|---|---|---|---|
| 인증/사용자 | **partial** | 회원가입/로그인/refresh/로그아웃/내정보/탈퇴/프로필수정/OAuth는 BE까지 완결. 온보딩 취향설문·bio·MyPage 통계는 mock | 코어 인증 OK / 온보딩·MyPage NG |
| 관광지 탐색 | **complete** | TourAPI 프록시 검색·상세·지역코드, Kakao 지도, '일정에 담기'까지 완결 | OK (평점·리뷰만 정적 더미) |
| 축제 | **broken** | BE 배치/DB/엔드포인트 완비되었으나 **FE가 BE를 우회**하고 TourAPI 직접 호출 | BE 경로 미사용 NG |
| AI 추천 | **complete** | 입력→생성→결과→계획저장 end-to-end. Spring AI + 검증 + 멱등 저장 | OK (preprocessing/STT는 고아) |
| 일정 계획 | **partial** | BE CRUD 8개 견고하나 FE 목록이 비반응적으로 안 그려짐, 편집/삭제 UI 없음 | 일부 NG (목록/편집 깨짐) |
| 커뮤니티+핫플 | **partial** | 게시글/댓글/좋아요는 완결. 핫플은 PENDING→APPROVED 승인 경로 부재로 단절 | 게시글 OK / 핫플 노출 NG |
| 동행 | **partial** | 모집/신청/승인은 실서버 배선. 신청취소 가짜, isApplied 미조회, 채팅버튼 하드코딩 | 코어 OK / 취소·상태 NG |
| 채팅 | **broken** | BE STOMP/Mongo/릴레이 구성되었으나 **FE에 STOMP 클라이언트 전무** | NG (로컬 mock) |
| FE 인프라 | **partial** | 라우팅/인증/인터셉터 dev 기준 완결. baseURL='' 프로덕션 빌드 미동작 | dev OK / prod NG |
| 기타 화면(결제/예약/뱃지/체크리스트 등) | **partial** | HomeView만 BE 연동. 나머지 6화면 전부 정적 하드코딩 | Home OK / 6화면 NG |
| 보안 횡단 | **partial** | JWT/OAuth/회전/해시 견고. 쿠키 secure(false), WS CORS *, ROLE_ 접두어 누락 | 인증 OK / 운영보안 주의 |

---

## 4. FE-BE 연동 현황 분류

### BE 배선됨 (실데이터 end-to-end)
- 인증 전체: `/auth/signup,login,refresh,logout`, `/users/me`(GET/PATCH/DELETE), OAuth2 (`AuthController.java`, `UserController.java`)
- 관광지: `/api/attractions`(검색/지역/상세), '일정에 담기' POST (`AttractionController`, `PlanController`)
- AI 추천: `/api/recommendations` 4종 (`RecommendController.java`)
- 일정: `/api/plans` POST/GET/GET-상세, place add (`PlanService.java`)
- 커뮤니티: 게시글 CRUD/좋아요/댓글, 핫플 목록·상세·등록 (`CommunityController`, `HotPlaceController`)
- 동행: 모집 생성/목록/상세/신청/승인/거절 (`CompanionController.java`)
- 홈: `/community/hotplaces`, `/community/posts` (`HomeView.vue:98-100`)

### Mock (로컬/하드코딩, BE 미호출 또는 폴백 의존)
- 온보딩 취향설문 (`PreferenceSurveyView.vue:119` router.push만)
- MyPage 통계/계획/앨범/뱃지 (`MyPageView.vue:239,251,259`)
- 채팅 메시지 송수신 (`ChatRoomView.vue:103,105-119` 로컬 Pinia)
- 결제/예약확정 (`PaymentView.vue:118`, `ConfirmationView.vue:13`)
- 뱃지/챌린지/체크리스트 (`BadgesView.vue:124-190`, `ChecklistView.vue:200-213`)
- PlaceDetail 평점·리뷰 (`PlaceDetailView.vue:188-192`)

### Static (프레젠테이션 컴포넌트 / 정적 화면)
- AppHeader, PlaceCard, BottomNav, App shell (props 기반)
- AlbumDetailView (로컬 배열, BE AlbumController 존재하나 FE 미호출)
- CommunityView 핫플 지도, HotplaceDetailView 미니맵 (정적 CSS 목업)

### 미구현 (BE 자체 부재)
- 챌린지/뱃지/퀘스트 컨트롤러 없음
- 결제(payment)/예약(booking) 컨트롤러·패키지 없음
- 공지(notice) 컨트롤러 없음
- 온보딩 취향설문 저장 엔드포인트/엔티티/컬럼 없음

### BE는 있으나 FE 미배선 (dead/untested)
- `/api/festivals`(GET) — FE가 정의만 하고 미사용 (`frontend/src/api/index.js:24-26`)
- `/community/images` 멀티파트 업로드 — FE 미호출
- 동행 update/delete/close, 신청취소(DELETE), 핫플 승인/반려/pending
- 채팅 메시지 히스토리 조회 REST (Repository 메서드만 존재)
- `/users/me/albums/{id}` (AlbumController 존재, FE albumApi 없음)

---

## 5. 검증된 핵심 결함 & 리스크

### 5-1. 적대적 검증으로 confirmed된 결함 (severity 순)

| # | severity | 영역 | 결함 | 근거 |
|---|---|---|---|---|
| 1 | high | 채팅 | FE에 STOMP/WebSocket 클라이언트 전무 → 실시간 송수신 미연결, 로컬 mock | `ChatRoomView.vue:103,105-119` / FE 전체 stomp grep 0건 (확인됨) |
| 2 | high | 채팅 | RabbitMQ `convertAndSend(단일인자)` → 설정 exchange/routing key 미사용 + 소비자 부재 dead code | `ChatService.java:48`, @RabbitListener 0건 |
| 3 | high | 축제 | FE가 BE `/api/festivals` 우회, TourAPI 직접 호출 + **하드코딩 서비스키** + mock 폴백 | `frontend/src/api/festival.js:5,17-19,45`, `stores/festival.js:54,59` |
| 4 | high | 축제/FE인프라 | TourAPI 서비스키가 소스 하드코딩 + `.env.example` 평문 커밋 → 클라이언트 노출 | `festival.js:17-19`, `frontend/.env.example:3` |
| 5 | high | 인증 | 온보딩 취향설문 완전 mock — BE 저장 로직/엔티티/컬럼 전무 | `PreferenceSurveyView.vue:110,119`, `User.java`(취향 컬럼 없음), `schema.sql:8-22` |
| 6 | high | 인증 | 프로필 한줄소개(bio) 미저장 — FE 미전송 + DTO/엔티티에 필드 자체 없음 | `ProfileEditView.vue:90-93`, `UserUpdateRequestDto.java:3-6`, `User.java:31-68` |
| 7 | high | 커뮤니티 | 핫플 등록(PENDING)→목록(APPROVED만) 단절 + 승인 UI 없음 → 등록 핫플 영구 미노출 | `HotPlace.java:49`, `HotPlaceService.java:36`, `api/index.js:67-72` |
| 8 | high | 커뮤니티 | 핫플 지도 핀이 BE에 없는 `hp.x/hp.y` 참조 → 실데이터 핀 좌상단 군집(left/top auto 폴백) | `CommunityView.vue:110`, `stores/hotplace.js:37-66`, `HotPlaceSummaryResponse.java:13-14` |
| 9 | high | 일정 | PlanView가 plans를 비반응적으로 destructure → 로드 후 목록 영영 안 그려짐 | `PlanView.vue:146`(`const plans = planStore.plans`), `stores/plan.js:27` |
| 10 | high | 일정 | PlanReportView 거리/시간/이동 통계가 하드코딩 더미 공식 | `PlanReportView.vue:183-206` |
| 11 | high | 일정 | 경로적용 버튼 미구현(라우팅만), 일정 편집/삭제/순서변경 UI 전무 | `PlanReportView.vue:208-214`, `stores/plan.js:68-172` vs `PlanView.vue:96-103` |
| 12 | high | 동행 | 신청취소가 BE DELETE 있음에도 FE 로컬 상태만 변경(가짜) | `CompanionDetailView.vue:184-187`, `CompanionController.java:121-129` |
| 13 | high | 동행 | isApplied 서버 미조회 → 새로고침 시 항상 미신청 표시, 중복신청 시도 가능 | `CompanionDetailView.vue:168`, `CompanionService.java:146-149` |
| 14 | high | 기타화면 | 결제 흐름 FE 전용 더미 — 실제 결제/예약 생성 없음(/confirmation 라우팅만) | `PaymentView.vue:118,136-181` |
| 15 | high | 기타화면 | 예약확정 화면 정적 더미 — 예약번호/티켓/금액 하드코딩 | `ConfirmationView.vue:13,97-108` |
| 16 | high | 보안 | 인증 쿠키 `secure(false)` → refreshToken/sessionId 평문 HTTP 노출 | `CookieUtil.java:43` (확인됨) |
| 17 | high | 보안 | WebSocket STOMP `setAllowedOrigins("*")` → REST 화이트리스트와 불일치 | `WebSocketBrokerConfig.java:52-53`, `WebConfig.java:31` |
| 18 | high | FE인프라 | API baseURL='' 하드코딩 → dev 프록시 의존, **프로덕션 빌드 시 모든 API 실패** | `http.js:20`(확인됨), `.env.example:2`(VITE_API_BASE_URL 미사용) |
| 19 | med | AI추천 | 프롬프트가 테마 영문키(sea/food 등)를 한글 의미 없이 LLM 전달 + 분석데이터 미반영 → 개인화 한계 | `RecommendService.java:341-373`, `AiPlanInputView.vue:284-292` |
| 20 | med | 축제 | 배치 lDongRegnCd 값을 areaCode 컬럼 저장, service는 areaCode로 필터 → 코드체계 불일치 | `FestivalItemProcessor.java:53`, `FestivalService.java:31`, `TourApiResponse.java:48,50` |
| 21 | med | 축제 | ItemWriter saveAll만 → TourAPI에서 사라진 미래/null end_date 행사 영구 잔존 | `FestivalItemWriter.java:20-23`, `FestivalSyncJobConfig.java:69-82` |
| 22 | med | 동행 | BE status(OPEN/CLOSED) vs FE 한글('마감임박') 비교 불일치 → 마감 배지 미동작, 영문 노출 | `CompanionStatus.java:4-8`, `CompanionDetailView.vue:36`, `stores/companion.js:26` |
| 23 | med | 동행 | 목록 요약 DTO에 currentMembers 없음 → 카드 인원수 기본값(1) 고정 | `CompanionPostSummaryResponse.java:10-20`, `stores/companion.js:27` |
| 24 | med | 동행 | 방장 채팅 버튼이 chatRoomId 무시하고 `/chat/1` 하드코딩 | `CompanionDetailView.vue:140`, `CompanionPostResponse.java:23` |
| 25 | med | 일정 | AI draft→plan 변환 시 contentType 12(관광지) 하드코딩 → 음식점/축제 등 오분류 | `PlanService.java:322-323` |
| 26 | med | 보안 | GrantedAuthority에 ROLE_ 접두어 누락 → 향후 hasRole 도입 시 즉시 발현되는 **잠재** 권한결함 | `JwtAuthenticationFilter.java:37`, `UserRole.java:5-7` |
| 27 | med | 인프라 | `app.frontend.allowed-origins` 프로퍼티 미정의 → CORS Origin 항상 기본값 | `WebConfig.java:22`, `application.yaml:91-99` |
| 28 | med | 인증 | BE 비밀번호 최소길이/형식 검증 부재(FE 8자 검증만) → API 직접호출 우회 가능 | `SignupRequestDto.java:18-20`, `LoginRequestDto.java:3` |
| 29 | med | 관광지 | detailCommon2가 좌표/주소 미반환 → upsertSnapshot 위경도 null 저장 가능 | `AttractionService.java:171-189` |

### 5-2. 검증 결과 partial (부분 성립)

- **채팅방 목록 템플릿 필드 불일치**: lastMsg/time/ended는 매핑 누락(confirmed)이나, **unreadCount는 0으로 실제 매핑됨(반증)** → 전체 partial. `ChatRoomListView.vue:31-41` vs `stores/companion.js:50-57`
- **MockSTTManager가 환경에 따라 가짜 텍스트 반환**: 두 빈 공존은 사실이나 @Primary(Whisper) 때문에 **환경 무관하게 Mock 자동선택 경로 없음**(반증) → partial. `MockSTTManager.java:10-11`, `WhisperSTTManager.java:20`
- **OAuth2 raw RuntimeException이 GlobalExceptionHandler 우회**: 비일관 500 결과는 성립하나, 원인은 'raw 예외'가 아니라 **'필터체인 위치'(@RestControllerAdvice 범위 밖)** → 메커니즘 설명 부정확, partial. `OAuth2SuccessHandler.java:42-43`

### 5-3. 검증 결과 반증됨 (refuted — 결함 아님)

- **`/sync` 익명 접근 시 401 반환이 결함(403 의도와 다름)**: 코드 주석(`SecurityConfig.java:69-70`)이 "미인증 API는 401 반환"을 **명시적으로 문서화한 의도된 동작**. 403 의도 근거 전무 → 반증됨.

---

## 6. 보안 관점 발견사항

| severity | 발견 | 근거 |
|---|---|---|
| high | 인증 쿠키 `secure(false)` — 평문 HTTP 전송 노출 | `CookieUtil.java:43` |
| high | WebSocket STOMP `setAllowedOrigins("*")` — REST CORS 화이트리스트와 불일치 | `WebSocketBrokerConfig.java:52-53` |
| high | TourAPI 키 FE 소스 하드코딩 + .env.example 평문 커밋 | `festival.js:17-19`, `.env.example:3` |
| med | BE 비밀번호 최소길이/형식 검증 부재 | `SignupRequestDto.java:18-20`, `LoginRequestDto.java:3` |
| med | ROLE_ 접두어 누락(잠재 권한결함) | `JwtAuthenticationFilter.java:37` |
| low | 로그아웃 후 access token denylist 미도입(최대 1h 유효, 의도적 수용) | `AuthService.java:118` |
| low | CSRF 전역 비활성화(JWT-header 안전하나 refresh는 SameSite=Lax만 의존) | `SecurityConfig.java:41` |
| low | 관리자 GET `/community/hotplaces/pending`이 security permitAll에 매칭(서비스단 verifyAdmin로 데이터는 보호되나 principal null시 NPE 추정) | `SecurityConfig.java:60`, `HotPlaceService.java:110-111` |
| low | RabbitMQ/MySQL 기본 자격증명 평문(guest/guest, root/password) | `docker-compose.yml:7,35-36`, `application.yaml:21,48-49` |
| low | 과거 JWT secret 커밋 이력 → 운영 시 로테이션 필요 | `BE/.env.example:2` |

**긍정 요소**: 시크릿 환경변수 외부화(하드코딩 없음, festival 키 제외), refresh 토큰 SHA-256 해시 저장 + 회전 + 재사용 탐지 + overlap window, BCrypt strength 12, 전역 예외 핸들러 500 마스킹.

---

## 7. 문서 vs 실제 코드 차이 (outdated 항목)

권위 있는 설계는 **`BE/docs/system-design.md`(v0.3, com.trip/JPA/Gradle/Java21/포트9090)** 로 현행 코드와 일치. 그 외 다수 문서가 outdated.

| 문서 | 기술 내용 | 실제 코드 | 판정 |
|---|---|---|---|
| `SETUP.md:6-10,46-53` | backend/ 디렉터리, 포트 8080, DB ssafy_ai, localhost:3306 | 실제 BE/, 9090, trip_chat, 3307 | **outdated** |
| `docs/system-design.md`(v0.1) | com.ssafy.trip, MyBatis, :8080, enjoytrip | com.trip, JPA, :9090 | **outdated**(문서 스스로 superseded 표기) |
| `WORKFLOW_STATUS.md:56,99,152` | 핫플 등록 500(category NOT NULL, DTO에 category 없음) | `HotPlaceCreateRequest.java:14`에 `@NotNull category` 존재 → **해소됨** | **outdated**(확인됨) |
| `BE/docs/system-design.md:17` | OAuth access token URL 노출(미수정) | `OAuth2SuccessHandler.java:74-76` URL 미포함 | **outdated** |
| `BE/docs/system-design.md:26,189` | 로그아웃 엔드포인트 없음/보강예정 | `AuthController.java:77-85` 구현됨 | **outdated** |
| `BE/docs/system-design.md:53-55` | attraction/plan/recommend = ★신규(미구현) | 컨트롤러까지 구현 완료 | **outdated** |
| `docs/domain-architecture.md:105-110` | community/companion/chat = MVP 제외/확장 | 컨트롤러까지 구현됨 | **스코프 괴리** |
| `docs/system-design.md:88` | jjwt 0.12.x | `build.gradle:53-55` jjwt 0.11.5 | 세부 불일치 |
| `BE/docs/system-design.md:75,220` | 발표 전 ddl-auto: validate 전환 | `application.yaml:26` ddl-auto: update 유지 | 미전환 |

> 추정: WORKFLOW_STATUS.md(2026-06-18)가 보고한 결함 상당수가 작성일 이후 커밋(~06-19)으로 이미 수정된 것으로 보이므로, 이 문서 전체를 신뢰 기준으로 삼으면 안 된다.

---

## 8. 우선순위별 권장 작업

### High (사용자 경험 직접 단절 / 보안)
1. **PlanView 목록 반응성 수정** — `storeToRefs` 사용. 현재 사용자가 만든 일정이 화면에 영영 안 보임. (`PlanView.vue:146`)
2. **핫플 승인 경로 구축** — 자동승인 정책 또는 관리자 승인 UI. 현재 등록 핫플이 목록에 영구 미노출. (`HotPlaceService.java:36`)
3. **채팅 STOMP 클라이언트 FE 구현** — 현재 실시간 채팅 전무. RabbitMQ 단일인자 발행도 정리. (`ChatRoomView.vue:105-119`, `ChatService.java:48`)
4. **FE API baseURL 환경변수화** — `VITE_API_BASE_URL` 실제 사용. 프로덕션 빌드 전체 API 실패 방지. (`http.js:20`)
5. **쿠키 secure(true) + WS Origin 화이트리스트 + TourAPI 키 서버 이전** — 운영 보안 3종. (`CookieUtil.java:43`, `WebSocketBrokerConfig.java:53`, `festival.js:17-19`)
6. **결제/예약 BE 구현 또는 데모 명시** — 현재 완전 더미. (`PaymentView.vue:118`)
7. **동행 신청취소 BE 배선 + isApplied 서버 조회** — 가짜 동작 제거. (`CompanionDetailView.vue:168,184-187`)
8. **핫플 지도 핀 lat/lng 기반 실지도 연동** — hp.x/hp.y 제거. (`CommunityView.vue:110`)

### Med (정합성 / 데이터 품질)
1. 축제 FE를 BE `/api/festivals`로 전환 + 배치 lDongRegnCd/areaCode 체계 통일. (`festival.js`, `FestivalItemProcessor.java:53`)
2. 온보딩 취향설문 저장(엔티티/컬럼/엔드포인트) 신설 + 회원가입 흐름 연결. (`PreferenceSurveyView.vue:119`)
3. 프로필 bio 필드 DTO/엔티티/FE 전송 추가. (`UserUpdateRequestDto.java:3-6`)
4. 동행 상태값(OPEN/CLOSED↔한글) 매핑 정규화 + 목록 currentMembers 추가. (`stores/companion.js:26`, `CompanionPostSummaryResponse.java`)
5. 방장 채팅 버튼 chatRoomId 사용. (`CompanionDetailView.vue:140`)
6. AI 추천 테마 한글 매핑 + draft contentType 보존. (`RecommendService.java:369`, `PlanService.java:322`)
7. BE 비밀번호 검증 추가, `app.frontend.allowed-origins` 정의. (`SignupRequestDto.java:18`, `WebConfig.java:22`)
8. detailCommon2 좌표/주소 보강(별도 API 또는 검색 결과 병합). (`AttractionService.java:171-189`)

### Low (기술부채 / 문서)
1. SETUP.md / docs v0.1 / WORKFLOW_STATUS.md outdated 갱신 또는 archive 표기.
2. 일정 편집/삭제/순서변경 UI, 댓글 좋아요 핸들러 연결, 게시글 카테고리 BE 파라미터 전달.
3. ExploreView 죽은 코드(PIN_POSITIONS) 제거, package.json leaflet 의존성 정리.
4. 프로파일(local/prod) 분리 + 앱 Dockerfile 추가.
5. ROLE_ 접두어 보정(향후 권한 도입 대비), 로그아웃 access token denylist 검토.
6. RabbitMQ Publisher Confirms 활성화, ddl-auto validate 전환(발표 전).

---

### 부록: 검증 신뢰도 메모
- 본 리포트의 high 결함은 대부분 적대적 검증에서 **confirmed**. partial 3건과 refuted 1건은 5-2, 5-3에 별도 분리했다.
- 직접 재확인한 항목: `HotPlaceCreateRequest.java:14`(category 존재), `http.js:20`(baseURL=''), `CookieUtil.java:43`(secure false), FE STOMP grep 0건, `festival.js:17-19`(키 하드코딩), `PlanView.vue:146`(비반응 destructure).

---

## 9. 비평 단계 추가 발견 (1차 리포트 §1~8이 놓친 항목)

> 완성도 비평(completeness critic)에서 별도로 잡은, 본문 상태표/결함표에 누락된 항목. 모두 file:line 근거가 있으나 §5의 적대적 검증을 거치지 않았으므로 적용 전 재확인 권장.

### 9-1. 추가 결함 (근거 확보)
| severity | 영역 | 결함 | 근거 |
|---|---|---|---|
| **high** | 동행 | 신청자 관리 화면의 ageGroup·tripCount·mannerScore·message가 BE 응답 DTO에 **필드 자체가 없음** → store 폴백(`'-'/0/null/''`)으로 신청자 메타·메시지 **영구 빈값/0 고정** | `CompanionApplicationResponse.java:9-16`, `stores/companion.js:131-134` |
| **high** | FE-BE 경로 | BE 매핑은 `/community`·`/companion`·`/auth`·`/users`(=`/api` 없음)인데 FE는 `/api/...` 호출. dev는 vite proxy가 `/api/community`·`/api/companion`만 rewrite로 `/api` 제거해서만 동작 → **prod에선 baseURL 이슈(#18)와 별개로 community/companion 경로가 추가로 깨짐** | `CompanionController.java:17`, `UserController.java:15`, `AuthController.java:17`, `vite.config.*:29-39` |
| **med** | 동행 | CompanionWriteView가 수집한 `tags`를 submit payload에서 누락, `estimatedCost: 0` 하드코딩 | `CompanionWriteView.vue:164-172` |
| **분류오류** | 전처리 | §3·§6에서 "preprocessing/STT는 고아"로 단정했으나 실제로는 REST 엔드포인트 존재(FE 호출만 0건) → "고아"가 아니라 **"BE 구현·FE 미배선"**(§4 'BE는 있으나 FE 미배선'에 추가돼야 함) | `PreprocessingController.java:24,33` |

### 9-2. 영역별 판정 누락 (상태표에 개별 판정 없던 화면)
- CompanionApplicantsView(신청자 관리), AiInputView, ChallengeDetailView, OAuthCallbackView, PostWriteView, SignupView, ExploreView — 핵심 흐름(Signup/PostWrite 포함)인데 §3 상태표에 개별 판정 부재.

### 9-3. 미평가 횡단 관심사
- **테스트**: BE에 단위 테스트 4종 존재(`AuthServiceTest`, `PlanServiceTest`, `RecommendServiceTest`, `TripApplicationTests`) — 나머지 도메인(community/companion/attraction/chat/festival) 테스트 전무. 커버리지 분포 미평가.
- **트랜잭션 경계**: `@Transactional` 50회(12개 서비스). AI 외부호출과 DB 트랜잭션 혼재·readOnly·전파 미검토.
- **N+1**: community 도메인 `@OneToMany`/`@ManyToOne` 31건 — 게시글·댓글·핫플 목록의 `JOIN FETCH`/`@EntityGraph` 적용 여부 미검토.
- **기타**: 로깅 정책, 커서 페이징(`CursorPageResponse`) 일관성, `@Valid` 적용 범위 미평가.

---

## 10. 2차 수정 완료 현황 (2026-06-19)

> §5의 적대적 검증 confirmed 결함 #1~#29와 §9-1 비평 추가건을 대상으로 2차 수정을 수행했다.
> BE `compileJava` + FE `npm run build` 통과 기준. 아래 표는 §5/§9의 **상태를 변경하지 않고**(원문 보존),
> 각 항목의 **종합 판정**을 [해결/부분/백로그]로 정리한다. 판정 근거는 한 줄로 압축한다.
> 표기: **해결** = 수정 코드로 end-to-end 동작 / **부분** = 핵심은 해소되나 일부 필드·범위 공백 / **백로그** = 미해결(정직 표기).

### 10-1. §5-1 적대적 검증 confirmed 결함 #1~#29

| # | 영역 | 판정 | 한 줄 근거 |
|---|---|---|---|
| 1 | 채팅 | **해결** | FE에 무의존 native WebSocket STOMP 클라이언트(`api/stomp.js`) + `stores/chat.js` 신설, ChatRoomView 실시간 송수신 전환(mock 제거) |
| 2 | 채팅 | **해결** | ChatService RabbitTemplate dead code 제거 → `messagingTemplate` /topic 발행으로 정리 |
| 3 | 축제 | **해결** | `festival.js`를 BE `/api/festivals` 경유 래퍼로 축소(TourAPI 직접호출·하드코딩키·mock 제거) |
| 4 | 축제/FE인프라 | **해결** | TourAPI 키 FE 소스/`.env.example`에서 제거(축제는 BE 경유) |
| 5 | 인증 | **해결** | User.preferred_interests/preferred_companion 컬럼 + `PATCH /users/me/preferences`, PreferenceSurveyView 실제 저장 호출 |
| 6 | 인증 | **해결** | User.bio 컬럼 + DTO + FE PATCH 저장 |
| 7 | 커뮤니티 | **해결** | 핫플 register 시 status=APPROVED 자동승인(등록 즉시 노출) |
| 8 | 커뮤니티 | **해결** | 지도 핀 실 lat/lng 사용(hp.x/hp.y 더미 제거), HotplaceDetail 미니맵 Kakao 실좌표 |
| 9 | 일정 | **해결** | PlanView `storeToRefs` 반응성 수정(목록 정상 렌더) |
| 10 | 일정 | **해결** | PlanReportView 더미 공식 제거 → Haversine 직선거리 + 도보/차량 추정(좌표없으면 '추정불가' 정직 표기) |
| 11 | 일정 | **해결** | PlanView 인라인 편집(삭제/장소 위아래 이동/장소 삭제) + '대체 동선 적용'(nearest-neighbor 재정렬 후 replacePlaces 저장) |
| 12 | 동행 | **해결** | 신청취소 실제 DELETE 배선 |
| 13 | 동행 | **해결** | isApplied·myApplicationId BE 응답 추가 + FE 사용(새로고침/중복신청 방지) |
| 14 | 기타화면 | **부분** | PaymentView에 '데모·실제아님' 배지 + 주석 명시(BE 결제 구현은 백로그) |
| 15 | 기타화면 | **부분** | ConfirmationView에 '데모·실제아님' 배지 + 주석 명시(BE 예약 구현은 백로그) |
| 16 | 보안 | **해결** | 쿠키 secure를 `@Value(app.cookie.secure)` 프로퍼티화(운영 COOKIE_SECURE=true) |
| 17 | 보안 | **해결** | WebSocketBrokerConfig `setAllowedOrigins("*")` → allowed-origins 프로퍼티로 제한, SecurityConfig `/ws-sockjs` permit 추가 |
| 18 | FE인프라 | **해결** | `baseURL=import.meta.env.VITE_API_BASE_URL\|\|''` + `.env.production` 신설 |
| 19 | AI추천 | **해결** | THEME_LABELS/COMPANION_LABELS 한글 매핑 + 프롬프트 개선 |
| 20 | 축제 | **해결** | 배치 처리기/서비스 areaCode 컬럼 정합(lDongRegnCd 표준) |
| 21 | 축제 | **해결** | sync cleanup(미수신 행사 ENDED 처리) + 조회 시 end_date>=today 필터 |
| 22 | 동행 | **해결** | status enum → 한글 라벨 정규화 |
| 23 | 동행 | **해결** | CompanionPostSummaryResponse currentMembers 추가 + 매핑 |
| 24 | 동행 | **해결** | 채팅버튼 chatRoomId 사용(하드코딩 `/chat/1` 제거) |
| 25 | 일정 | **해결** | AI draft→plan contentType을 detailCommon2 contentTypeId로 추론(하드코딩 12 제거, 미반환시 12 폴백) |
| 26 | 보안 | **해결** | ROLE_ 접두어(JwtAuthenticationFilter + CustomOAuth2UserService) |
| 27 | 인프라 | **해결** | `application.yaml` app.frontend.allowed-origins 정의(FE_ALLOWED_ORIGINS) |
| 28 | 인증 | **해결** | SignupRequestDto `@Size(min=8)` + LoginRequestDto 검증 추가 |
| 29 | 관광지 | **해결** | 좌표 null 덮어쓰기 방지(기존 스냅샷 좌표 보존) |

### 10-2. §9-1 비평 추가 발견 종합

| 영역 | 판정 | 한 줄 근거 |
|---|---|---|
| 동행 신청자 메타(ageGroup·tripCount·mannerScore·message) | **부분** | CompanionApplication.message 컬럼 + 응답 message 실데이터 + birthDate 파생 ageGroup 해소. tripCount·mannerScore는 통계 모델 부재로 null(**백로그**) |
| FE-BE 경로(`/api` prefix vs BE 매핑) | **해결** | dev는 vite proxy가 `/api/community`·`/api/companion` rewrite, 운영은 `.env.production`/리버스 프록시 미러링 명시(vite.config·`.env.production` 주석) |
| CompanionWrite tags 누락·estimatedCost 하드코딩 | **해결** | tags + estimatedCost를 submit payload에 포함 |
| 전처리(preprocessing/STT) '고아' 분류오류 | **분류정정** | 기능 결함 아님 — REST 엔드포인트 존재(FE 미배선)로 §4 'BE는 있으나 FE 미배선' 분류가 정확 |

### 10-3. 추가 인프라/채팅 보강 (매니페스트 반영)

- **채팅 히스토리 REST**: `ChatHistoryController GET /api/chat/rooms/{id}/messages` 신설(`BE/src/main/java/com/trip/chat/controller/ChatHistoryController.java`).
- **WebSocket 엔드포인트**: `/ws`(raw) · `/ws-sockjs` 병행 등록, SecurityConfig에 `/ws-sockjs` permit 추가.
- **죽은 코드 정리**: ExploreView 죽은 코드 제거, leaflet 의존성 제거, vite.config 죽은 `/api/tour` 프록시 제거.

### 10-4. 백로그(2차 수정 후에도 미해결 — 정직 표기)

- **동행 신청자**: tripCount·mannerScore(통계 모델 부재).
- **일정 표시 공백**: `PlanSummaryResponseDto`의 destination/spots, `DayResponseDto`의 summary 필드 부재(FE 옵셔널 체이닝이라 무해).
- **축제 매칭**: RecommendService의 cross-domain areaCode(legacy ↔ lDongRegnCd) 매칭은 기존 한계.
- **데이터 모델**: `HotPlaceSummaryResponse` description 부재, AlbumDetail/계획↔앨범 매핑.
- **BE 미구현**: 챌린지/뱃지/공지(notice), 결제/예약, 체크리스트, 탐색 지도, 핫플 중복 위치 확인.
- **런타임 전제**: 축제는 인증된 sync 배치 1회 실행 필요(네트워크 + TourAPI 키), 채팅은 RabbitMQ STOMP 릴레이(61613) + MongoDB(히스토리) 구동 필요, dev에서 WebSocket은 클라이언트가 BE(:9090)로 직접 연결.
