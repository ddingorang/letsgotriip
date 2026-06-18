# trip-fe

`gptgeminiclaude/FE`의 UI를 **기준(base)**으로 삼고, `springaitrip/frontend`의 기능을 흡수해 만든 통합 프론트엔드입니다. 데이터는 실제 Spring 백엔드(`springaitrip/BE`, 포트 9090)와 한국관광공사 TourAPI에 연동됩니다.

## 스택

Vue 3 · Vite · Pinia · vue-router 5 · axios · `@` → `src` alias

## 실행

```bash
# 1) 백엔드 (별도 터미널) — springaitrip/BE 를 9090 포트로 구동
#    (SETUP 은 ../springaitrip/SETUP.md 참고)

# 2) 프론트엔드
npm install
cp .env.example .env   # VITE_KOREA_TOURISM_API_KEY 등 확인
npm run dev            # http://localhost:5173
```

백엔드가 꺼져 있어도 화면은 **mock/seed 폴백**으로 동작합니다(탐색·핫플·커뮤니티·동행 등). 로그인/AI추천/계획 등 실데이터 기능은 BE 구동이 필요합니다.

### Dev 프록시 (`vite.config.js`)

| 경로 | 대상 |
|---|---|
| `/api/tour/*` | `https://apis.data.go.kr` (TourAPI, rewrite로 prefix 제거) |
| `/api`, `/auth`, `/users`, `/oauth2`, `/login/oauth2` | `http://localhost:9090` (Spring BE) |

> `/api/tour` 규칙은 반드시 `/api` 보다 먼저 와야 합니다(더 구체적인 규칙 우선).

## 아키텍처

- **앱 셸**: `App.vue`가 `<RouterView>` + 전역 `BottomNav`를 렌더. `route.meta.tabBar === false`인 라우트(로그인·결제·AI결과 등 풀스크린 플로우)에서는 BottomNav 숨김. 각 뷰는 자체 BottomNav를 갖지 않습니다.
- **인증**: `stores/auth.js` — JWT accessToken(메모리) + refresh 쿠키. `api/http.js`가 401 발생 시 single-flight refresh → 재시도. 앱 시작 시 `bootstrap()`로 silent refresh. `meta.requiresAuth` 라우트는 전역 가드가 `/login?redirect=...`로 보냅니다. 카카오/구글 OAuth2 지원.
- **API 레이어**: `api/index.js`의 도메인 모듈이 실제 BE 엔드포인트에 정렬됨.
  - 실연동: `auth`, `users/me`, `/api/attractions`, `/api/festivals`, `/api/plans`, `/api/recommendations`
  - mock 폴백(현재 BE 미제공): `hotplace`, `community`, `companion`
- **TourAPI**: `api/festival.js`가 `/api/tour` 프록시로 축제 데이터를 직접 호출.

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
| `/payment`, `/confirmation` | 결제 / 예약확정 | ✗ | |
| `/login`, `/signup`, `/oauth/callback` | 인증 | ✗ | |

핫플(`/hotplace/*`), 동행(`/companion/*`), 채팅(`/chat/*`) 등 FE 고유 화면도 포함합니다.

## 출처

- UI 기준: `gptgeminiclaude/FE`
- 기능/백엔드 연동: `springaitrip/frontend` + `springaitrip/BE`
