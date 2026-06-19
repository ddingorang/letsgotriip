코드 수정 없이 리뷰만 수행했습니다. 심각도순입니다.

- [high] 토큰 갱신이 API baseURL을 우회함 / `http`는 `VITE_API_BASE_URL`을 쓰지만 `refresh()`는 raw `axios.post('/auth/refresh')`를 호출합니다. 만료 토큰 401 retry도 이 경로를 탑니다. prod에서 API origin 분리 시 마이페이지 인증 복구가 깨집니다. 근거: `frontend/src/api/http.js:31`, `frontend/src/api/http.js:62`, `frontend/src/stores/auth.js:29`. / 수정: refresh도 동일 baseURL axios 인스턴스 또는 별도 configured client 사용.

- [high] 앨범 상세가 실제 BE/권한/ID를 전혀 쓰지 않는 mock / 마이페이지는 실제 앨범 API를 읽지만, 상세 화면은 정적 배열에서 찾고 없으면 첫 앨범을 보여줍니다. 완료 계획의 “앨범 보기”도 album id가 아니라 plan id로 이동합니다. 근거: `frontend/src/views/MyPageView.vue:120`, `frontend/src/views/MyPageView.vue:329`, `frontend/src/views/AlbumDetailView.vue:67`, `frontend/src/views/AlbumDetailView.vue:74`. / 수정: `/users/me/albums/{albumId}` 조회, 404/403 처리, 계획-앨범 관계를 명시.

- [high] 앨범 imageUrl 신뢰로 파일 삭제 path traversal 가능 / create/update DTO의 `imageUrls`가 검증 없이 저장되고, 삭제 시 그 문자열에서 파일명을 뽑아 `resolve()` 후 삭제합니다. 악의적 `../...` 값이 저장되면 album 디렉터리 밖 삭제 위험이 있습니다. 근거: `BE/src/main/java/com/trip/user/dto/AlbumCreateRequest.java:6`, `BE/src/main/java/com/trip/user/service/AlbumService.java:115`, `BE/src/main/java/com/trip/user/service/AlbumStorageService.java:47`, `BE/src/main/java/com/trip/user/service/AlbumStorageService.java:50`. / 수정: 서버 발급 파일명만 저장, normalize 후 baseDir/album 하위인지 검증.

- [high] 알림/공지 API 실패를 seed 데이터로 숨김 / 실제 BE 엔드포인트가 있는데도 알림/공지 실패 시 가짜 알림과 공지를 표시합니다. 401/500/배포 장애가 “읽지 않은 알림 있음”으로 위장됩니다. 근거: `frontend/src/stores/notification.js:116`, `frontend/src/stores/notification.js:121`, `frontend/src/stores/notification.js:129`, `BE/src/main/java/com/trip/notification/controller/NotificationController.java:28`, `BE/src/main/java/com/trip/notice/controller/NoticeController.java:22`. / 수정: seed는 dev flag로 제한하고 prod는 오류/빈 상태를 명시.

- [med] “이달의 챌린지”가 월간이 아니라 누적 장소 수를 셈 / FE는 “이번 달”이라고 설명하지만 BE는 전체 장소 수를 날짜 필터 없이 집계합니다. 근거: `frontend/src/views/ChallengeDetailView.vue:30`, `BE/src/main/java/com/trip/gamification/service/GamificationService.java:30`, `BE/src/main/java/com/trip/plan/repository/PlanRepository.java:40`. / 수정: 이번 달 기간 기준 createdAt/plan date 필터를 쿼리에 반영하거나 문구를 누적으로 변경.

- [med] 뱃지 페이지의 레벨/XP/퀘스트가 전부 하드코딩 / API 데이터는 뱃지 목록에만 쓰고, 레벨·XP·퀘스트 진행도는 정적 값입니다. 실제 사용자 상태와 다르게 표시됩니다. 근거: `frontend/src/views/BadgesView.vue:9`, `frontend/src/views/BadgesView.vue:132`, `frontend/src/views/BadgesView.vue:143`, `frontend/src/views/BadgesView.vue:150`. / 수정: BE DTO를 확장하거나 정적 섹션 제거.

- [med] 알림 link와 단건 읽음 처리가 UI에서 버려짐 / BE와 store는 `link`를 전달하지만 화면은 클릭 이동도 단건 `markRead`도 없습니다. 알림을 눌러도 대상 화면으로 갈 수 없습니다. 근거: `BE/src/main/java/com/trip/notification/dto/NotificationResponse.java:12`, `frontend/src/stores/notification.js:107`, `frontend/src/views/NotificationsView.vue:31`, `frontend/src/views/NotificationsView.vue:42`. / 수정: row 클릭 시 `markRead(id)` 후 `router.push(n.link)`.

- [med] 프로필 수정 입력검증이 FE 힌트뿐임 / FE는 닉네임 2~12자라고 안내하지만 BE DTO/컨트롤러에 `@Valid`, `@Size`, `@Pattern`이 없습니다. bio도 DB 100자 제한만 있고 FE maxlength가 없습니다. 근거: `frontend/src/views/ProfileEditView.vue:39`, `frontend/src/views/ProfileEditView.vue:41`, `BE/src/main/java/com/trip/user/dto/UserUpdateRequestDto.java:3`, `BE/src/main/java/com/trip/user/controller/UserController.java:33`, `BE/src/main/java/com/trip/user/entity/User.java:58`. / 수정: DTO Bean Validation, trim, 서버-클라이언트 동일 규칙 적용.

- [med] 앨범 목록 조회 N+1 / 앨범마다 사진 count와 thumbnail 조회를 별도 쿼리로 실행합니다. 앨범 N개면 최소 `1 + 2N` 쿼리입니다. 근거: `BE/src/main/java/com/trip/user/service/AlbumService.java:31`, `BE/src/main/java/com/trip/user/service/AlbumService.java:33`, `BE/src/main/java/com/trip/user/service/AlbumService.java:35`. / 수정: count/thumbnail projection 쿼리 또는 batch/aggregate 조회.

- [med] 마이페이지 하위 상세 라우트에 인증 가드 누락 / `/mypage`·알림·프로필 수정은 `requiresAuth`인데 챌린지와 앨범 상세는 빠져 있습니다. 특히 앨범 상세는 mock까지 보여줍니다. 근거: `frontend/src/router/index.js:44`, `frontend/src/router/index.js:47`, `frontend/src/router/index.js:48`. / 수정: 두 라우트에 `requiresAuth: true` 추가.

- [low] 챌린지 기간이 6월로 하드코딩 / BE는 현재 월을 계산하지만 화면 조건은 “6월 1일 ~ 6월 30일” 고정입니다. 근거: `frontend/src/views/ChallengeDetailView.vue:65`, `BE/src/main/java/com/trip/gamification/service/GamificationService.java:46`. / 수정: periodStart/periodEnd를 DTO로 내려 렌더링.

- [low] 뱃지 페이지 탭이 상태만 바꾸고 콘텐츠를 바꾸지 않음 / `activeTab`은 class에만 쓰이고 본문은 항상 같은 섹션입니다. 근거: `frontend/src/views/BadgesView.vue:34`, `frontend/src/views/BadgesView.vue:39`, `frontend/src/views/BadgesView.vue:43`. / 수정: 탭별 `v-if` 콘텐츠 구현 또는 탭 제거.

- [low] 앨범 썸네일과 생성 버튼이 죽어 있음 / API에서 `thumbnailUrl`을 매핑하지만 템플릿은 이미지를 렌더링하지 않고, “앨범 만들기” 버튼도 핸들러가 없습니다. 근거: `frontend/src/views/MyPageView.vue:164`, `frontend/src/views/MyPageView.vue:173`, `frontend/src/views/MyPageView.vue:336`. / 수정: 썸네일 `<img>`/fallback 렌더링, 생성 flow 연결.

참고: 이 범위의 pg-mypage 경로에서는 TourAPI/OpenAI/Redis Vector 직접 호출은 발견하지 못했습니다.