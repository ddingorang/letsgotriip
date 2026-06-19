# 관통 여행 — 시스템 설계서 (v0.1 Draft)

> ⚠️ **폐기됨(superseded)**: 이 문서는 강의용 backend/(MyBatis 데모) 전제로 작성된 v0.1입니다.
> 실제 팀 백엔드(`BE/`, com.trip JPA) 기준의 최신 설계는 **`BE/docs/system-design.md` (v0.2+)** 를 보세요.
> 본문의 `com.ssafy.trip`·MyBatis·:8080·`enjoytrip` 등은 모두 **현행 코드와 다릅니다**(실제: `com.trip`·JPA·:9090·`trip_chat`).

> ── 현행 구현 상태 주석 (2026-06-19) ────────────────────────────────────────────
> 본 v0.1 의 MVP 스코프(인증·관광탐색·계획·AI추천)는 **모두 구현 완료**되었고, v0.1이 "MVP 제외"로
> 둔 도메인까지 실제로는 컨트롤러·서비스·DB까지 **구현**되어 가동 중입니다. 현행 구현 도메인(9개):
> **attraction · plan · recommend · community(+핫플) · companion · chat · festival · user · (+preprocessing)**.
> 2차 수정(2026-06-19)으로 해소된 주요 항목:
> - **계획**: PlanView 반응성·인라인 편집, PlanReport 동선 통계(Haversine + 도보/차량 추정·대체동선 적용).
> - **추천/관광지**: 테마 한글 매핑 프롬프트, draft→plan contentType 추론(detailCommon2), 좌표 null 보존.
> - **커뮤니티/핫플**: 핫플 등록 자동승인(즉시 노출), 지도 핀 실 lat/lng.
> - **동행**: 신청취소(DELETE)·isApplied·채팅버튼 chatRoomId·status 한글 라벨·currentMembers·신청자 message/ageGroup.
> - **채팅**: 무의존 STOMP 클라이언트(FE) + 히스토리 REST, RabbitMQ /topic 발행, WebSocket origin 제한.
> - **축제**: FE가 BE `/api/festivals` 경유(TourAPI 직접호출 제거), 배치 areaCode 정합(lDongRegnCd) + ENDED cleanup.
> - **인증/보안**: bio·온보딩 취향설문 영속화, 쿠키 secure 프로퍼티화, ROLE_ 접두어, 비밀번호 검증, CORS allowed-origins.
> 미구현(스코프 밖, 백로그): 챌린지/뱃지/공지, 결제/예약, 체크리스트 BE, 탐색 지도. 종합 판정은 `ANALYSIS.md §10` 참조.
> ─────────────────────────────────────────────────────────────────────────────

> 상태: **초안(폐기) — 역사적 기록**
> 범위: MVP (도메인 1 인증 · 2 관광 탐색 · 3 여행 계획 · 4 AI 추천) + 확장 대비 구조
> 관련 문서: [domain-architecture.md](domain-architecture.md), [design/design-system.md](design/design-system.md)

---

## 1. 목표와 스코프

### 1.1 MVP 목표
가입 → 탐색 → AI 추천 → 계획 저장의 핵심 루프를 **실사용 가능한 품질**로 완성한다.

### 1.2 MVP 포함 (이번 구현)
| 도메인 | 기능 | 대응 화면 |
|---|---|---|
| 1. 사용자·인증 | 회원가입, 로그인/로그아웃, 토큰 재발급, 프로필 조회/수정 | 로그인/가입(신규), 마이페이지(05) |
| 2. 관광 탐색 | 지역·유형별 관광지 검색, 상세, 축제 검색 (TourAPI) | 홈(01), 검색(02), 상세(03) |
| 3. 여행 계획 | 계획 CRUD, 일자/장소 구성, 순서 변경 | 계획 편집(10) |
| 4. AI 추천 | 조건 입력 → AI 일정 초안 생성 → 계획으로 저장 | AI 입력(08), AI 결과(09) |

### 1.3 MVP 제외 (구조만 대비)
결제(04·07 화면은 목업 유지), 커뮤니티, 동행, 게임화, 체크리스트, 리뷰, 관리자.
→ 테이블·패키지 네이밍은 확장을 막지 않게 설계하되 **선구현하지 않는다.**

> 현행(2026-06-19) 정정: **커뮤니티(+핫플)·동행·채팅·축제는 이미 구현되어 가동 중**입니다(v0.1 작성 이후 확장됨).
> 여전히 미구현(백로그)은 결제/예약, 게임화(뱃지/챌린지), 체크리스트 BE, 관리자 백오피스입니다. 결제 화면은 '데모' 배지로 명시됩니다.

---

## 2. 시스템 아키텍처

```
[Vue 3 SPA :5173]
   │  REST /api/v1/**  (Vite dev proxy → :8080)
   ▼
[Spring Boot 3.5 :8080]
   ├─ Spring Security + JWT
   ├─ MyBatis ──────────────► [MySQL 8 : enjoytrip]
   ├─ TourAPI Client ───────► 한국관광공사 TourAPI 4.0
   └─ Spring AI ChatClient ─► SSAFY GMS (OpenAI 호환, gpt-4o-mini)
```

- 프론트: Vue 3 + Vite + Pinia + Axios (기존 frontend/)
- 백엔드: 기존 `backend/`(Spring Boot 3.5.9, Java 17, Maven, MyBatis) 위에 신규 패키지로 구현
- DB: 신규 스키마 `enjoytrip` (기존 강의용 `ssafy_ai`와 분리)
- 지도: Kakao Maps JS SDK (프론트 직접 호출, 키는 FE .env)

### 2.1 기존 코드 처리
- `com.ssafy.ai.*` 강의 데모(UserTools, Weather 등)는 **유지하되 미사용** — 추후 삭제 판단
- Spring AI 설정(`AiConfig`, GMS base-url)은 재사용
- 신규 코드는 `com.ssafy.trip.*` 루트로 작성

---

## 3. 백엔드 패키지 구조

도메인형 패키지 (domain-architecture.md의 경계를 그대로 반영):

```
com.ssafy.trip
├─ global
│  ├─ config          # SecurityConfig, WebConfig(CORS), MyBatisConfig, OpenApiConfig
│  ├─ auth            # JwtTokenProvider, JwtAuthenticationFilter, LoginMember 리졸버
│  ├─ exception       # ErrorCode(enum), BusinessException, GlobalExceptionHandler
│  ├─ response        # ApiResponse<T> 공통 envelope
│  └─ external        # 외부 API 공통 (RestClient 설정, 로깅)
├─ member             # 도메인 1
│  ├─ controller / service / mapper / dto / entity
├─ attraction         # 도메인 2 (TourAPI 프록시 + 캐싱)
│  ├─ controller / service / client(TourApiClient) / dto
├─ plan               # 도메인 3
│  ├─ controller / service / mapper / dto / entity
└─ recommend          # 도메인 4
   ├─ controller / service / ai(PromptFactory, ItineraryParser) / dto / entity
```

레이어 규칙:
- Controller: 인증·검증·DTO 변환만. 비즈니스 로직 금지
- Service: 트랜잭션 경계. 도메인 간 호출은 Service→Service만 허용
- Mapper(MyBatis): XML 매퍼, `resources/mapper/*.xml`
- DTO: 요청/응답 분리 (`XxxRequest`, `XxxResponse`), entity 외부 노출 금지

### 3.1 추가 의존성
```xml
spring-boot-starter-security
spring-boot-starter-validation
jjwt-api / jjwt-impl / jjwt-jackson (0.12.x)
springdoc-openapi-starter-webmvc-ui (2.x)   <!-- Swagger -->
```

---

## 4. DB 설계

스키마 `enjoytrip`, utf8mb4, InnoDB. FK는 명시적으로 걸고 soft delete는 MVP에서 미사용.

```sql
-- 도메인 1: 회원
CREATE TABLE members (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  email         VARCHAR(100) NOT NULL UNIQUE,
  password      VARCHAR(255) NOT NULL,           -- BCrypt
  nickname      VARCHAR(30)  NOT NULL UNIQUE,
  profile_image VARCHAR(255) NULL,
  role          ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  member_id   BIGINT NOT NULL,
  token       VARCHAR(512) NOT NULL,
  expires_at  DATETIME NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_refresh_member (member_id),       -- 1인 1토큰(회전)
  CONSTRAINT fk_refresh_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
);

-- 도메인 2: 관광지 스냅샷 (TourAPI 응답 캐시 — 계획에 담긴 장소의 영속화용)
CREATE TABLE attractions (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  content_id   VARCHAR(20) NOT NULL UNIQUE,       -- TourAPI contentid
  content_type INT NOT NULL,                      -- 12 관광지, 14 문화, 15 축제, 32 숙박, 39 음식점...
  title        VARCHAR(200) NOT NULL,
  addr         VARCHAR(300) NULL,
  area_code    INT NULL,
  sigungu_code INT NULL,
  latitude     DECIMAL(13,10) NULL,
  longitude    DECIMAL(13,10) NULL,
  image_url    VARCHAR(500) NULL,
  tel          VARCHAR(50) NULL,
  overview     TEXT NULL,
  fetched_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_attraction_area (area_code, content_type)
);

-- 도메인 3: 여행 계획 (TripPlan → TripDay → TripPlace)
CREATE TABLE trip_plans (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  member_id   BIGINT NOT NULL,
  title       VARCHAR(100) NOT NULL,
  start_date  DATE NOT NULL,
  end_date    DATE NOT NULL,
  companions  VARCHAR(20) NULL,                   -- SOLO/COUPLE/FAMILY/FRIENDS
  budget      INT NULL,                           -- 1인 예산(원)
  visibility  ENUM('PRIVATE','PUBLIC') NOT NULL DEFAULT 'PRIVATE',
  origin      ENUM('MANUAL','AI') NOT NULL DEFAULT 'MANUAL',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_plan_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
);

CREATE TABLE trip_days (
  id        BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id   BIGINT NOT NULL,
  day_no    INT NOT NULL,                          -- 1일차=1
  memo      VARCHAR(300) NULL,
  UNIQUE KEY uk_day (plan_id, day_no),
  CONSTRAINT fk_day_plan FOREIGN KEY (plan_id) REFERENCES trip_plans(id) ON DELETE CASCADE
);

CREATE TABLE trip_places (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  day_id        BIGINT NOT NULL,
  attraction_id BIGINT NOT NULL,
  seq           INT NOT NULL,                      -- 일자 내 방문 순서
  visit_time    TIME NULL,
  memo          VARCHAR(300) NULL,
  UNIQUE KEY uk_place_seq (day_id, seq),
  CONSTRAINT fk_place_day FOREIGN KEY (day_id) REFERENCES trip_days(id) ON DELETE CASCADE,
  CONSTRAINT fk_place_attraction FOREIGN KEY (attraction_id) REFERENCES attractions(id)
);

-- 도메인 4: AI 추천 이력
CREATE TABLE recommendations (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  member_id    BIGINT NOT NULL,
  request_json JSON NOT NULL,                      -- 조건(지역/기간/동행/예산/테마)
  result_json  JSON NOT NULL,                      -- AI 일정 초안(구조화 응답)
  model        VARCHAR(50) NOT NULL,
  status       ENUM('SUCCESS','FAILED') NOT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_reco_member (member_id, created_at DESC),
  CONSTRAINT fk_reco_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
);
```

설계 결정:
- **attractions = 스냅샷 캐시.** 검색은 TourAPI 실시간 호출(저장 안 함). 계획에 장소를 담는 순간 upsert로 영속화 → 계획 데이터가 외부 API 변동·장애와 무관해짐
- **trip_places.seq 순서 변경**은 일자 단위 전체 재배치(배열 PUT)로 처리 — 드래그 정렬 UI와 매칭, 동시성 단순화
- recommendations는 JSON 컬럼 — 초안은 구조가 자주 바뀌므로 정규화하지 않음. "계획에 담기" 시점에 trip_plans로 정규화 변환

---

## 5. API 설계

### 5.1 공통 규약
- Base: `/api/v1`, JSON only
- 응답 envelope:
```json
{ "success": true,  "data": { ... } }
{ "success": false, "error": { "code": "PLAN_NOT_FOUND", "message": "..." } }
```
- 에러코드: `ErrorCode` enum 단일 소스 (HTTP status 매핑 포함). 검증 실패 400 `INVALID_INPUT`, 인증 401 `UNAUTHORIZED`, 권한 403 `FORBIDDEN`, 없음 404 `*_NOT_FOUND`, 중복 409 `DUPLICATE_*`
- 페이지네이션: `?page=1&size=20` → `data: { items, page, size, totalCount }`

### 5.2 엔드포인트

**인증/회원 (member)**
| Method | Path | Auth | 설명 |
|---|---|---|---|
| POST | /auth/signup | - | 가입 (email, password, nickname) |
| POST | /auth/login | - | 로그인 → access + refresh 발급 |
| POST | /auth/refresh | refresh | access 재발급 (refresh 회전) |
| POST | /auth/logout | ✔ | refresh 무효화 |
| GET | /members/me | ✔ | 내 프로필 |
| PATCH | /members/me | ✔ | 닉네임/프로필 수정 |

**관광 탐색 (attraction)** — TourAPI 프록시
| Method | Path | Auth | 설명 |
|---|---|---|---|
| GET | /attractions | - | 검색: `areaCode, sigunguCode, contentTypeId, keyword, page` |
| GET | /attractions/{contentId} | - | 상세 (detailCommon + intro + 이미지) |
| GET | /attractions/festivals | - | 축제: `areaCode, eventStartDate` |
| GET | /attractions/areas | - | 지역코드 목록 (서버 캐시, 1일 TTL) |

**여행 계획 (plan)**
| Method | Path | Auth | 설명 |
|---|---|---|---|
| POST | /plans | ✔ | 생성 (title, 기간 → trip_days 자동 생성) |
| GET | /plans | ✔ | 내 계획 목록 |
| GET | /plans/{id} | ✔(소유자) | 상세 (days + places + attraction 포함 트리) |
| PATCH | /plans/{id} | ✔(소유자) | 제목/기간/공개 수정 |
| DELETE | /plans/{id} | ✔(소유자) | 삭제 |
| POST | /plans/{id}/days/{dayNo}/places | ✔ | 장소 추가 (contentId → attraction upsert) |
| PUT | /plans/{id}/days/{dayNo}/places | ✔ | 일자 내 장소 순서/구성 전체 교체 |
| DELETE | /plans/{id}/days/{dayNo}/places/{placeId} | ✔ | 장소 제거 |

**AI 추천 (recommend)**
| Method | Path | Auth | 설명 |
|---|---|---|---|
| POST | /recommendations | ✔ | 조건 → AI 일정 초안 생성 (동기, 타임아웃 60s) |
| GET | /recommendations | ✔ | 내 추천 이력 |
| GET | /recommendations/{id} | ✔ | 추천 상세 |
| POST | /recommendations/{id}/save-plan | ✔ | 초안 → trip_plans 변환 저장 |

---

## 6. 인증 설계 (JWT)

- **Access Token**: 30분, HS256, claims: `sub(memberId), role`. `Authorization: Bearer` 헤더
- **Refresh Token**: 14일, DB 저장(1인 1행), 재발급 시 회전(rotate) — 탈취 대응
- 저장 위치: FE는 access를 메모리(Pinia), refresh는 `httpOnly` 쿠키 (XSS 방어)
  - dev에서 SameSite=Lax + Vite proxy로 same-origin화 → CORS·쿠키 문제 회피
- BCrypt cost 10. 비밀번호 정책: 8자+, 영문/숫자 조합
- Spring Security 필터체인: `/api/v1/auth/**`, `/api/v1/attractions/**` permitAll, 그 외 인증
- 컨트롤러에서 `@LoginMember Long memberId` ArgumentResolver로 주입

---

## 7. AI 추천 설계 (RAG)

```
조건 입력 → TourAPI로 후보 장소 수집(지역×테마별 상위 N) 
         → 후보 목록을 컨텍스트로 프롬프트 구성
         → ChatClient.entity(ItineraryDraft.class) 구조화 응답
         → 검증(후보 외 장소 환각 제거, 일자 수 일치) → 저장/응답
```

- **환각 억제**: 프롬프트에 "반드시 제공된 후보 contentId만 사용" 제약 + 응답 파싱 후 후보에 없는 contentId 필터링
- **구조화 응답**: Spring AI `BeanOutputConverter` (`ItineraryDraft { days[ { dayNo, places[ { contentId, time, reason } ], summary } ], totalSummary }`)
- 후보 수집: contentTypeId 12/14/39 (관광지/문화/음식점) × 지역, 각 타입 상위 15개 → 컨텍스트 ~45개 (토큰 제한 내)
- 실패 처리: 타임아웃/파싱 실패 시 1회 재시도 → 실패 시 `AI_GENERATION_FAILED` (FE는 재시도 버튼)
- 모델: GMS `gpt-4o-mini` (기본), 키는 `application-local.properties` (gitignore)

---

## 8. 프론트엔드 연동

- `src/api/http.js`: Axios 인스턴스. 요청 인터셉터(access 헤더), 응답 인터셉터(401 → refresh → 재요청, envelope unwrap)
- Pinia 스토어: `auth.js`(member, accessToken, login/logout/refresh), `attraction.js`(검색 상태), `plan.js`, `recommend.js`
- Vite proxy: `/api` → `http://localhost:8080` (개발)
- 라우터 가드: `meta.requiresAuth` (plan/ai/mypage) → 미로그인 시 `/login` 리다이렉트
- 신규 화면: LoginView, SignupView (디자인 시스템 토큰으로 작성 — 디자인에 없으므로 08 입력 폼 스타일 준용)
- 화면별 데이터 교체: 02 검색(/attractions), 03 상세(/attractions/{id}), 08→09(POST /recommendations), 10(plan CRUD), 05(members/me)
- Kakao Maps: `useKakaoMap.js` 컴포저블 재사용, 10 계획 편집의 경로 지도에 적용

---

## 9. 비기능 요구사항

- **예외**: GlobalExceptionHandler 1곳에서 ErrorCode→응답 변환. 스택트레이스는 서버 로그만
- **검증**: `@Valid` + Bean Validation. 날짜 역전, seq 음수 등 도메인 검증은 Service
- **시크릿**: DB/GMS/TourAPI 키는 `application-local.properties` (gitignore) + `.env.example` 갱신
- **CORS**: dev는 Vite proxy로 회피, 운영 대비 WebConfig에 allowedOrigins 설정 지점만 마련
- **로깅**: 외부 API 호출(요청 요약, 소요시간, 실패 응답), AI 호출은 기존 Langfuse 연동 유지
- **Swagger**: springdoc — `/swagger-ui.html`, API 계약의 단일 소스
- **테스트**: Service 단위 테스트(Mockito) + 인증/계획 핵심 플로우 `@SpringBootTest` 통합 테스트. 외부 API는 `MockRestServiceServer`
- **Git**: 현재 git 미초기화 → **구현 시작 전 `git init` + .gitignore 정비 + 초기 커밋** (필수)

---

## 10. 마일스톤

| 단계 | 내용 | 완료 기준 |
|---|---|---|
| **M0** | git init, 스키마 생성, 패키지 스캐폴드, 공통(응답/예외/Security 뼈대), Swagger | 빌드 + /actuator/health 200 |
| **M1** | 인증: signup/login/refresh/me + FE 로그인·가입 화면 + 인터셉터 | E2E: 가입→로그인→마이페이지 표시 |
| **M2** | 탐색: TourAPI 프록시 + FE 검색/상세 실데이터 | 검색→상세→지도 표시 |
| **M3** | 계획: plan CRUD + 장소 담기/정렬 + FE 계획 편집 연동 | 상세→계획에 담기→편집→저장 |
| **M4** | AI 추천: RAG 파이프라인 + FE 08/09 연동 + save-plan | 조건 입력→초안→계획 저장 E2E |
| **M5** | 마감: 통합 테스트, 에러 UX(토스트/빈 상태), README | 전 플로우 시연 가능 |

---

## 11. 리뷰 요청 포인트 (Codex에게)

1. attractions 스냅샷 캐시 전략 vs 전체 동기화(배치 적재) — SSAFY 규모에서 타당한가
2. refresh 쿠키(httpOnly) vs 바디 반환 — 학습 프로젝트 복잡도 대비 적정선
3. trip_places 순서 변경의 PUT 전체교체 방식 — 동시성/성능 우려
4. AI 추천 동기 처리(60s) vs 비동기(폴링) — UX·구현 비용 트레이드오프
5. MyBatis 환경에서 도메인 패키지 구조·트랜잭션 경계의 함정
6. 누락된 횡단 관심사(보안 헤더, rate limit, 입력 정규화 등) 중 MVP에 필수인 것
