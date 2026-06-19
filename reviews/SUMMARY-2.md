# Codex 적대적 리뷰 종합 2차 (나머지 6영역)

> attraction / festival / newdomains(notice·notification·gamification) / preproc-global / fe-infra / fe-views
> 각 `reviews/codex-*.md`. 총 ~58건. (1차 5영역은 SUMMARY.md)

## 🔴 횡단(전 영역 반복) — 고레버리지

| # | 결함 | 근거(대표) |
|---|---|---|
| X1 | **외부 HTTP 호출에 timeout 전무**(TourAPI/Whisper/OpenAI) → 지연 시 servlet thread·트랜잭션 무기한 점유 | `RestClientConfig.java:16-20`, `TourApiClient.java:28`, `WhisperSTTManager.java:46` |
| X2 | **외부 API 호출을 DB 트랜잭션 안에서** 수행(스냅샷 upsert, STT) → DB 락/커넥션이 외부 지연에 묶임 | `AttractionService.java:163`, `PlanService.java:165`, `PreprocessingService.java:31` |
| X3 | **mock/seed 폴백이 장애를 성공으로 위장**(BE/API 실패→가짜 데이터 표시) — attraction·notification·posts·MyPage·Home 전부 | `stores/attraction.js:138`, `stores/notification.js:113`, `stores/posts.js:71`, `HomeView.vue:145` |
| X4 | **`auth.refresh()`가 baseURL 우회**(bare `axios.post('/auth/refresh')`) → 운영(FE/BE origin 분리)에서 refresh·OAuth콜백 실패 | `stores/auth.js:29`, `http.js:31`, `OAuthCallbackView.vue:31` |
| X5 | **인터셉터가 로그인 실패 401에도 refresh 시도** | `http.js:54`, `AuthService.java:68` |
| X6 | **라우트 인증 가드 누락**: `/chat`,`/chat/:id`,`/survey`,`/mypage/challenge`,`/payment`,`/confirmation` | `router/index.js:36,43,53,63` |
| X7 | **N+1**: attraction/festival/album/gamification 목록 전부 | `AlbumService.java:30`, `FestivalService.java:45` |
| X8 | **docker compose 기본 자격증명·관리포트 노출 + 업로드 볼륨 없음 + base64 DB저장** | `docker-compose.yml:18,48`, `WebConfig.java:44`, `PostWriteView.vue:119` |
| X9 | **STOMP 채팅 IDOR**(1차 재확인, 미수정) / **실 시크릿 .env 평문**(검증용) | `StompSessionInterceptor.java:29`, `.env:2-7` |

## 🟠 도메인별 High

**festival (배치)**
- [high] **Reader 싱글턴 상태 재사용** → 2회차 sync가 read 0건 → cleanup이 전 행사 ENDED 처리 (`FestivalItemReader.java:23`, `FestivalSyncJobConfig.java:91`)
- [high] **TourAPI 장애/오류를 "빈 결과"로 처리** → 데이터 ENDED 오염 (`TourApiClient.java:43`)
- [high] 일반 인증자도 수동 sync 가능 + 동시실행 차단 없음 (`SecurityConfig.java:54`, `FestivalController.java:36`)
- [high] 배치 실패도 컨트롤러 항상 200 (`FestivalScheduler.java:30`)

**attraction**
- [high] **Redis 장애가 attraction API 전체 500**(캐시 read가 try 밖) (`AttractionService.java:70`)
- [med] 공개 TourAPI 프록시 → 서버 키 쿼터 무제한 소모(rate limit 없음)

**newdomains**
- [high] **게임화 "이달 챌린지"가 전체 누적 장소로 집계** → 과거 계획만으로 100% (`GamificationService.java:42`)
- [med] 알림 unread 배지가 top50만 세서 51개+면 과소표시, AFTER_COMMIT 알림 실패 영구유실

**preproc-global**
- [high] **STT 실패가 성공 데이터로 저장**(예외 대신 "실패" 문자열을 rawText 저장, 200 반환) (`WhisperSTTManager.java:62`)
- [high] **기본 실행 시 고정 관리자 계정 생성**(admin@triip.com/admin1234, SEED_ENABLED 기본 true) — *우리가 추가. 공유/운영 시 권한탈취* (`DataSeeder.java:85`)
- [med] DB 예외 cause 메시지가 API 응답으로 누출 가능 (`GlobalExceptionHandler.java:157`)

**fe-views**
- [high] 앨범 상세가 실제 API 미사용 + 잘못된 id도 첫 목업 폴백 (`AlbumDetailView.vue:67`)
- [med] 실제 앨범 thumbnail/photos 계약 받고도 화면에서 안 그림

## 권장 우선순위(2차)
1. **X1·X2 외부 timeout + 트랜잭션 분리** — 장애 전파 차단(전 도메인 안정성).
2. **X3 mock 폴백 운영 비활성** — 장애 은폐 제거(가장 혼란 큰 항목, 오늘 이미지건과 동일 계열).
3. **X4·X5 refresh baseURL/auth 제외** — 운영 로그인·OAuth 정상화.
4. **X6 라우트 가드** 일괄.
5. festival 배치(Reader scope·실패 처리·sync 권한), STT 실패 처리, 게임화 월 집계.
6. **관리자 시드 기본 off**(SEED_ENABLED 또는 admin 분리).
