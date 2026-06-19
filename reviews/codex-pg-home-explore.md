코드 수정 없이 정적 리뷰만 했습니다. 라인 근거 있는 항목만 적습니다.

[high] Redis 장애가 `/api/attractions` 공개 조회를 500/502로 죽임  
근거: `AttractionService.java:70`, `:109`, `:139`에서 Redis `get`이 try 밖에 있고, `:87-88`, `:119`의 Redis `set` 실패도 TourAPI 실패처럼 처리됨.  
왜: 캐시는 보조 수단인데 Redis 장애가 목록/상세/지역코드 전체 장애로 승격된다. 수정: Redis 접근을 별도 try로 감싸고 실패 시 TourAPI 결과를 그대로 반환, 캐시 실패는 warn+skip.

[high] TourAPI 동기 호출에 타임아웃이 없음  
근거: `RestClientConfig.java:15-20`은 `baseUrl`/header만 설정하고 connect/read timeout이 없다. `AttractionTourApiClient.java:31-48`, `:58-75`, `:85-103`, `:112-123`은 `.retrieve().body(...)`로 요청 스레드를 점유한다.  
왜: TourAPI 지연 시 Spring worker가 묶여 홈/탐색 전체가 멈출 수 있다. 수정: request factory timeout, retry/circuit breaker, 실패 상태코드 매핑을 명시.

[med] 깨진 상세 캐시가 200 null/FE 런타임 오류로 이어짐  
근거: `AttractionService.java:109-112`는 캐시값을 바로 반환하고, `:307-313`은 역직렬화 실패 시 `null` 반환. FE는 `attraction.js:162-163`에서 `mapAttraction(data)`를 호출하고 `mapAttraction`은 `:90`에서 `item.contentId`를 바로 읽는다.  
왜: Redis 값 하나가 깨지면 상세 API가 정상 200처럼 보이거나 FE가 TypeError로 터진다. 수정: 역직렬화 실패 시 캐시 삭제 후 원본 재조회, FE도 응답 null 가드.

[med] 검색/필터 요청 레이스로 오래된 응답이 최신 화면을 덮음  
근거: `ExploreView.vue:306-323` 검색 debounce, `:291-299` 카테고리 선택이 모두 `store.list`를 호출한다. `attraction.js:129-145`는 요청 식별자/취소 없이 마지막 도착 응답을 그대로 `attractions`에 대입한다.  
왜: 느린 이전 요청이 늦게 끝나면 현재 검색어/카테고리와 다른 목록·지도 핀이 표시된다. 수정: AbortController 또는 request sequence로 최신 요청만 반영.

[med] 외부 API 오류 응답을 빈 성공으로 삼을 수 있음  
근거: `AttractionTourApiResponse.java:26-38`은 `response.body`만 모델링하고, `AttractionTourApiClient.java:160-166`은 body/items가 없으면 `List.of()`를 반환한다.  
왜: TourAPI가 오류 payload를 줘도 사용자는 “검색 결과 없음”으로 보게 된다. 수정: TourAPI header/resultCode를 모델링하고 비정상 코드는 502/운영 로그로 분리.

[med] 좌표/콘텐츠 타입 입력 검증 없이 외부 API로 전달  
근거: `AttractionController.java:31-39`는 `mapX`, `mapY`, `contentTypeId`를 문자열로 받고, `AttractionSearchRequestDto.java:23-24`는 non-blank만 좌표로 인정한다. 이후 `AttractionTourApiClient.java:95-99`가 그대로 query param으로 보낸다.  
왜: `mapX=abc` 같은 요청이 400으로 걸러지지 않고 외부 장애/빈 결과로 흐른다. 수정: 위경도 숫자·범위, contentTypeId whitelist 검증.

[med] 실패를 mock이 숨김  
근거: `attraction.js:138-141`은 목록 실패 시 mock을 채우고, `:165-170`은 상세 실패 시 mock/null을 반환한다. `HomeView.vue:80-83`, `:145-148`은 오류 표시 없이 그 데이터를 사용한다. `places.js:19-31`도 실패를 mock으로 삼킨다.  
왜: API 장애가 홈에서는 정상 인기 여행지처럼 보이고, 테스트/QA가 백엔드 실패를 놓친다. 수정: prod에서는 mock fallback 제거 또는 명시적 demo mode로 격리.

[med] 상세 실패 상태에서도 “일정에 담기”가 활성화됨  
근거: `PlaceDetailView.vue:83-93` 버튼은 `place` 유효성으로 disabled 되지 않는다. `attraction.js:165-170`은 실패 시 `null` 가능, `PlaceDetailView.vue:226-229`는 `route.params.id`와 기본 `contentType=12`로 추가 요청을 보낸다.  
왜: 존재하지 않거나 상세 조회 실패한 장소도 일정 추가 흐름으로 들어간다. 수정: 상세 성공 전 버튼 비활성화, null/404/502는 fatal state로 처리.

[med] 토큰 refresh가 API baseURL을 무시함  
근거: `http.js:29-32`는 `VITE_API_BASE_URL`을 쓰지만, `auth.js:28-31`의 refresh는 raw `axios.post('/auth/refresh')`다. 401 인터셉터는 `http.js:61-73`에서 이 refresh를 호출한다.  
왜: 운영에서 API origin을 분리하면 refresh만 프론트 origin으로 가서 만료 토큰 복구가 실패한다. 수정: refresh도 동일 baseURL을 쓰되 인터셉터 재귀를 막는 skip 옵션을 둔다.

[low] 홈 카테고리 칩이 필터를 전달하지 않음  
근거: `HomeView.vue:36-40`은 어떤 칩이든 `/explore`만 push한다. `ExploreView.vue:177`, `:181-187`은 기본값 `all`로 시작한다.  
왜: “맛집”을 눌러도 탐색은 전체 목록이다. 수정: query/category param을 넘기고 Explore에서 초기화/watch.

[low] 북마크 토글 UI가 실제로 변하지 않음  
근거: `HomeView.vue:49`는 `PlaceCard`에 `bookmarked` prop을 넘기지 않고, `:115-117`만 store 객체를 mutate한다. `PlaceCard.vue:7`, `:41-44`는 prop 기본값 `false`를 사용한다.  
왜: 사용자가 눌러도 아이콘 상태가 계속 비어 있다. 수정: `:bookmarked="place.bookmarked"` 전달 또는 미구현 기능 제거.