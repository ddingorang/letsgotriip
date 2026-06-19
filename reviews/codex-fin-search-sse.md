리뷰 결과: 수정 없이 정적 검토만 했습니다.

**[높음] 공개 검색에서 축제는 상한 없이 전체 활성 행사를 메모리로 읽음**  
왜: `/api/search`는 비회원 공개인데 [SecurityConfig.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/global/config/SecurityConfig.java:63), 축제 검색은 [SearchService.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/search/service/SearchService.java:126)에서 `findByEndDateGreaterThanEqual(LocalDate.now())`로 모든 미종료 축제를 가져온 뒤 [127-130](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/search/service/SearchService.java:127)에서 메모리 필터/limit 합니다. 해당 리포지토리 메서드도 `Pageable` 없는 `List` 반환입니다 [FestivalRepository.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/festival/repository/FestivalRepository.java:17). 데이터가 늘면 공개 엔드포인트 한 번으로 대량 로드가 발생합니다.  
수정: `title/address contains + endDate/status + Pageable` 쿼리로 DB에서 제한하고 인덱스/상한을 적용.

**[높음] SSE 연결 수 제한이 없어 인증 사용자 1명이 emitter를 무한 누적 가능**  
왜: registry는 userId별 리스트만 두고 [SseEmitterRegistry.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/notification/sse/SseEmitterRegistry.java:28), `/stream` 호출마다 무조건 새 emitter를 추가합니다 [36](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/notification/sse/SseEmitterRegistry.java:36). timeout은 1시간이고 [26](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/notification/sse/SseEmitterRegistry.java:26), heartbeat가 전체 emitter를 30초마다 순회합니다 [75-85](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/notification/sse/SseEmitterRegistry.java:75). 탭/스크립트로 연결을 많이 열면 메모리와 heartbeat 작업량이 계속 증가합니다.  
수정: user/global emitter cap, 초과 시 429 또는 오래된 연결 교체, 현재 연결 수 metric/로그 추가.

**[중간] TourAPI 장애가 검색 결과 없음으로 은폐됨**  
왜: `AttractionService.search()`는 외부 실패와 stale miss 시 `EXTERNAL_API_ERROR`를 던지지만 [AttractionService.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/attraction/service/AttractionService.java:105), 통합검색은 모든 예외를 잡아 빈 배열을 반환합니다 [SearchService.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/search/service/SearchService.java:81). `type=attraction` 단독 요청도 200 + empty가 되어 장애와 “검색 결과 없음”이 구분되지 않습니다.  
수정: 단일 attraction 검색은 에러 전파, `all` 검색은 `sourceErrors` 같은 부분 실패 메타를 응답에 포함.

**[중간] 잘못된 `type`이 400이 아니라 200 빈 결과로 처리됨**  
왜: 컨트롤러는 `type`을 문자열 그대로 받고 [SearchController.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/search/controller/SearchController.java:27), 서비스는 허용값 검증 없이 분기합니다 [SearchService.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/search/service/SearchService.java:55). `type=foo`면 모든 조건이 false라 빈 응답 200입니다 [64-73](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/search/service/SearchService.java:64).  
수정: enum/allowlist 검증 후 미지원 type은 400 반환.

**[중간] 게시글/동행 검색은 최신 200개 밖의 실제 매치를 누락함**  
왜: post/hotplace/companion은 DB에서 최신 200개만 가져온 뒤 메모리에서 제목/지역 필터를 합니다 [SearchService.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/search/service/SearchService.java:94), [117](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/search/service/SearchService.java:117). 매칭 글이 201번째 이후면 검색 결과가 없습니다.  
수정: repository에 `title containing ignore case`류 쿼리와 `PageRequest(0, RESULT_LIMIT)`을 추가해 DB에서 검색/제한.

**[중간] FE 검색 응답 레이스로 오래된 결과가 최신 검색어를 덮을 수 있음**  
왜: `runSearch()`는 요청 취소나 sequence check 없이 응답을 바로 상태에 씁니다 [SearchView.vue](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/frontend/src/views/SearchView.vue:272). TourAPI read timeout은 8초라 [AttractionTourApiClient.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/attraction/client/AttractionTourApiClient.java:27), 느린 이전 요청이 빠른 새 요청 뒤에 도착하면 [283-286](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/frontend/src/views/SearchView.vue:283)에서 화면을 덮습니다.  
수정: `AbortController` 또는 request id를 두고 최신 요청만 반영.

테스트는 실행하지 않았습니다. 리뷰 전용 요청이라 코드 변경도 하지 않았습니다.