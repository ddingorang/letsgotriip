# Codex 적대적 리뷰 통합 백로그 (SUMMARY-3)

> 입력: reviews/codex-*.md 23개(배치1 5영역 + 배치2 6영역 + 배치3 pg-* 11개 + rag-assistant + plan-ai) 및 SUMMARY.md/SUMMARY-2.md.
> 같은 근원/파일:라인 중복은 1건으로 병합. 행동 영향 없는 순수 스타일 nit는 제외, 보안/정합 이슈는 전부 보존.
> read-only 리뷰이며 코드 수정은 없음.

## 심각도별 집계 (병합 후)

| 심각도 | 건수 |
|---|---|
| high | 26 |
| med | 27 |
| low | 12 |
| **합계** | **65** |

표기 (해결추정) = 최근 신규 작업(checklist/context/plan-share/password-reset/notice-admin/STT-fix/story/group/notification-expansion)으로 이미 해결되었을 가능성.

---

## TOP 10 MUST-FIX (최고 레버리지 보안/정합)

1. STOMP 채팅방 IDOR (구독·송신 멤버십 미검증 + body roomId 스푸핑)
2. 업로드 검증 우회 (MIME·원본 확장자만 신뢰 -> /uploads에 .html/.js 공개)
3. Refresh Token 회전 비원자성 (정상 세션이 reuse 오탐으로 삭제 / 탈취 RT로 연장)
4. 재사용 탐지 후에도 access token이 계속 통과 (jti/familyId 미대조)
5. 외부 HTTP(TourAPI/Whisper/OpenAI) timeout 전무 + 외부호출을 DB 트랜잭션 내 수행
6. mock/seed 폴백이 BE/API 장애를 성공으로 위장 (attraction·notification·posts·hotplace·home)
7. STT 실패가 성공 데이터(rawText)로 저장 + HTTP 200
8. RAG 문서 프롬프트 인젝션 -> 상태변경 Tool(savePlan) 자동 실행
9. 기본 compose가 고정 관리자 계정(admin@triip.com/admin1234)·데모 프리필 생성
10. AI 초안/save-plan이 장소 누락·upsert 실패를 조용히 "저장됨"으로 확정

---

## HIGH (26)

### 보안 - 인가/인증
- [high] [auth/chat] STOMP 채팅방 IDOR - StompSessionInterceptor.java:29 / ChatStompController.java:34 / ChatService.java:53 - CONNECT 토큰만 검증하고 SUBSCRIBE/SEND 멤버십 미검사 + payload chatRoomId 스푸핑 -> 임의 방 송수신 - SUBSCRIBE/SEND마다 active membership 검증, path roomId와 body roomId 일치 검증.
- [high] [chat] 차단/퇴장 멤버도 히스토리 조회 가능 - ChatHistoryController.java:44 / ChatRoomMembership.java:37 - isBanned/leftAt 미검사 - 조회 시 active 멤버만 허용.
- [high] [community/auth] 업로드 검증 우회 - FileStorageService.java:34 / AlbumStorageService.java:31 / SecurityConfig.java:65 - MIME 헤더·원본 확장자만 신뢰 -> /uploads/에 .html/.js 저장·동일 origin 공개 - magic-byte 검사, 서버 결정 확장자 allowlist, attachment/CSP.
- [high] [mypage] 앨범 imageUrl path traversal 삭제 - AlbumCreateRequest.java:6 / AlbumService.java:115 / AlbumStorageService.java:47 - 검증 없는 imageUrls 저장 후 파일명 추출·resolve 삭제 -> ../ 값으로 디렉터리 밖 삭제 - 서버 발급 파일명만 저장, normalize 후 baseDir 하위 검증.
- [high] [auth] Refresh Token 회전 비원자성 - AuthService.java:128-190 / http.js:47 - Redis read-compare-write 분리 + FE single-flight가 탭 단위뿐 -> 동시 refresh가 정상 세션을 SESSION_REUSE로 삭제 / 탈취 RT 연장 - Lua CAS·WATCH/MULTI·세션버전으로 원자화.
- [high] [auth] 재사용 탐지 후에도 access token 통과 - AuthService.java:159 / JwtAuthenticationFilter.java:30 / JwtUtil.java:74 - 필터가 서명만 검증하고 familyId/currentAccessJti를 Redis와 대조 안 함 -> 탈취 토큰 만료까지 유효 - reuse 세션 jti/familyId denylist 또는 Redis 세션 대조.
- [high] [auth] 탈퇴 후 타 기기 refresh 가능 - UserService.java:82 / AuthService.java:136 - 현재 sessionId만 삭제, DB status 미검증 - 탈퇴 시 전 세션 무효화 + active 검증.
- [high] [auth] 실패 로그인 401이 기존 쿠키 세션을 되살림 - http.js:54-73 / auth.js:28-33,55 - /auth/login도 공용 인터셉터를 타 비번오류 401에서 refresh가 성공해 인증상태가 됨 - auth endpoint를 401 refresh 대상에서 제외.
- [high] [community/hotplace] 비승인/반려 핫플 상세 공개 조회 - HotPlaceController.java:35 / HotPlaceService.java:40,147 - 목록은 APPROVED만이나 상세는 findById라 pending/rejected id를 알면 비회원도 조회 - 공개 상세는 findByIdAndStatus(APPROVED).
- [high] [community/hotplace] 관리자 pending GET이 공개 와일드카드에 걸림 + principal NPE - SecurityConfig.java:61 / HotPlaceController.java:71 - GET /community/hotplaces/* permitAll이 /pending에 매칭 -> 익명 호출이 principal.userId() NPE(500) - /pending을 admin 매처로 먼저 선언 + null 방어.
- [high] [secret] 실제 시크릿이 워크트리 env 평문 - .env:2-7 / BE/.env:7-11 - JWT/OpenAI/Tour/Google/Kakao 평문 (gitignore지만 워크트리/로그 공유 시 노출) - 키 회전, secret manager/배포 env 주입.
- [high] [auth/deploy] 기본 compose가 고정 관리자 계정·데모 프리필 생성 - DataSeeder.java:85-89 / application.yaml:109 / docker-compose.yml:103 / LoginView.vue:126 - SEED_ENABLED 기본 true -> admin@triip.com/admin1234 + FE 데모 계정 노출 (우리가 추가) - 배포 SEED_ENABLED=false, 관리자 생성 분리, 프리필 제거.
- [high] [assistant] RAG 문서 프롬프트 인젝션 -> 상태변경 Tool 실행 - AssistantService.java:82-158 - RAG 컨텍스트와 같은 호출에 createTravelPlan Tool 노출, Tool이 savePlan()까지 실행 -> 악성 문서가 영속 데이터 생성 유도 - Tool은 초안까지만, 저장은 사용자 확인 API 분리.
- [high] [assistant] 대화 메모리가 사용자별 격리 안 됨 - AssistantController.java:34 / AssistantService.java:78 - conversationId가 클라 입력 그대로이고 ChatMemory 키도 그뿐 -> ID 유출/재사용 시 타 사용자 맥락 혼입 - 키를 userId:conversationId로 네임스페이스+소유권 검증.

### 안정성 - 외부 호출/장애 전파
- [high] [global] 외부 HTTP timeout 전무 - RestClientConfig.java:16-20 / TourApiClient.java:28 / WhisperSTTManager.java:46 - TourAPI/Whisper/OpenAI connect/read timeout 없음 -> 지연 시 servlet thread·트랜잭션 무기한 점유 - connect/read/response timeout + retry/backoff/CB.
- [high] [global] 외부 API 호출을 DB 트랜잭션 안에서 수행 - AttractionService.java:163 / PlanService.java:165 / PreprocessingService.java:31 - 스냅샷 upsert·STT가 트랜잭션 내 동기 호출 -> DB 락/커넥션이 외부 지연에 묶임 - 외부 조회와 DB 트랜잭션 분리.
- [high] [attraction] Redis 장애가 attraction API 전체 500 - AttractionService.java:70,109,139 - 캐시 read가 try 밖, set 실패도 TourAPI 실패처럼 처리 -> 캐시 장애가 목록/상세/지역코드 전체 장애로 승격 - 캐시 접근 별도 try, 실패 시 TourAPI 결과 반환·warn+skip.
- [high] [preproc] STT 실패가 성공 데이터로 저장 - WhisperSTTManager.java:62 / PreprocessingService.java:61 - 예외 대신 "STT 변환 실패..." 문자열을 rawText에 저장하고 200/dataId 반환 - 실패는 502/503 예외 전파 또는 실패상태 컬럼 분리. (해결추정: STT-fix - 재확인)

### 데이터 정합 - 실패 은폐
- [high] [global/FE] mock/seed 폴백이 장애를 성공으로 위장 - stores/attraction.js:138 / stores/notification.js:113 / stores/posts.js:71 / stores/hotplace.js:95 / HomeView.vue:145 - BE/API 실패 시 가짜 데이터 표시(인기여행지·읽지않은알림·후기·핫플) -> QA가 장애 놓침 - prod mock 제거 또는 명시 demo flag로 격리, 오류/빈 상태 표시.
- [high] [community/FE] 생성/수정 실패를 성공처럼 처리 - PostWriteView.vue:159 / HotplaceRegisterView.vue:284 - 글·핫플 등록 401/400/500에도 mock push·이동·registrationSuccess=true - catch에서 성공처리 금지, 에러 표시.
- [high] [community] 게시글 이미지를 base64로 DB 저장(업로드 API 우회) - PostWriteView.vue:119,149 / CommunityController.java:34 / PostImage.java:23 - multipart 업로드 안 하고 data URL을 길이 무제한 컬럼에 저장 -> DB 비대/500 - uploadImage() 먼저 호출 후 반환 URL만 저장.
- [high] [community] 수정 시 기존 이미지 파일 삭제 후 깨진 URL 재저장 - PostWriteView.vue:180 / CommunityService.java:99,229 / FileStorageService.java:49 - imageUrls에 기존 URL 보내면 BE가 파일 삭제 후 같은 URL 재저장 -> 이미지 깨짐 - 유지=imageUrls null 또는 기존 URL 비교 후 삭제 제외.
- [high] [community] 댓글 수정/삭제/좋아요가 URL postId 미검증 - CommunityController.java:113-147 / CommunityService.java:156,199,208 - commentId만 기준, 삭제된 글의 댓글도 parent 검증 없이 조작 가능 - commentId+postId+post.deleted=false로 조회.
- [high] [community] 좋아요/조회수 카운터 동시성 유실 - CommunityService.java:66,139,168 / Post.java:58 - 엔티티 int 증가라 동시 커밋 시 덮어쓰기 - 원자 update 쿼리/락 또는 like 테이블 집계.
- [high] [plan/recommend] save-plan이 장소 누락·upsert 실패 삼키고 빈 계획 "저장됨" 확정 - RecommendService.java:278 / PlanService.java:326-352 / TripPlace.java:44(memo 300) / ItineraryDraft.reason(무제한) - TourAPI 상세 실패·긴 reason 하나로 장소 조용히 누락, 전부 실패해도 빈 계획 저장 - 변환 실패 집계·최소 장소 미달 시 롤백, reason 절단, 실제 장소 확인 후 markSavedPlan.
- [high] [companion] 탈퇴 멤버가 정원·내 방·목록에 잔존 - ChatService.java:101-113 / CompanionService.java:201-205,288-299 / CompanionApplicationRepository - leave가 leftAt만 세팅, count/list는 전체 membership -> 정원 차지+/my 노출+승인취소 시 멤버십 잔존 - active(leftAt null, isBanned=false) 전용 count/list로 통일.

### 기능 - 동시성/배치/플로우
- [high] [companion] 동시 중복 신청 레이스 - CompanionService.java:150-164 / CompanionApplication.java:12 - exists 후 saveAndFlush인데 (post,applicant) unique 제약 없음 -> 동시 중복 PENDING - DB unique/락, 재신청은 REJECTED row 갱신.
- [high] [companion] 승인 정원 레이스 -> 정원 초과 - CompanionService.java:189-213 / ChatRoomMembership.java:24 - 조회->save, unique 제약 없음 - day/방 단위 락 또는 unique 제약.
- [high] [companion] /companion/posts/my가 공개 GET 매처에 걸려 미인증 500(NPE) - SecurityConfig.java:62 / CompanionController.java:25 - 공개 GET이 /my까지 매칭 - /my를 인증 매처로 먼저 선언 + null 방어.
- [high] [auth] 카카오 로그인 버튼이 실제 미동작 - LoginView.vue:86,166 / application.yaml:15 / OAuth2Attribute.java:21 - FE는 kakao로 보내나 BE는 Google만 지원 - kakao registration/attribute 추가 또는 버튼 제거.
- [high] [auth/FE] 운영 분리배포에서 refresh/OAuth 콜백이 잘못된 origin으로 - auth.js:29 / http.js:31 / OAuthCallbackView.vue:31 - auth.refresh()가 bare axios.post('/auth/refresh')라 baseURL 우회 -> FE origin으로 나가 로그인/콜백 실패 - refresh도 공용 baseURL 인스턴스 사용(인터셉터 재진입만 차단).
- [high] [festival] 배치 Reader 싱글턴 상태 재사용 -> 2회차 sync read 0건·전체 ENDED - FestivalItemReader.java:23 / FestivalSyncJobConfig.java:91 - areaCodeIndex가 끝까지 간 상태로 남음, cleanup이 기존 행사 ENDED 처리 - Reader @StepScope 또는 실행마다 reset, cleanup은 완전 성공 sync에만.
- [high] [festival] TourAPI 장애/오류를 "빈 결과"로 처리 -> ENDED 오염 - TourApiClient.java:43 / FestivalItemReader.java:45 - null body·인증키오류·quota가 빈 페이지처럼 처리 - header/resultCode 파싱, 실패는 Job 실패, stale cleanup은 성공 마커 시에만.
- [high] [festival] 일반 인증자도 수동 sync 가능 + 동시실행 차단 없음 - SecurityConfig.java:54 / FestivalController.java:36 / FestivalSyncScheduler.java:26 - 누구나 외부 호출·DB갱신·cleanup 유발, syncAt마다 새 JobInstance로 동시 실행 - ADMIN 제한, 실행 중 409, ShedLock.
- [high] [festival] 배치 실패도 컨트롤러 항상 200 - FestivalSyncScheduler.java:30 / FestivalController.java:37 - JobExecution 상태 미확인, 예외 삼킴 - getStatus/ExitStatus로 FAILED 5xx·실행중 409·비동기 202.
- [high] [gamification] 이달 챌린지가 전체 누적 장소로 집계 -> 과거 계획만으로 100% - GamificationService.java:30,42 / PlanRepository.java:40 - 날짜 필터 없이 전체 count - startDate/endDate 또는 추가시점 기준 월 필터.
- [high] [assistant] PDF/텍스트 추출 실패가 성공(INGESTED)으로 저장 - DocumentService.java:57-74,143 - extractText 실패/non-UTF-8을 null로 삼키고 markIngested(0) -> RAG엔 빈 데이터 - 실패/미지원을 FAILED/UNSUPPORTED로 저장·사유 노출.
- [high] [assistant] 문서 삭제 실패 시 벡터 잔존 -> 삭제 문서 계속 RAG 참조 - AssistantService.java:69-76 / DocumentService.java:92 / IngestionService.java:67 - 벡터 삭제 실패 삼키고 DB 문서만 삭제, 검색은 userId만 필터 - 삭제 실패 전파/outbox/tombstone.
- [high] [plan] 이전 플랜 동선 리포트가 현재 플랜에 적용될 수 있음 - stores/plan.js:38-61 / PlanReportView.vue:186,410 - routeReport 전역인데 loadPlan이 초기화 안 함, planId 검증 없이 stale report.days 적용 - plan별 초기화, report.planId==planId일 때만 사용.
- [high] [plan] 장소 재정렬이 DB 데이터 대신 TourAPI 재호출 - stores/plan.js:148 / PlanService.java:217 / AttractionService.java:163 - 기존 placeId 대신 contentId만 보내 매 항목 upsertSnapshot(외부 호출, timeout 없음) - 기존 장소는 placeId로 교체, 신규만 외부 호출.
- [high] [plan] 동시 장소 추가 seq/중복 race가 500 - PlanService.java:174-183 / TripPlace.java:17 - max(seq)+1 계산이 동시 요청에 동일값 통과, unique 위반이 catch-all 500 - day 단위 락 또는 제약 위반을 409로 변환.
- [high] [plan] replacePlaces 입력검증 부재로 500 - PlaceItemDto.java:9 / PlacesReplaceRequestDto.java:7 / PlanService.java:239 - seq/content/memo 검증 없이 nullable=false 컬럼 입력, 중복 seq/content - @Valid/@NotNull/@Positive·중복 검증, 400/409.
- [high] [plan] 좌표 없는 장소가 동선 최적화에서 거리 0 처리 - RouteCalculator.java:22,61 - haversineKm가 좌표 없으면 0, nearestNeighbor가 필터 안 함 -> 잘못된 추천 순서 - 좌표 없는 장소 제외/뒤 고정 + missingCoordinates 명시.

---

## MED (27)

- [med] [global] mock 상세 캐시 깨짐이 200 null/FE TypeError - AttractionService.java:109,307 / attraction.js:162 - 역직렬화 실패 시 null 반환·FE가 contentId 바로 읽음 - 손상 캐시 삭제 후 재조회, FE null 가드.
- [med] [attraction] 공개 TourAPI 프록시가 서버 키 쿼터 무제한 소모 - SecurityConfig.java:57 / AttractionService.java:63 - permitAll + page 상한 없음 -> 비인증 반복 호출로 쿼터 소진 - rate limit, page 상한, single-flight 캐시.
- [med] [attraction] 캐시 JSON 손상 시 빈 결과/null 성공 응답 - AttractionService.java:70,293 - 손상 캐시가 "결과 없음"으로 장애 은폐 - 손상 캐시 삭제 후 재조회 또는 502.
- [med] [attraction] 외부 API 오류 payload를 빈 성공으로 처리 - AttractionTourApiResponse.java:26 / AttractionTourApiClient.java:160 - body/items 없으면 List.of() -> 오류가 "결과 없음" - header/resultCode 모델링, 비정상 코드 502.
- [med] [attraction/festival] 좌표/contentType 입력 검증 부재 - AttractionController.java:31 / AttractionSearchRequestDto.java:23 - non-blank만 검증해 mapX=abc가 외부 장애로 흐름 - 위경도 숫자·범위, contentTypeId whitelist 검증.
- [med] [attraction/FE] 검색/필터 요청 레이스로 오래된 응답이 최신 덮음 - attraction.js:129 / ExploreView.vue:306 - request id/abort 없음 - AbortController 또는 request token.
- [med] [chat] 나간 멤버도 정원 카운트에 포함 - ChatService.java:112 / CompanionService.java:201 - countByChatRoomId가 left/banned 포함 -> 누가 나가도 full - active count 쿼리.
- [med] [chat] 히스토리 로드와 STOMP 구독 사이 메시지 유실 레이스 - ChatRoomView.vue:309 / chat.js:88 - REST 먼저·구독 나중이라 사이 메시지 누락 - 구독 후 cursor/timestamp catch-up.
- [med] [chat] 히스토리 전체 조회 + 인덱스 없음 - ChatMessageRepository.java:11 / ChatService.java:70 / ChatMessage.java:11 - 전체 List 후 메모리 정렬 -> 메시지 쌓이면 지연 - (chatRoomId,timestamp) 인덱스 + cursor 페이징.
- [med] [chat] 참여자 조회 N+1 - ChatService.java:132 - active 멤버마다 findById - userId 목록 일괄 조회/join.
- [med] [chat] STOMP 메시지 DTO 검증 미적용 - MessageSendRequestDto.java:13 / ChatStompController.java:35 - @NotBlank/@Size 있으나 컨트롤러에 @Valid 없음 -> 빈/과대 content가 Mongo 저장 - @Payload @Valid + 서비스 type별 검증.
- [med] [companion] 만석 모집글에도 신청 가능 - CompanionService.java:147 / CompanionDetailView.vue:120 - 신청은 상태/중복만 보고 정원은 승인 단계만 막음, FE도 0자리에 버튼 노출 - 신청 시 active >= max면 409, 만석 CLOSED/비활성.
- [med] [companion] 모집 생성 DTO가 DB 제약과 불일치 - CompanionPostCreateRequest.java:15 / CompanionPost.java:40 - duration/description null 허용 DTO인데 엔티티 nullable=false, 과거 날짜 허용 -> 500 - @NotBlank/@FutureOrPresent.
- [med] [companion/community/mypage] 목록·신청자·핫플·앨범 N+1 - CompanionService.java:100,303 / CommunityService.java:38 / HotPlaceService.java:35,113 / AlbumService.java:30 - 게시글/앨범마다 count·thumbnail·author lazy -> 1+2N 쿼리 - fetch join/projection/batch count.
- [med] [companion] 신청자 관리 직접 진입 시 글 정보 빔 - CompanionApplicantsView.vue:12,66 - mount가 getApplications만 호출, 새로고침 시 제목/정원 빔 - getDetail도 호출 또는 API가 글 요약 반환.
- [med] [companion] 승인된 신청자에게 "승인 대기 중" 배너 - CompanionDetailView.vue:60,126 - 조건이 isApplied && !isOwner라 APPROVED도 대기 표시 - 조건에 !isApproved 추가.
- [med] [community] 목록 size 무제한 - CommunityController.java:49 / PlanController.java:41 - 큰 size로 N+1 폭증 - size 상한 검증/@Min/@Max.
- [med] [community/plan/companion/survey/profile] 도메인 DTO 입력 검증 부재 - PostCreateRequest.java / PreferenceUpdateRequestDto.java:10 / UserUpdateRequestDto.java:3 / RecommendRequestDto.java - @Valid/@NotBlank/@Size/allowlist 부재로 400 대신 500/오염 (GlobalExceptionHandler 400 매핑은 별개) - 도메인 DTO 제약 일괄 추가.
- [med] [community] FE 입력 필드와 BE DTO 계약 불일치 - PostWriteView.vue:90 / PostCreateRequest.java:8 / PostDetailView.vue:76 - 장소·태그 입력받지만 DTO에 없어 미저장, 상세는 렌더 - BE 필드 추가 또는 FE 입력 제거.
- [med] [recommend] 실패 추천이 결과 화면 "최신"으로 로드+저장 버튼 활성 - RecommendService.java:229 / AiResultView.vue:197,151 - history 자동 로드가 FAILED draft null도 로드, 저장 버튼이 saveLoading만 봄 - SUCCESS/PARTIAL만 로드, FAILED는 재생성 CTA.
- [med] [recommend] savedPlanId가 삭제된 plan 영구 참조 - RecommendService.java:255 / PlanService.java:153 / schema.sql:120 - FK/unique 아니고 plan 삭제 시 역참조 미정리 - ON DELETE SET NULL 또는 삭제 시 정리.
- [med] [recommend] resultJson 파싱 실패 은폐로 SUCCESS인데 draft null - RecommendationResponseDto.java:21 / AiResultView.vue:40 - 파싱 예외 무시·draft null 반환 - 파싱 실패 로깅·5xx, FE 저장 불가 처리.
- [med] [plan] 여러 일자 동선 적용 부분 커밋 - PlanReportView.vue:410 / PlanService.java:204 - 일자별 순차 PUT, 중간 실패 시 앞 일자만 적용 - 배치 API 단일 트랜잭션.
- [med] [plan] FE-BE DTO 계약 불일치(destination/spots/summary) - PlanView.vue:53,83 / PlanSummaryResponseDto.java:10 / DayResponseDto.java:7 - FE가 없는 필드 렌더 - DTO 추가 또는 FE 표시 수정.
- [med] [plan] replacePlaces placeId 소속 검증이 dayNo 미확인 - PlanService.java:205,221 - 같은 plan이면 타 일자 placeId도 통과 - getDay().getDayNo()==dayNo 검증.
- [med] [plan/community/mypage] 플랜·앨범·MyPage 로드 실패를 빈 상태로 은폐 - PlanView.vue:15,202 / MyPageView.vue:278,304 / stores/plan.js:29 - 실패가 []로 바뀌어 빈상태/Phase2 위장 - error 상태 분리 렌더.
- [med] [auth/FE] 인터셉터가 로그인 실패 401에도 refresh 시도 - http.js:54 / auth.js:55 / AuthService.java:68 - 모든 401을 refresh 대상으로 처리, 비번오류 401도 - auth endpoint(login/signup/refresh) 제외.
- [med] [auth] 가입 성공 후 자동 로그인 실패를 "가입 실패"로 표시 - SignupView.vue:169 / AuthService.java:47,102 - 회원 생성 성공인데 Redis/로그인만 실패 시 가입 오류 표시·재시도 시 중복 - signup/auto-login 분리, 로그인 화면 안내.
- [med] [auth] 로그인 API가 계정 존재여부를 상태코드로 노출 - AuthService.java:64 / ResponseCode.java:36 - 없는 이메일 404 vs 비번오류 401 -> enumeration - 동일 401/코드/지연.
- [med] [auth] OAuth 사용자정보 null 처리 누락 NPE/DB오류 - OAuth2Attribute.java:31 / User.java:54,106 - picture/name/email null 허용 후 Map.of·nullable=false에 입력 - null 기본값·필수 claim 검증.
- [med] [auth/deploy] OAuth/CORS/쿠키 기본값이 localhost·Secure=false - application.yaml:21,114,120 / CookieUtil.java:43 / docker-compose.yml:115,120 - redirect/FE/CORS가 localhost 고정, Secure=false라 HTTPS에서도 HTTP 쿠키 - 배포 도메인 env 필수, prod COOKIE_SECURE=true.
- [med] [survey] 취향설문 인증 가드 없음 + 저장 실패 은폐 + DTO 검증 없음 - router/index.js:53 / PreferenceSurveyView.vue:127 / PreferenceUpdateRequestDto.java:10 - 비로그인 진입·실패 무시 후 /home 이동·allowlist 없어 255 초과 500 - requiresAuth, 실패 표시/재시도, DTO size/allowlist.
- [med] [hotplace] 등록 즉시 APPROVED로 승인 플로우 우회 - HotPlaceService.java:51 - pending/approve/reject 플로우 있는데 등록이 즉시 APPROVED -> 스팸 즉시 노출 (우리가 "등록 후 노출"로 변경, 정책 재확인) - 등록 PENDING, 공개는 APPROVED만.
- [med] [hotplace] 위치 미선택·잘못된 좌표 등록 가능 - HotplaceRegisterView.vue:131,275 / HotPlaceCreateRequest.java:10 - 기본 좌표 고정·이름/카테고리만 검증·BE도 카테고리만 @NotNull - FE 마커 필수, BE @NotBlank·위경도 범위.
- [med] [hotplace] 이미지 URL 무제한·무검증 저장 - HotPlaceCreateRequest.java:16 / HotPlaceService.java:121 - 개수·URL·도메인 검증 없이 전체 저장·inline background 렌더 - 최대개수/길이/프로토콜/도메인 검증.
- [med] [hotplace] Kakao SDK 실패 후 검색 null deref - HotplaceRegisterView.vue:157,200 - 키 누락 reject 후에도 검색 UI 노출·geocoder/ps 무방어 호출 - ready 전 검색 비활성·null guard.
- [med] [assistant] 업로드 타입 검증 사실상 없음 + 공개 정적 루트 저장 - DocumentController.java:53 / DocumentService.java:104 / WebConfig.java:42 - 빈/20MB만 검사·OTHER도 저장·/uploads/** 공개 - PDF/TXT whitelist, RAG 문서는 private storage.
- [med] [assistant] RAG/LLM 장애가 전부 500 - AssistantService.java:82 / GlobalExceptionHandler.java:167 - VectorStore/OpenAI 예외 분류·timeout·fallback 없음 - 502/503 매핑, timeout, "RAG 없이 답변" fallback.
- [med] [assistant] 재인덱싱이 기존 벡터 미삭제 -> 중복/stale 청크 - UserDataIndexer.java:33 / IngestionService.java:58 - add()만 해 수정 후 옛 내용도 검색 - ingest 전 deleteByDoc 또는 deterministic chunk id upsert.
- [med] [assistant] 여행계획 인덱싱 N+1 - PlanRepository.java:27 / UserDataIndexer.java:37 - plan 목록 후 lazy days/places/attraction 루프 - fetch join/entity graph/projection.
- [med] [assistant] 파일명 검증 누락으로 500·파일 찌꺼기 - DocumentController.java:53 / DocumentService.java:47 / TripDocument.java:24 - null/blank/255 초과 미검증, DB 실패 전 파일 저장됨 - 파일명 검증·안전한 fallback·보상 처리.
- [med] [preproc] 카카오 업로드 검증 부재로 사용자 입력 오류가 500 - PreprocessingService.java:38,57 - 빈/크기/확장자/charset 미검증, 비 UTF-8이 서버 예외 - multipart 검증·디코딩 실패 400.
- [med] [global] 원인 예외 메시지가 API 응답으로 누출 - ResponseCode.java:108 / GeneralException.java:71 / GlobalExceptionHandler.java:157 - DataIntegrityViolation 메시지가 응답에 붙어 제약명/SQL 노출 - 고정 메시지, cause는 로그만.
- [med] [preproc/global] DataSeeder/NoticeSeeder 부분 시드 고착·비트랜잭션 - DataSeeder.java:50 / NoticeSeeder.java:15 - 트랜잭션 없이 예외 삼킴, 중간 실패 시 count>0로 영구 skip, NoticeSeeder는 SEED_ENABLED 무시 - run() 트랜잭션화·@ConditionalOnProperty·idempotent. (해결추정: notice-admin - 재확인)
- [med] [notification] unread 배지가 top50만 세서 51개+ 과소표시 - NotificationRepository.java:14 / notification.js:86 - 목록 top50 count로 배지 - unreadCount() API로 서버 카운트 사용. (해결추정: notification-expansion - 재확인)
- [med] [notification] "모두 읽음"이 서버 실패 전 로컬부터 변경·실패 삼킴 - notification.js:140 - PATCH 실패해도 UI 모두 읽음 - API 성공 후 반영 또는 rollback. (해결추정: notification-expansion)
- [med] [notification] AFTER_COMMIT 알림 적재 실패 영구 유실 - NotificationEventListener.java:21 / NotificationService.java:27 - 커밋 후 저장 실패를 로그만, 재시도/outbox 없음 - outbox/재시도 큐. (해결추정: notification-expansion)
- [med] [gamification/notification] 챌린지 상세·알림 라우트 인증 가드 누락 - router/index.js:43,44,47,48 / GamificationController.java:23 - /mypage/challenge·앨범상세 비로그인 진입 시 401 삼킨 빈 화면 - requiresAuth 추가.
- [med] [misc/FE] 결제/확정 라우트 인증 가드 누락 + 결제 없이 완료 화면 - router/index.js:63,67 / PaymentView.vue:127 / ConfirmationView.vue:12 - /payment·/confirmation에 requiresAuth 없고 PG/예약 API 없이 confirmation 이동·하드코딩 완료 - 가드 추가, 서버 예약/결제 id 검증.
- [med] [misc/FE] 결제 약관 동의 기본 true - PaymentView.vue:111,129 - agreed 초기 true라 동의 없이 결제 활성 - 기본 false, 약관 링크·서버 검증.
- [med] [misc] 체크리스트 상태가 사용자/여행별 미저장·카운트 불일치 - ChecklistView.vue:193,52,200 - ref 배열·메모리 토글·하드코딩 부산 목업·긴급 카운트 불일치 - plan/user 키 API 또는 localStorage, computed 카운트. (해결추정: checklist - 재확인)
- [med] [festival] 필수 title 미검증으로 1건이 배치 중단 - FestivalItemProcessor.java:21,50 / Festival.java:20 - contentId만 검사, title null이 nullable 위반 - title nonblank 검증·skip policy.
- [med] [festival] 지역코드 체계 충돌(lDongRegnCd vs legacy areaCode) - FestivalItemProcessor.java:43 / RecommendService.java:304 - 축제는 26 저장, AI 추천은 6 사용 -> findByAreaCode("6")가 부산 축제 못 찾음 - 별도 컬럼/매핑 테이블·조회 전 변환.
- [med] [festival] 날짜 기준이 JVM TZ·DB CURDATE·스케줄러 KST로 분산 - FestivalSyncScheduler.java:23 / TourApiClient.java:26 / FestivalService.java:27 - TZ 불일치 시 00:00~08:59 KST 구간 오차 - Clock/ZoneId.of("Asia/Seoul") 주입.
- [med] [festival] 공개 목록 무제한 + 조회 인덱스 없음 - FestivalService.java:22,45 / FestivalRepository.java:19 / schema.sql:25 - 전체 반환·인덱스 없어 풀스캔 - Pageable/limit·(area_code,status,end_date) 인덱스.

---

## LOW (12)

- [low] [recommend] 캐시 히트에도 201 Created - RecommendService.java:127 / RecommendController.java:33 - 캐시 히트를 200으로 구분.
- [low] [recommend] /ai 확인 화면이 history.state에만 의존 - AiPlanInputView.vue:338 / AiInputView.vue:142 - 새로고침/직접 진입 시 조건 소실 - Pinia/sessionStorage 또는 redirect.
- [low] [plan] /api/plans page size 하한 검증 없음 - PlanController.java:41 / PlanService.java:85 - size=0/-1이 프레임워크 예외 - @Min(1)/clamp.
- [low] [plan] PlanView 하드코딩 동행 데이터 - PlanView.vue:196 - 정적 배열 노출 - API 데이터 또는 제거.
- [low] [plan] 리포트 추천 없을 때 확인 버튼 로직 죽음 - PlanReportView.vue:145,403 - canApply false인데 분기 unreachable - 라벨 변경/활성화.
- [low] [auth] FE/BE 닉네임 검증 계약 불일치 - SignupView.vue:58 / SignupRequestDto.java:9 - FE 2~20인데 BE max=20만 -> 1자 가입 가능 - BE @Size(min=2,max=20).
- [low] [chat] 채팅 목록 last message/time/unread가 계약에 없음 - MyCompanionRoomResponse.java:9 / ChatRoomListView.vue:36 - 빈/0 표시 - DTO에 필드 추가 또는 UI 제거.
- [low] [chat] 하드코딩 날짜 구분선·죽은 plan 타입 분기 - ChatRoomView.vue:40,62 / MessageType.java:3 - 항상 "6월 10일", plan 타입 미존재 - timestamp 기반 구분선·타입 계약 정합.
- [low] [chat] 댓글 좋아요 UI가 API 미연결 - PostDetailView.vue:125 / api/index.js:112 - 핸들러 없음 - likeComment 호출 연결.
- [low] [companion] 승인/거절 실패가 화면 미표시 + 등록 더블클릭 중복 생성 - CompanionApplicantsView.vue:84 / CompanionWriteView.vue:11,188 - store error 미렌더·submitting unused - error 표시·in-flight guard.
- [low] [home/attraction] 홈 카테고리 칩이 필터 미전달 + 북마크 토글 미반영 - HomeView.vue:36,49 / PlaceCard.vue:7 - 모두 /explore만 push·bookmarked prop 미전달 - query param 전달·:bookmarked 바인딩.
- [low] [attraction] 상세 이미지 fallback이 placeholder 대신 영역 숨김 - PlaceDetailView.vue:11 - onerror가 display none - placeholder 분기.
- [low] [hotplace] 사진 UI·상세 카운터·CTA가 죽은 기능/하드코딩 - HotplaceRegisterView.vue:100 / HotplaceDetailView.vue:21,54 - 사진 버튼 핸들러 없음·"사진 1/24" 고정·길찾기/북마크 무동작 - 연결 또는 비활성/숨김.
- [low] [festival] status=ENDED API 계약과 실제 동작 불일치 - festival.js:18 / FestivalService.java:36 - FE는 ENDED 허용 문서, BE는 endDate>=today 강제 - 거부/문서화 또는 종료 이력 조회.
- [low] [mypage] 챌린지 기간 6월 하드코딩·뱃지 레벨/XP/퀘스트 하드코딩·탭 콘텐츠 미변경·앨범 썸네일/생성 죽음·알림 link/단건읽음 미연결 - ChallengeDetailView.vue:65 / BadgesView.vue:9,34 / MyPageView.vue:164,336 / NotificationsView.vue:31 - 정적 값/죽은 컨트롤 - DTO 확장 또는 UI 정리. (해결추정: story/group/notification-expansion 일부)
- [low] [misc] 결제 금액/숙박기간 표시 불일치·날짜/D-day 하드코딩 오류·체크리스트 죽은 컨트롤 - PaymentView.vue:152 / ConfirmationView.vue:126 / ChecklistView.vue:11 - 2박-1박 라벨·요일/D-day 오류·핸들러 없는 버튼 - ISO 날짜 기반 계산·기능 구현/제거.
- [low] [assistant] FE가 PENDING을 완료로 표시 - DocumentStatus.java:3 / DocumentsView.vue:148 - PENDING 미처리 default 완료 - PENDING->처리중, INGESTED->완료, FAILED->실패 매핑.

---

## 횡단 패턴 요약

- 입력 검증 부재: 대부분 컨트롤러 @Valid 없음, DTO에 @NotBlank/@Size/@Min/allowlist 없음 -> 400 대신 500/오염. (GlobalExceptionHandler 400 매핑은 별개, 도메인 제약 필요)
- N+1: attraction/festival/album/community/companion/chat/gamification 목록·상세 전부.
- FE가 실패를 성공으로: mock/seed 폴백, 등록 실패 후 이동, "모두 읽음" 낙관 갱신.
- FE-BE 계약 불일치: AI status DONE vs SUCCESS, plan destination/spots/summary, chat plan 타입, 닉네임 길이, 알림 link.
- 외부 호출 안정성: timeout 전무 + 트랜잭션 내 호출 + 오류를 빈결과로 처리.
- 동시성: 좋아요/조회수 ++, 중복 신청, 정원, plan seq, RT 회전.
- 배포/시크릿: compose 기본 자격증명(root/password, guest/guest)·관리포트 노출, 업로드 볼륨 미마운트, 쿠키 Secure=false, env 평문 시크릿, 고정 admin 시드.

## 권장 실행 순서
1. STOMP 인가(IDOR)·업로드 검증·RT 회전/탈퇴 무효화 - 보안 최우선.
2. 외부 timeout + 트랜잭션 분리 - 전 도메인 안정성.
3. mock/seed 폴백 prod 비활성 - 장애 은폐 제거.
4. refresh baseURL/auth 인터셉터 제외 + 라우트 가드 일괄.
5. plan/recommend 빈 계획 저장·동선 stale·seq race, festival 배치(Reader scope·실패처리·sync 권한), gamification 월 집계, STT 실패 처리.
6. 도메인 DTO @Valid 일괄, N+1 정리, 배포 시크릿/시드 기본 off.
