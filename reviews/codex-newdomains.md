범위 내 코드만 정적 리뷰했고, 수정은 하지 않았습니다.

1. [high] 이달 챌린지가 전체 누적 장소 수로 달성됨  
   근거: `BE/src/main/java/com/trip/gamification/service/GamificationService.java:30`, `:37`, `:42-52`, `BE/src/main/java/com/trip/plan/repository/PlanRepository.java:40-41`  
   왜 문제: 화면/DTO는 “이번 달” 챌린지인데, 쿼리는 사용자 전체 장소 수를 카운트합니다. 과거 계획 10곳만 있어도 이번 달 챌린지가 100%가 됩니다.  
   재현/수정: 지난달 계획에 장소 10개 저장 후 `/api/gamification/summary` 호출. `TripPlan.startDate/endDate` 또는 장소 추가 시점 기준 월 필터를 쿼리에 넣으세요.

2. [high] 알림/공지 API 실패를 가짜 시드 데이터로 위장함  
   근거: `frontend/src/stores/notification.js:113-130`  
   왜 문제: `/api/notifications`가 401/500/네트워크 오류여도 실제 오류 대신 로컬 시드 알림과 공지가 표시됩니다. 인증 실패나 백엔드 장애를 정상 데이터처럼 숨깁니다.  
   재현/수정: 백엔드 중지 또는 토큰 만료 후 알림 페이지 진입. seed fallback은 개발 전용으로 분리하고, 운영 흐름에서는 오류/빈 상태를 명확히 표시하세요.

3. [med] “모두 읽음”이 서버 실패 전 로컬 상태부터 변경하고 실패를 삼킴  
   근거: `frontend/src/stores/notification.js:140-148`  
   왜 문제: PATCH 실패 시 서버 unread는 그대로인데 UI만 모두 읽음으로 바뀝니다. 새로고침 전까지 사용자는 잘못된 상태를 봅니다.  
   재현/수정: `/api/notifications/read-all`을 500/401로 실패시키고 클릭. API 성공 후 로컬 반영하거나 실패 시 rollback/error 처리하세요.

4. [med] unread-count API가 있는데 FE는 최신 50개 목록만 세서 배지를 표시함  
   근거: `BE/src/main/java/com/trip/notification/repository/NotificationRepository.java:14-16`, `frontend/src/stores/notification.js:86-88`, `frontend/src/views/NotificationsView.vue:22`  
   왜 문제: unread가 51개 이상이면 목록은 top50만 가져오므로 배지 카운트가 실제보다 작습니다.  
   재현/수정: unread 알림 51개 생성 후 알림 페이지 확인. `notificationApi.unreadCount()`를 별도 호출해 배지를 서버 카운트로 표시하세요.

5. [med] 챌린지 상세 라우트가 인증 가드에서 빠짐  
   근거: `frontend/src/router/index.js:43`, `BE/src/main/java/com/trip/gamification/controller/GamificationController.java:23-26`, `frontend/src/views/ChallengeDetailView.vue:104-107`  
   왜 문제: 화면은 인증 API를 호출하지만 `/mypage/challenge`에는 `requiresAuth`가 없어 비로그인 사용자가 들어와 401을 조용히 삼킨 빈/기본 화면을 봅니다.  
   재현/수정: 로그아웃 상태에서 `/mypage/challenge` 직접 진입. 라우트에 `meta: { tabBar: false, requiresAuth: true }` 추가.

6. [med] AFTER_COMMIT 알림 적재 실패가 영구 유실됨  
   근거: `BE/src/main/java/com/trip/notification/event/NotificationEventListener.java:21-29`, `BE/src/main/java/com/trip/notification/service/NotificationService.java:27-36`  
   왜 문제: 원본 트랜잭션 커밋 후 알림 저장이 실패하면 예외를 로그만 남기고 삼킵니다. 재시도/아웃박스가 없어 알림은 복구 불가입니다.  
   재현/수정: 원본 도메인 커밋 직후 notifications insert 실패 유도. outbox 테이블, 재시도 큐, 실패 이벤트 저장 중 하나가 필요합니다.

7. [med] NoticeSeeder가 시드 토글/트랜잭션 없이 항상 실행됨  
   근거: `BE/src/main/java/com/trip/notice/config/NoticeSeeder.java:15-25`, `:27-46`, `BE/src/main/resources/application.yaml:108-110`  
   왜 문제: `SEED_ENABLED` 설정이 있는데 NoticeSeeder에는 조건부 실행이 없습니다. 또 `run()`이 트랜잭션이 아니라 중간 실패 시 일부 공지만 저장되고 다음 기동 때 `count() > 0` 때문에 복구되지 않습니다.  
   재현/수정: 두 번째/세 번째 save 실패 유도 후 재기동. `@ConditionalOnProperty`와 단일 트랜잭션/업서트 키를 적용하세요.

8. [low] 알림 딥링크 계약이 FE에서 무시됨  
   근거: `BE/src/main/java/com/trip/notification/dto/NotificationResponse.java:12`, `frontend/src/stores/notification.js:107`, `frontend/src/views/NotificationsView.vue:31-45`  
   왜 문제: BE가 `link`를 내려주고 store도 매핑하지만, 알림 row 클릭 동작이 없어 사용자가 대상 글/동행으로 이동할 수 없습니다.  
   재현/수정: `link` 있는 알림 생성 후 클릭. row/button 클릭 시 `router.push(n.link)` 처리.

테스트는 실행하지 않았고, 요청대로 정적 코드 리뷰만 수행했습니다.