아래는 근거 있는 지적만 정리했습니다.

[높음] Place 상세 리뷰/찜이 실제 API와 연결되지 않음  
왜: 찜 버튼은 `bookmarked = !bookmarked` 로컬 토글뿐이고, 리뷰는 `STATIC_REVIEWS` 고정 데이터입니다. `reviewApi`/`favoriteApi`는 존재하지만 이 화면에서 미사용입니다.  
근거: `frontend/src/views/PlaceDetailView.vue:26`, `:196`, `:203`, `:251`, `:330`; `frontend/src/api/index.js:47`, `:60`  
수정: 진입 시 `favoriteApi.list/toggle('ATTRACTION', contentId)`, `reviewApi.list(contentId)`를 호출하고 작성/수정/삭제 UI도 API 기준으로 갱신.

[높음] BadgesView가 게임화 실데이터를 대부분 버리고 하드코딩 표시  
왜: `gamiStore.load()` 후 실제 `summary.badges`만 일부 사용하고, 레벨/XP/통계/퀘스트는 전부 상수입니다. 실제 사용자 레벨과 퀘스트 진행이 틀리게 표시됩니다.  
근거: `frontend/src/views/BadgesView.vue:9`, `:13`, `:18`, `:123`, `:132`, `:143`, `:150`  
수정: `summary.level`, `summary.stats`, `summary.inProgressQuests` 또는 `/api/gamification/quests` 결과로 hero/통계/퀘스트를 렌더링.

[높음] 동행 신청 중복 레이스 방어가 DB에 없음  
왜: 서비스는 `existsBy...StatusNot` 확인 후 `saveAndFlush`하고 `DataIntegrityViolationException`을 기대하지만, `CompanionApplication`에는 `(post,user,active status)` 유니크 제약이 없습니다. 동시 신청 두 건이 모두 통과할 수 있습니다.  
근거: `BE/src/main/java/com/trip/companion/service/CompanionService.java:150`, `:162`; `BE/src/main/java/com/trip/companion/entity/CompanionApplication.java:13`, `:24`, `:28`, `:32`  
수정: 활성 신청(PENDING/APPROVED)에 대한 DB 유니크 제약 또는 락 기반 직렬화를 추가하고 충돌은 409로 변환.

[높음] 리뷰 1인 1개 제약도 레이스에 취약  
왜: `existsByUserIdAndContentId` 후 insert만 하고, `attraction_reviews` 엔티티에는 `(user_id, content_id)` 유니크 제약이 없습니다. 중복 작성 실패도 현재 400으로 처리됩니다.  
근거: `BE/src/main/java/com/trip/review/service/ReviewService.java:50`, `:51`, `:53`; `BE/src/main/java/com/trip/review/entity/AttractionReview.java:15`, `:25`, `:29`  
수정: DB 유니크 제약 추가, `DataIntegrityViolationException`을 409 Conflict로 매핑.

[중간] 신청자 승인/거절 실패가 화면에서 사라짐  
왜: 승인/거절 실패 시 store에는 에러가 저장되지만, `CompanionApplicantsView`는 catch에서 아무 것도 표시하지 않고 목록만 재조회합니다. 정원 초과/이미 처리됨/403이 사용자에게 전달되지 않습니다.  
근거: `frontend/src/views/CompanionApplicantsView.vue:23`, `:84`, `:87`, `:91`, `:94`, `:97`; `frontend/src/stores/companion.js:219`, `:221`, `:236`, `:238`  
수정: 화면에 `companionStore.error` 상태/토스트를 표시하고 실패 시 버튼 상태를 서버 응답 기준으로 복구.

[중간] 검색 응답 레이스로 오래된 결과가 최신 검색어를 덮어씀  
왜: debounce는 타이머만 지우고 이미 날아간 요청은 취소/검증하지 않습니다. 이전 요청이 늦게 끝나면 `attractions/posts/companions/festivals`를 그대로 덮어씁니다.  
근거: `frontend/src/views/SearchView.vue:241`, `:247`, `:272`, `:282`, `:286`, `:294`  
수정: `AbortController` 또는 request sequence id로 최신 요청만 반영.

[중간] TourAPI 검색 실패가 “검색 결과 없음”으로 은폐됨  
왜: BE가 attraction 검색 예외를 잡아 빈 배열로 반환하고, FE는 전체 요청 성공으로 처리해 empty state를 보여줍니다. 외부 장애와 실제 무결과를 구분할 수 없습니다.  
근거: `BE/src/main/java/com/trip/search/service/SearchService.java:81`, `:86`, `:88`; `frontend/src/views/SearchView.vue:70`, `:282`, `:286`  
수정: 부분 실패 메타를 응답에 포함하거나 attraction-only 검색에서는 502를 전파.

[중간] 알림 모두 읽음 실패 시 로컬 상태가 거짓 성공으로 남음  
왜: 먼저 모든 알림을 `read=true`로 바꾸고 API 실패를 삼킵니다. 서버에는 안 읽음이 남아도 화면은 읽음 처리된 상태로 유지됩니다.  
근거: `frontend/src/stores/notification.js:119`, `:123`, `:125`, `:126`  
수정: API 성공 후 로컬 반영하거나 실패 시 이전 상태로 롤백.

[중간] 즐겨찾기 토글 API는 동시 생성 충돌을 처리하지 않음  
왜: DB 유니크 제약은 있지만 서비스가 `find -> save`만 하고 `DataIntegrityViolationException`을 잡지 않습니다. 동시 토글 생성 충돌 시 500으로 떨어질 수 있습니다.  
근거: `BE/src/main/java/com/trip/favorite/entity/Favorite.java:16`, `:18`; `BE/src/main/java/com/trip/favorite/service/FavoriteService.java:24`, `:25`, `:32`  
수정: 충돌을 잡아 현재 상태 재조회 후 응답하거나 명시적 add/remove 멱등 API로 분리.

[낮음] Admin 라우터에 관리자 가드가 없음  
왜: FE 라우트는 `requiresAuth`만 검사합니다. BE는 `verifyAdmin`으로 막지만, 비관리자가 `/admin`에 직접 진입해 운영 화면과 403 에러를 보게 됩니다.  
근거: `frontend/src/router/index.js:90`; `frontend/src/views/AdminView.vue:84`; `BE/src/main/java/com/trip/community/service/HotPlaceService.java:163`  
수정: `requiresAdmin` meta와 `authStore.user.userRole === 'ADMIN'` 라우터 가드 추가.