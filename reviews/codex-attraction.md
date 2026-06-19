리뷰 결과: 코드 수정 없이 정적 리뷰만 수행. 심각도순 findings입니다.

- [high] Redis 캐시 장애가 attraction API 전체 500으로 전파됨  
  근거: `BE/src/main/java/com/trip/attraction/service/AttractionService.java:70`, `109`, `139`에서 Redis `get()`이 `try` 밖에 있음.  
  왜: Redis가 캐시인데 Redis down이면 TourAPI fallback도 못 타고 요청이 실패한다.  
  재현/수정: Redis 중지 후 `GET /api/attractions?keyword=서울`. Redis read/write 예외는 별도 catch하고 TourAPI 조회 또는 명확한 503으로 분리.

- [high] TourAPI 호출에 timeout 설정이 없음  
  근거: `BE/src/main/java/com/trip/global/config/RestClientConfig.java:17-20`은 `baseUrl`/header만 설정, client timeout 없음. 실제 호출은 `AttractionTourApiClient.java:47`, `75`, `102`, `122`, `146`.  
  왜: TourAPI 지연 시 servlet thread가 무기한 점유될 수 있다.  
  재현/수정: TourAPI 응답 지연 프록시로 재현. connect/read timeout, retry/backoff, bulkhead 적용.

- [high] 스냅샷 upsert가 외부 API 호출을 트랜잭션 안에서 수행  
  근거: `AttractionService.java:163-165`, `177-179`. plan 경로에서는 이미 트랜잭션 안에서 `upsertSnapshot` 호출: `BE/src/main/java/com/trip/plan/service/PlanService.java:165-173`.  
  왜: 장소 추가 중 TourAPI가 느리면 DB 트랜잭션/락/커넥션 점유 시간이 외부 API 지연에 묶인다.  
  재현/수정: TourAPI 지연 상태에서 일정 담기 동시 요청. 외부 조회와 DB upsert 트랜잭션을 분리.

- [high] 동시 insert 충돌 처리 catch가 안전하지 않음  
  근거: `AttractionService.java:198` 선조회 후 `218` save, `232-236`에서 `DataIntegrityViolationException`을 같은 트랜잭션 안에서 복구 시도.  
  왜: unique 충돌이 flush/commit 시점에 나면 catch가 못 잡고, 잡더라도 트랜잭션 rollback-only 상태일 수 있다.  
  재현/수정: 같은 `contentId/contentType`을 동시에 일정에 추가. DB native upsert, 별도 retry transaction, 또는 명시적 lock/flush 후 새 트랜잭션 재조회.

- [med] 공개 TourAPI 프록시가 서버 API 키 쿼터를 무제한 소모 가능  
  근거: `SecurityConfig.java:57`에서 `GET /api/attractions/**` permitAll, 서버 키는 `AttractionTourApiClient.java:35`, `62`, `89`, `115`, `137`에 주입됨. `page` 상한도 없음: `AttractionService.java:63-64`.  
  왜: 비로그인 스크립트가 검색/page를 반복 호출하면 서버 TourAPI 키 쿼터가 소진된다.  
  재현/수정: 비인증 상태에서 다량 GET 반복. rate limit, page 상한, 캐시 single-flight, 필요 시 인증/캡차 적용.

- [med] “stale 캐시 반환”이 실제로는 만료 캐시를 보존하지 않음  
  근거: TTL 설정 `AttractionService.java:39-45`, 저장 `88`, `119`, `146`; 실패 시 같은 키를 다시 조회 `94-96`, `126-128`, `151-153`.  
  왜: Redis TTL 만료 후에는 키가 삭제되므로 TourAPI 장애 시 stale 데이터가 없다.  
  재현/수정: 캐시 TTL 만료 후 TourAPI 장애 유도. fresh/stale 키 분리 또는 만료 시간을 payload에 넣고 Redis TTL은 더 길게 유지.

- [med] 좌표 입력 검증이 non-blank뿐이라 잘못된 좌표가 TourAPI로 전달됨  
  근거: `AttractionSearchRequestDto.java:23-24`, 컨트롤러는 문자열 그대로 받음 `AttractionController.java:37-42`, 클라이언트는 그대로 전달 `AttractionTourApiClient.java:95-98`.  
  왜: `mapX=abc&mapY=def` 같은 입력이 400이 아니라 외부 API 오류/빈 결과로 흐른다.  
  재현/수정: `GET /api/attractions?mapX=abc&mapY=def`. double parse 및 경도/위도 범위 검증 후 400 반환.

- [med] 캐시 JSON 손상 시 성공 응답으로 빈 결과/null을 반환  
  근거: 검색 캐시 hit는 바로 반환 `AttractionService.java:70-72`, 역직렬화 실패 시 빈 리스트 `293-299`; 상세는 `109-112`, 실패 시 null `307-312`, 컨트롤러는 그대로 200 `AttractionController.java:60-61`.  
  왜: Redis에 깨진 값이 들어가면 장애가 숨겨지고 사용자는 “결과 없음” 또는 null body를 받는다.  
  재현/수정: Redis cache key에 invalid JSON 저장. 손상 캐시는 삭제 후 원천 재조회하거나 502 처리.

- [med] FE attraction 검색 요청 race로 오래된 응답이 최신 상태를 덮어씀  
  근거: `frontend/src/stores/attraction.js:129-138`에 request id/abort 없음. `ExploreView.vue:306-323`, `291-299`, `331-338`에서 검색/카테고리/위치 요청이 연속 발생.  
  왜: 느린 이전 요청이 늦게 도착하면 최신 검색 결과를 덮어쓴다.  
  재현/수정: 느린 네트워크에서 검색어 입력 후 즉시 clear/category 변경. `AbortController` 또는 monotonically increasing request token 적용.

- [med] 운영 장애를 mock 데이터로 성공처럼 숨김  
  근거: 목록 실패 시 mock 대체 `frontend/src/stores/attraction.js:138-141`, 상세 실패 시 mock/null 반환 `165-170`; Home은 오류 표시 없이 attraction만 사용 `HomeView.vue:80-81`.  
  왜: BE/TourAPI 장애 또는 404가 실제 데이터처럼 표시되어 잘못된 관광지를 보여줄 수 있다.  
  재현/수정: BE 중지 후 Home/상세 진입. mock fallback은 dev/demo 플래그로 제한하고 prod에서는 명확한 오류/empty state 표시.

- [med] docker 배포 기본 자격증명이 하드코딩되어 있음  
  근거: `docker-compose.yml:18`, `84` MySQL root/password, `48-49`, `89-90` RabbitMQ guest/guest, 관리 포트 노출 `52-53`; `BE/trip-docker/docker-compose.yml:7`, `35-40`도 동일.  
  왜: 배포 compose를 그대로 쓰면 DB/브로커가 기본 비밀번호와 공개 포트로 노출된다.  
  재현/수정: compose up 후 host에서 15672 접속. 모든 비밀번호 env 주입, 관리 포트 비공개/내부망 제한.

- [low] 상세 이미지 fallback이 깨진 이미지를 placeholder로 대체하지 않고 영역을 숨김  
  근거: `PlaceDetailView.vue:11-15`, `12`의 `onerror`가 parent display를 none 처리.  
  왜: 이미지 URL만 깨진 경우 fallback placeholder가 아니라 빈 hero가 된다.  
  재현/수정: `firstimage`를 404 URL로 응답. 이미지 에러 상태를 두고 placeholder 분기로 렌더링.