[high] `AlbumDetailView`가 실제 앨범 API를 전혀 쓰지 않습니다.  
`MyPageView.vue:120`은 완료 일정의 `plan.id`로 `/mypage/album/${plan.id}`에 보내지만, BE 상세는 `albumId`를 받습니다(`AlbumController.java:43-48`). 상세 화면은 로컬 배열과 fallback만 사용합니다(`AlbumDetailView.vue:67-74`), 실제 `photos[].imageUrl` DTO는 무시됩니다(`AlbumResponse.java:9-12`, `AlbumPhotoResponse.java:6-9`). 결과: 실제 앨범/사진이 안 보이고, 잘못된 id면 제주 목업이 표시됩니다.

[high] 프로필 사진 변경 UI가 죽어 있습니다.  
`ProfileEditView.vue:22`, `:28` 버튼에 클릭/input 핸들러가 없고 저장은 `PATCH /users/me`만 합니다(`ProfileEditView.vue:90-95`). 업로드 래퍼는 존재하지만 미사용입니다(`frontend/src/api/index.js:21-27`). 게다가 주석은 `{ imageUrl }`라 하지만 BE는 전체 `UserProfileResponseDto`를 반환합니다(`UserController.java:42-47`). 연결해도 `data.imageUrl` 기대 시 undefined가 납니다.

[high] `BadgesView`는 게임화 실데이터를 대부분 버리고 하드코딩합니다.  
레벨/XP는 고정 `LV 7`, `1,660 XP`입니다(`BadgesView.vue:9`, `:18-19`). 통계와 퀘스트도 고정 배열입니다(`BadgesView.vue:143-150`). BE는 `level`, `stats`, `quests`를 내려줍니다(`GamificationSummaryDto.java:9-14`, `QuestDto.java:6-16`)이고 `gamificationApi.quests()`도 있는데(`frontend/src/api/index.js:91-93`) 화면은 `gamiStore.load()` summary만 호출합니다(`BadgesView.vue:123`).

[med] `BadgesView` 퀘스트 shape가 BE DTO와 맞지 않습니다.  
템플릿은 `quest.id`, `progressVal`, `progressWidth`, `rewards`를 기대합니다(`BadgesView.vue:51`, `:69-78`), BE `QuestDto`는 `code`, `percent`, `rewardExp`, `progressText`입니다(`QuestDto.java:6-16`). 현재는 하드코딩으로 가려져 있지만 실데이터로 바꾸면 그대로 깨집니다.

[med] 챌린지/앨범 상세 라우트 인증 가드가 없습니다.  
`/mypage/challenge`, `/mypage/album/:id`는 `requiresAuth`가 빠져 있습니다(`router/index.js:60-61`). BE gamification/album은 인증 필요입니다(`GamificationController.java:28-37`, `AlbumController.java:36-48`, `SecurityConfig.java:92`). 직접 진입 시 챌린지는 0 fallback, 앨범은 목업 공개 화면이 됩니다.

[med] 게임화 summary 캐시가 갱신되지 않습니다.  
`gamificationStore.load()`는 한 번 `loaded=true`면 force 없이는 재호출하지 않습니다(`gamification.js:14-22`). MyPage/Badges/Challenge 모두 force 없이 호출합니다(`MyPageView.vue:570-574`, `BadgesView.vue:123`, `ChallengeDetailView.vue:107`). 계획/장소/보상 변경 후 같은 세션에서 오래된 레벨/뱃지가 보일 수 있습니다.

[med] “이달의 챌린지”가 실제로는 전체 누적 장소 수입니다.  
BE summary는 `countPlacesByUserId` 전체 누적 places로 challenge를 만듭니다(`GamificationService.java:189-205`, `:222-232`). 화면은 “이번 달 계획에 새로운 장소”라고 표시합니다(`ChallengeDetailView.vue:29-30`). 문구와 산식이 달라 사용자가 진행도가 이상하다고 느낍니다.

[med] 마이페이지 앨범 목록도 썸네일을 버립니다.  
목록 매핑은 `thumbnailUrl`을 저장하지만(`MyPageView.vue:489-494`), 템플릿은 그라데이션 박스/카운트/라벨만 렌더링합니다(`MyPageView.vue:173-179`). BE는 썸네일을 명시 제공합니다(`AlbumSummaryResponse.java:8-13`).

[med] 관리자 진입 게이팅이 라우터/화면에는 없습니다.  
마이페이지 메뉴는 `userRole === 'ADMIN'`일 때만 보이지만(`MyPageView.vue:414`, `:315-318`), `/admin` 라우트는 인증만 요구합니다(`router/index.js:90`). `AdminView`도 role 확인 없이 바로 pending API를 호출합니다(`AdminView.vue:63-68`, `:118`). 비관리자는 화면 진입 후 서버 403만 봅니다(`HotPlaceService.java:163-166`).

[low] `BadgesView` 탭은 상태만 바뀌고 콘텐츠를 필터링하지 않습니다.  
탭 클릭은 `activeTab`만 바꿉니다(`BadgesView.vue:34-40`, `:125-126`), 실제 콘텐츠는 항상 퀘스트와 뱃지 전체가 렌더링됩니다(`BadgesView.vue:43-88`). “완료 기록” 탭도 별도 화면이 없습니다.

[low] 기본 프로필 이미지가 일부 화면에서 깨질 수 있습니다.  
BE 기본값은 `/images/default-profile.png`입니다(`User.java:85-86`). Home은 이 값을 필터링합니다(`HomeView.vue:145-150`)인데 MyPage/ProfileEdit은 그대로 `<img>`에 넣습니다(`MyPageView.vue:26`, `ProfileEditView.vue:19`). BE 정적 리소스는 `/uploads/**`만 설정되어 있습니다(`WebConfig.java:42-45`).

[low] 앨범/공유/사진 추가 버튼이 장식입니다.  
마이페이지 “앨범 만들기”에 핸들러가 없습니다(`MyPageView.vue:164`). 상세의 “추가”, “링크 복사”, “커뮤니티 공유”도 클릭 동작이 없습니다(`AlbumDetailView.vue:25`, `:45`, `:51`).

코드만 읽었고 수정/서버 실행/테스트는 하지 않았습니다.