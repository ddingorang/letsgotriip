# trip-fe

`gptgeminiclaude/FE`의 UI를 **기준(base)**으로 삼고, `springaitrip/frontend`의 기능을 흡수해 만든 통합 프론트엔드입니다. 데이터는 실제 Spring 백엔드(`springaitrip/BE`, 포트 9090)에 연동됩니다(관광지·축제 등 외부 데이터도 모두 BE 경유 — FE는 TourAPI 를 직접 호출하지 않습니다).

## 스택

Vue 3 · Vite · Pinia · vue-router 5 · axios · `@` → `src` alias

## 실행

```bash
# 1) 백엔드 (별도 터미널) — springaitrip/BE 를 9090 포트로 구동
#    (SETUP 은 ../springaitrip/SETUP.md 참고)

# 2) 프론트엔드
npm install
cp .env.example .env   # 아래 "환경변수" 참고 (VITE_API_BASE_URL · VITE_KAKAO_MAP_KEY)
npm run dev            # http://localhost:5173
```

대부분의 기능(로그인·탐색·AI추천·계획·커뮤니티·동행·채팅)은 실데이터 기반이므로 **BE 구동이 필요**합니다. 채팅 실시간 송수신은 BE 의 RabbitMQ(STOMP)·MongoDB 까지 떠 있어야 동작합니다(`../SETUP.md` 의 docker compose 참고).

### 환경변수 (`.env`)

| 변수 | 용도 |
|---|---|
| `VITE_API_BASE_URL` | API 기본 URL. dev 에서는 **비워 둠**(빈 값이면 `baseURL=''` 로 Vite dev 프록시가 `/api`·`/auth`·`/users` 등을 :9090 으로 라우팅). 프로덕션은 `.env.production` 에서 BE/리버스 프록시 origin 지정 |
| `VITE_KAKAO_MAP_KEY` | 카카오 지도 JS 키 (핫플 등록/지도·미니맵 표시) |

> TourAPI 키(과거 `VITE_KOREA_TOURISM_API_KEY`)는 **더 이상 FE 에 두지 않습니다.** 축제 등 TourAPI 데이터는 BE `/api/festivals` 를 경유하며, 키는 BE `.env`(`TOUR_API_KEY`)에만 존재합니다.

### Dev 프록시 (`vite.config.js`)

| 경로 | 대상 |
|---|---|
| `/api/community`, `/api/companion` | `http://localhost:9090` (Spring BE, `/api` prefix 제거 — BE는 `/community`·`/companion` 로 서빙) |
| `/api`, `/auth`, `/users`, `/uploads`, `/oauth2`, `/login/oauth2` | `http://localhost:9090` (Spring BE) |

> `/api/community`·`/api/companion` 규칙은 반드시 `/api` 보다 먼저 와야 합니다(더 구체적인 규칙 우선). SPA 라우트(`/community`·`/companion`)와 경로가 겹쳐 프록시가 네비게이션을 가로채는 것을 막기 위해 `/api/*` 로 감싸 호출합니다.
>
> WebSocket(STOMP)은 dev 에서 FE 클라이언트가 BE(:9090)로 **직접** 연결합니다(프록시 비경유). 죽은 `/api/tour` 프록시(과거 TourAPI 직접 호출용)는 제거되었습니다.

## 아키텍처

- **앱 셸**: `App.vue`가 `<RouterView>` + 전역 `BottomNav`를 렌더. `route.meta.tabBar === false`인 라우트(로그인·결제·AI결과 등 풀스크린 플로우)에서는 BottomNav 숨김. 각 뷰는 자체 BottomNav를 갖지 않습니다.
- **인증**: `stores/auth.js` — JWT accessToken(메모리) + refresh 쿠키. `api/http.js`가 401 발생 시 single-flight refresh → 재시도. 앱 시작 시 `bootstrap()`로 silent refresh. `meta.requiresAuth` 라우트는 전역 가드가 `/login?redirect=...`로 보냅니다. 카카오/구글 OAuth2 지원.
- **API 레이어**: `api/index.js`의 도메인 모듈이 실제 BE 엔드포인트에 정렬됨.
  - 실연동: `auth`, `users/me`, `/api/attractions`, `/api/festivals`, `/api/plans`, `/api/recommendations`, `/api/community`(+핫플), `/api/companion`, `/api/chat`
  - 채팅 실시간: `api/stomp.js`(무의존 native WebSocket STOMP 클라이언트) + `stores/chat.js`
- **축제**: `api/festival.js`는 BE `/api/festivals` 를 호출하는 얇은 래퍼입니다(TourAPI 직접 호출·하드코딩 키·mock 폴백 제거). 데이터는 BE 의 sync 배치 1회 실행 후 채워집니다.

## 주요 라우트

| 경로 | 화면 | tabBar | 인증 |
|---|---|---|---|
| `/home` | 홈 | ✓ | |
| `/explore` | 탐색(관광/축제 실데이터) | ✓ | |
| `/ai`, `/ai/result` | AI 추천 입력 → 결과 → 계획저장 | ✓ / ✗ | ✓ |
| `/plan` | 여행 계획 | ✓ | ✓ |
| `/community` | 커뮤니티 | ✓ | |
| `/mypage` | 마이페이지 | ✓ | ✓ |
| `/badges`, `/checklist` | 뱃지 / 체크리스트 | ✓ | ✓ |
| `/payment`, `/confirmation` | 결제 / 예약확정 (**데모** — BE 미연동, 화면에 '실제아님' 배지) | ✗ | |
| `/login`, `/signup`, `/oauth/callback` | 인증 | ✗ | |

핫플(`/hotplace/*`), 동행(`/companion/*`), 채팅(`/chat/*`) 화면도 포함하며, 모두 BE(`/api/community`·`/api/companion`·`/api/chat`)에 실연동됩니다.

## 출처

- UI 기준: `gptgeminiclaude/FE`
- 기능/백엔드 연동: `springaitrip/frontend` + `springaitrip/BE`
