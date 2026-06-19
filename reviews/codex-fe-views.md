검토 범위: 지정된 8개 view와 해당 view가 직접 의존하는 `router/store/api/BE DTO` 계약. 코드 수정 없음.

1. [high] OAuth 콜백이 운영 API baseURL을 우회함  
   `frontend/src/views/OAuthCallbackView.vue:31`에서 `auth.refresh()`를 호출하지만, 실제 구현은 `frontend/src/stores/auth.js:29`의 bare `axios.post('/auth/refresh')`입니다. 반면 공용 HTTP는 `frontend/src/api/http.js:29-32`에서 `VITE_API_BASE_URL`을 적용합니다. 운영에서 정적 FE 호스트와 BE origin이 분리되면 OAuth 콜백 토큰 교환만 FE 호스트로 날아가 로그인 실패가 납니다.  
   수정: `refresh()`도 공용 baseURL 적용 axios/http 인스턴스를 쓰거나, 운영 nginx가 `/auth`를 반드시 프록시하도록 계약을 고정.

2. [high] 앨범 상세가 실제 앨범 API를 전혀 쓰지 않고 잘못된 id도 첫 목업으로 폴백  
   `MyPageView.vue:306-314`는 실제 `/users/me/albums`를 읽고 `MyPageView.vue:171`에서 상세로 이동하지만, `AlbumDetailView.vue:67-74`는 로컬 목업 배열만 보고 `?? albums[0]`로 폴백합니다. BE에는 실제 상세 API가 있습니다: `BE/.../AlbumController.java:43-49`.  
   재현: `/mypage/album/999` 접근 시 404/에러가 아니라 첫 목업 앨범이 표시됨.  
   수정: `/users/me/albums/{id}` 호출, 404 처리, 사진 목록 렌더링, 라우트 인증 적용.

3. [med] 결제/확정 화면이 인증·서버 상태 없이 공개 라우트로 “예약 완료”를 표시  
   `router/index.js:63-64`의 `/payment`, `/confirmation`은 `requiresAuth`가 없습니다. `PaymentView.vue:142-144`, `PaymentView.vue:129`가 실제 결제 없이 `/confirmation`으로 보내고, `ConfirmationView.vue:12-14`, `ConfirmationView.vue:25`, `ConfirmationView.vue:42`가 하드코딩 예약 완료를 표시합니다.  
   수정: 데모 라우트는 운영 빌드에서 제거하거나, 예약 id 기반 서버 상태 확인 후 확정 화면 진입.

4. [med] 취향 설문 저장 실패를 사용자에게 숨기고 홈으로 이동  
   `/survey`는 인증 가드가 없습니다(`router/index.js:53`). 저장은 `PreferenceSurveyView.vue:127-130`에서 `/users/me/preferences`로 보내지만, 실패 시 `PreferenceSurveyView.vue:133-135`에서 무시하고 `PreferenceSurveyView.vue:149`로 홈 이동합니다.  
   수정: 인증 필요 처리 또는 실패 표시/재시도. “다음” 클릭 저장 실패는 조용히 성공처럼 처리하면 안 됩니다.

5. [med] MyPage가 API 실패를 빈 상태/Phase2 상태로 위장  
   계획 로딩 실패는 `MyPageView.vue:278-286`에서 `[]`로 바뀌고 빈 계획 UI가 뜹니다(`MyPageView.vue:129-141`). 앨범 실패도 `MyPageView.vue:304-317`에서 `[]` 처리되어 `albumPhase2`가 켜집니다(`MyPageView.vue:301-302`, `MyPageView.vue:148-157`).  
   수정: 빈 데이터와 로딩 실패를 분리한 error state를 표시.

6. [med] HomeView가 BE/TourAPI 장애를 목업 데이터로 숨김  
   `HomeView.vue:145-147`에서 관광지/후기를 로드합니다. 관광지는 실패 시 `MOCK_ATTRACTIONS`로 대체됩니다(`stores/attraction.js:5-67`, `stores/attraction.js:138-142`). 후기도 실패 시 목업으로 대체됩니다(`stores/posts.js:71-73`, `stores/posts.js:159-221`).  
   수정: 운영에서는 목업 폴백 금지. 장애/빈 상태를 명확히 표시.

7. [med] 실제 앨범 이미지 계약을 받아놓고 화면에서 버림  
   BE 앨범 목록 DTO는 `thumbnailUrl`을 제공합니다(`AlbumSummaryResponse.java:8-13`). `MyPageView.vue:308-314`도 `thumbnailUrl`을 매핑하지만 템플릿 `MyPageView.vue:173-179`는 이미지를 렌더링하지 않습니다. 상세 DTO도 photos가 있습니다(`AlbumResponse.java:9-14`)만 `AlbumDetailView.vue:33-39`는 사진 라벨만 반복합니다.  
   수정: 목록 썸네일과 상세 `photos[].imageUrl` 렌더링.

8. [med] MyPage 앨범 목록이 BE N+1 쿼리를 유발  
   `MyPageView.vue:306`이 `/users/me/albums`를 호출합니다. BE `AlbumService.getAlbums()`는 앨범 목록을 가져온 뒤 각 앨범마다 `countByAlbumId`와 `findAllByAlbumId...`를 호출합니다(`AlbumService.java:30-39`). 앨범 N개면 최소 2N+1 쿼리입니다.  
   수정: count/thumbnail을 projection 또는 group query로 한 번에 조회.

9. [med] ChecklistView는 사용자 계획과 무관한 부산 목업이며 상태 저장도 없음  
   라우트는 `/checklist` 단일 경로입니다(`router/index.js:68`). 화면은 “부산 2박 3일”과 날짜/D-day가 하드코딩입니다(`ChecklistView.vue:29-32`). 항목도 로컬 배열입니다(`ChecklistView.vue:200-213`), 토글도 메모리 변경뿐입니다(`ChecklistView.vue:215-218`).  
   수정: plan id 기반 API 계약 추가, 체크 상태 서버 저장.

10. [low] 체크리스트 긴급 개수 표시가 실제 데이터와 불일치  
   화면은 긴급 2개를 고정 표시합니다(`ChecklistView.vue:52`). 실제 항목 중 `urgent: true`는 하나뿐입니다(`ChecklistView.vue:211`).  
   수정: unchecked urgent 항목 수를 computed로 계산.

11. [low] MyPage 알림 배지가 API 실패 시 가짜 unread로 표시될 수 있음  
   `MyPageView.vue:12`는 `notifStore.hasUnread`로 점을 표시하고 `MyPageView.vue:336`에서 로드합니다. 알림 API 실패 시 seed 데이터로 폴백합니다(`stores/notification.js:116-122`), seed에는 unread가 있습니다(`stores/notification.js:29-53`).  
   수정: 실패 시 seed 대신 error/offline 상태, 또는 dev-only seed.

12. [low] Home 카테고리 칩이 필터 없이 모두 같은 `/explore`로 이동  
   `HomeView.vue:37`의 모든 카테고리 버튼이 동일하게 `/explore`만 push합니다. 카테고리 정의도 label/icon뿐입니다(`HomeView.vue:96-113`).  
   수정: `contentTypeId` 또는 검색 query를 라우트에 전달.