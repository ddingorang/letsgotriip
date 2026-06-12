# 관통 여행 — 시스템 설계서 (v0.3 — 구현 기준 확정본)

> 기준 코드: `gptgeminiclaude/BE` (com.trip, Spring Boot 3.5.14 + Gradle + JPA, Java 21)
> 리뷰 이력: v0.1 Codex xhigh 13건 반영 `[CR-n]` → v0.2 Codex xhigh 재리뷰 반영 `[CR2-n]` (본 문서)
> 관련 문서: 도메인 정의 `pjt/docs/domain-architecture.md`, 디자인 `pjt/docs/design/design-system.md`

---

## 0. 즉시 조치 (M0에서 처리)

| # | 항목 | 심각도 |
|---|---|---|
| 0-1 | **JWT secret 평문 노출** [CR2 정정]: Google/TourAPI 키는 이미 `${ENV}` 참조 — 실제 노출은 `jwt.secret`(public 저장소에 커밋됨). **① JWT secret 재생성 ② `${JWT_SECRET}` env 참조로 교체 ③ 기존 토큰 전체 무효화 공지**. MySQL `password`/RabbitMQ `guest`는 로컬 dev 기본값이라 위험도 낮음 — env 참조로만 정리 | critical |
| 0-2 | `SecurityConfig`: 공개 조회 경로 permitAll 추가 — `GET /api/festivals`, `GET /api/attractions/**`, springdoc. **공개 GET에서 Authorization 헤더가 잘못 들어와도 무시하고 통과** (필터에서 예외 던지지 않음 — 현재 validateToken이 boolean 반환이라 이미 충족, 유지) [CR2] | major |
| 0-3 | `users.email` `@Column(unique=true)` 추가 + signup race는 DB 제약 위반 catch로 `USER_ALREADY_EXISTS` 변환 | major |
| 0-4 | `POST /api/festivals/sync` ADMIN 권한 제한 | minor |
| 0-5 | OAuth2SuccessHandler가 **access token을 쿼리스트링으로 `test-chat.html`에 리다이렉트** 중 — 토큰 URL 노출. **변경: 핸들러는 refresh 쿠키만 심고 FE `/oauth/callback`으로 리다이렉트 → FE가 `/auth/refresh` 호출로 access 획득** (기존 refresh 플로우 재사용, 토큰이 URL에 남지 않음) [CR2] | major |

---

## 1. 현재 BE 자산 (As-Is)

### 1.1 이미 구현된 것 — 재사용
| 영역 | 내용 | 상태 |
|---|---|---|
| **인증** | 이메일/비번 가입·로그인, Google OAuth2, JWT access(1h) + opaque refresh(7d), refresh는 SHA-256 해시로 Redis 저장(`rt:session{familyId}` 키), **회전 + 재사용 탐지 + 5분 overlap window**, httpOnly 쿠키 | 견고. 로그아웃만 없음 |
| **회원** | GET/PATCH/DELETE `/users/me` (소프트 탈퇴 status=false) | 동작 |
| **축제** | `festivals`(contentId PK), TourAPI 배치 적재(06/18시 KST), GET `/api/festivals` | 동작 |
| **채팅** | ChatRoom/Membership(JPA) + ChatMessage(Mongo), STOMP over RabbitMQ | 기반만 (MVP 제외) |
| **에러** | `ResponseCode` enum → `XxxHandler extends GeneralException` → `GlobalExceptionHandler` → `ErrorResponse{code,message}` | 컨벤션 확정 |
| **인프라** | docker-compose: MySQL 8(:3307), Mongo 7, Redis 7.2, RabbitMQ 3.12(STOMP) / 서버 :9090 | 동작 |

### 1.2 미병합 작업
- `origin/feat/datacollection`: preprocessing 도메인(Whisper STT, `UserAnalysisData`) → **dev에 머지하고 이어서 개발**

### 1.3 컨벤션 (신규 코드도 동일)
- 패키지 `com.trip.{domain}.controller|service|repository|entity|dto`, JPA + `BaseEntity`, DTO는 record + `-RequestDto/-ResponseDto`
- 응답: envelope 없이 `ResponseEntity<Dto>`. 에러만 `ErrorResponse{code,message}`
- Redis 키: 신규는 `cache:attr:*`, `lock:reco:*` 네임스페이스. 기존 RT 키(`rt:session{familyId}`)는 호환 유지 [CR2 — 문서 전제 보정]

---

## 2. 목표 아키텍처 (To-Be)

```
[Vue 3 SPA :5173] ── Vite proxy /api,/auth,/users,/oauth2,/login,/ws → :9090
        ▼
[Spring Boot :9090  com.trip]
 ├─ user / global(security)   ✔ 기존 (+logout, OAuth 콜백 수정)
 ├─ festival (+batch)         ✔ 기존
 ├─ chat                      ✔ 기존 — `chat.enabled` 토글 도입 [CR2-6]
 ├─ preprocessing (STT)       ◐ feat/datacollection 머지
 ├─ attraction                ★ 신규 — TourAPI 프록시 + Redis 캐시 + 스냅샷
 ├─ plan                      ★ 신규
 └─ recommend                 ★ 신규 — Spring AI (RAG)
```

- **chat 조건부 기동** [CR2-6]: `WebSocketBrokerConfig`/`RabbitMQConfig`/Mongo 관련 빈에 `@ConditionalOnProperty(name="chat.enabled", havingValue="true", matchIfMissing=true)`. 기본 켜짐(현행 유지)이되, MVP 시연·CI에서는 `chat.enabled=false`로 Mongo/RabbitMQ 없이 기동 가능
- MVP 스코프: 인증(기존) + 탐색 + 계획 + AI 추천

### 2.1 추가 의존성
```groovy
implementation platform("org.springframework.ai:spring-ai-bom:1.1.2")
implementation "org.springframework.ai:spring-ai-starter-model-openai"   // GMS — M0에서 smoke test 필수 [CR2]
implementation "org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.x"
```

### 2.2 URL 정책
기존 경로(`/auth/**`, `/users/**`, `/api/festivals`) 유지, 신규는 `/api/attractions|plans|recommendations` (v0.2 결정 유지 — Codex 타당 판정).

---

## 3. DB 설계 (신규 테이블)

원칙 [CR2]: **DB ENUM 대신 `VARCHAR + @Enumerated(EnumType.STRING)`** (ddl-auto update와 enum 변경 궁합 문제 회피). FK·cascade는 JPA 연관관계로 명시: `TripPlan→TripDay→TripPlace`는 `cascade=ALL, orphanRemoval=true`, `TripPlace→Attraction`은 단방향 `@ManyToOne(LAZY)`. **스키마 스냅샷 `docs/schema.sql`을 항상 최신 유지, 발표 전 `ddl-auto: validate` 전환 체크리스트** [CR2-3].

```sql
CREATE TABLE attractions (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  content_id    VARCHAR(20) NOT NULL,
  content_type  INT NOT NULL,
  title         VARCHAR(200) NOT NULL,
  addr          VARCHAR(300) NULL,
  area_code     VARCHAR(10) NULL,
  sigungu_code  VARCHAR(10) NULL,
  latitude      DOUBLE NULL,
  longitude     DOUBLE NULL,
  image_url     VARCHAR(500) NULL,
  tel           VARCHAR(50) NULL,
  overview      TEXT NULL,
  fetched_at    DATETIME NOT NULL,
  UNIQUE KEY uk_attr_content (content_id, content_type),   -- [CR2] contentId 단독 대신 복합
  KEY idx_attractions_area (area_code, content_type)
);

CREATE TABLE trip_plans (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  title       VARCHAR(100) NOT NULL,
  start_date  DATE NOT NULL,
  end_date    DATE NOT NULL,
  companions  VARCHAR(20) NULL,        -- SOLO/COUPLE/FAMILY/FRIENDS (EnumType.STRING)
  budget      INT NULL,
  origin      VARCHAR(10) NOT NULL DEFAULT 'MANUAL',
  version     BIGINT NOT NULL DEFAULT 0,   -- @Version [CR-5]
  created_at  DATETIME, updated_at DATETIME,
  KEY idx_plans_user (user_id, updated_at DESC),
  CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE trip_days (
  id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  day_no  INT NOT NULL,
  memo    VARCHAR(300) NULL,
  UNIQUE KEY uk_day (plan_id, day_no),
  CONSTRAINT fk_day_plan FOREIGN KEY (plan_id) REFERENCES trip_plans(id) ON DELETE CASCADE
);

CREATE TABLE trip_places (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  day_id        BIGINT NOT NULL,
  attraction_id BIGINT NOT NULL,
  seq           INT NOT NULL,
  visit_time    TIME NULL,
  memo          VARCHAR(300) NULL,
  UNIQUE KEY uk_place_seq (day_id, seq),
  UNIQUE KEY uk_place_attr (day_id, attraction_id),  -- [CR2] 같은 일자 내 같은 장소 중복 금지(409 DUPLICATE_PLACE)
  CONSTRAINT fk_place_day FOREIGN KEY (day_id) REFERENCES trip_days(id) ON DELETE CASCADE,
  CONSTRAINT fk_place_attr FOREIGN KEY (attraction_id) REFERENCES attractions(id)
);

CREATE TABLE recommendations (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  request_json  JSON NOT NULL,
  request_hash  VARCHAR(64) NOT NULL,        -- [CR2-4] 조건 정규화 SHA-256 (멱등 판단)
  result_json   JSON NULL,
  model         VARCHAR(50) NOT NULL,
  status        VARCHAR(10) NOT NULL,        -- SUCCESS/PARTIAL/FAILED [CR2]
  saved_plan_id BIGINT NULL,                 -- [CR2] save-plan 멱등 — 1추천 1계획
  error_code    VARCHAR(30) NULL,
  error_message VARCHAR(300) NULL,
  latency_ms    INT NULL,
  created_at    DATETIME,
  KEY idx_reco_user (user_id, created_at DESC),
  CONSTRAINT fk_reco_user FOREIGN KEY (user_id) REFERENCES users(id)
);
```

도메인 규칙:
- **기간 수정**: 축소로 장소 있는 일자가 제거되면 `409 PLAN_PERIOD_CONFLICT`. 연장은 빈 trip_days 추가 [CR-4]
- **버전 규칙** [CR2-2(critical)]: **plan 하위의 모든 변경(place 추가/삭제/PUT, day memo, plan 메타 수정)은 `trip_plans.version`을 증가**시킨다 — 구현은 해당 Service 메서드에서 plan 엔티티를 항상 조회·터치(`plan.touch()` + `@Version` 자동 증가). 클라이언트는 PUT/PATCH에 `expectedVersion` 동봉, 불일치 시 `409 PLAN_VERSION_CONFLICT`
- **스냅샷 upsert**: 장소 담기 시 (contentId, contentType)로 upsert

---

## 4. API 설계 (신규)

공통: plan/recommend 전체 소유자 검증(`403 _FORBIDDEN`) [CR-1][CR-2]. 페이지 `size ≤ 50` [CR-12].

**attraction (공개, 인증 불요)**
| Method | Path | 설명 |
|---|---|---|
| GET | /api/attractions | TourAPI 프록시. `areaCode, sigunguCode, contentTypeId, keyword(≥2자), page, size` — keyword 2자 미만 400 [CR2-2] |
| GET | /api/attractions/{contentId} | detailCommon2. Redis 캐시 6h |
| GET | /api/attractions/areas | 지역코드. 캐시 24h |

캐시 정책 [CR2]:
- 키 정규화: keyword `trim → lower → 공백압축`, 파라미터 정렬 후 해시 — `cache:attr:search:{hash}` 10분
- TourAPI 실패 시 **stale 캐시가 있으면 stale 반환 + 로그**, 없으면 `502 EXTERNAL_API_ERROR`
- 간이 rate limit: IP당 분당 60회 (Redis INCR+EXPIRE, 초과 `429 TOO_MANY_REQUESTS`)

**plan (인증+소유자)** — v0.2와 동일 8개 엔드포인트. 추가 규칙:
- 장소 추가 시 같은 일자 중복 장소 `409 DUPLICATE_PLACE`
- 모든 mutation이 plan version 증가 (3절 버전 규칙)

**recommend (인증+소유자)**
| Method | Path | 설명 |
|---|---|---|
| POST | /api/recommendations | 동기 30s. **락**: `SET lock:reco:{userId} {requestId} NX EX 60` → 성공/실패/예외 모두 finally에서 **값 비교 후 삭제(Lua compare-and-delete)** [CR2-4]. 락 점유 중 `409 RECO_IN_PROGRESS`. 직전 5분 내 동일 `request_hash` SUCCESS 존재 시 그 결과 재반환(LLM 호출 생략) |
| GET | /api/recommendations | 내 이력 |
| GET | /api/recommendations/{id} | 상세 |
| POST | /api/recommendations/{id}/save-plan | **멱등** [CR2-1(critical)]: `saved_plan_id` 이미 있으면 기존 plan을 200으로 재반환(중복 생성 없음). 저장 성공 시 saved_plan_id 기록 (동일 트랜잭션) |

**auth 보강**
| Method | Path | 설명 |
|---|---|---|
| POST | /auth/logout | Redis 세션(familyId) 삭제 + 쿠키 만료. **access token은 만료까지 유효함을 명시적 정책으로 채택**(1h, denylist 미도입 — 구현 비용 대비 수용) [CR2-1]. withdraw에도 동일한 세션 삭제 추가 |

OAuth 콜백 (0-5): SuccessHandler → refresh 쿠키 설정 → `{FE_URL}/oauth/callback` 리다이렉트(토큰 없이) → FE가 `/auth/refresh`로 access 획득.

---

## 5. AI 추천 설계

파이프라인은 v0.2와 동일 (TourAPI 후보 + festivals 테이블 RAG → ChatClient 구조화 응답 → 검증).

검증·결과 정책 [CR-10][CR2]:
1. 후보 외 contentId 제거 → 2. dayNo 범위 밖 제거 → 3. 중복 contentId 1회만
4. 보충은 **후보 풀 안에서 1회만** 시도. 그래도 일자당 2곳 미만이면 **`status=PARTIAL`로 저장·반환** (FE는 "일부 일정만 생성됨" 표시 + 해당 일자에 빈 슬롯 UI). 전체가 비면 `FAILED + RECO_EMPTY_RESULT`
5. visit_time "HH:mm" 강제(실패 시 null)
- M0에서 **GMS smoke test** (모델 1회 호출 통합 테스트) — base-url 호환 검증 [CR2]

---

## 6. 프론트엔드 연동

v0.2 동일 + 변경점:
- OAuth: FE `/oauth/callback` 라우트 신설 — 도착 시 `/auth/refresh` 호출 → access 저장 → 홈 이동
- 401 인터셉터 single-flight [CR-6], `withCredentials: true`

---

## 7. 비기능

- **시크릿**: 0-1. `.env`(gitignore) + `.env.example`. JWT secret 회전
- **캐시/락 키**: `cache:attr:*`, `lock:reco:*` (기존 `rt:session*`과 충돌 없음)
- **외부 API**: RestClient connect 3s / read 10s. 추천용 LLM 호출만 read 30s
- **스키마**: `docs/schema.sql` 스냅샷 유지, 발표 전 `validate` 전환
- **테스트 필수 케이스** [CR2]: ① refresh overlap(이전 RT 5분 내 허용) ② logout 후 refresh 실패 ③ withdraw 후 refresh 실패 ④ plan 소유자 위반 403 ⑤ 기간 축소 409 ⑥ save-plan 멱등 ⑦ recommend 검증 규칙
- **Swagger**: springdoc 전 엔드포인트

---

## 8. 마일스톤 (dev 브랜치, feat/* → dev)

| 단계 | 내용 | 완료 기준 |
|---|---|---|
| **M0 정비** | 0-1~0-5, feat/datacollection 머지, 의존성 추가, chat.enabled 토글, .env 체계, Swagger, GMS smoke test, schema.sql | 빌드 + 컨텍스트 기동(chat off 포함) + Swagger + GMS 응답 확인 |
| **M1 탐색** | attraction(프록시+캐시+rate limit) + FE 검색/상세/홈 + festival 연동 | 비로그인 검색→상세 E2E |
| **M2 인증 연동** | logout/withdraw 세션 정리 + FE Login/Signup + 인터셉터 + OAuth 콜백 | 가입→로그인→새로고침 유지→로그아웃 E2E (+테스트 ①②③) |
| **M3 계획** | plan 도메인(버전 규칙 포함) + FE 계획 편집 연동 | 담기→편집→순서변경→저장 E2E (+테스트 ④⑤) |
| **M4 AI 추천** | recommend(락+멱등+PARTIAL) + FE 08/09 + save-plan | 조건→초안→계획 저장 E2E (+테스트 ⑥⑦) |
| **M5 마감** | 통합 테스트, 에러 UX, README, ddl validate 전환, 시연 시나리오 | 전 플로우 시연 |

---

## 9. 리뷰 이력

- **v0.1 → Codex xhigh** (`pjt/.omc/artifacts/ask/codex-...06-22-33`): 13건 — 소유자 검증, 실패 이력, 기간 규칙, 낙관적 잠금, single-flight, 30s, TTL 캐시, visibility 제거 등 → v0.2 반영
- **v0.2 → Codex xhigh** (`pjt/.omc/artifacts/ask/codex-...06-42-19`): 시크릿 범위 정정(JWT만 평문), save-plan 멱등(critical), 락 compare-and-delete(critical), place 변경의 version 증가 규칙(critical), chat 조건부 기동, OAuth 콜백 토큰 노출, ENUM→VARCHAR, 캐시 정규화, PARTIAL 정책, 테스트 3케이스 → 본 v0.3 반영. **설계 확정.**
