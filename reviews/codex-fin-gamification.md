수정 없이 정적 리뷰만 수행했습니다. 근거 없는 추측성 항목은 제외했습니다.

[HIGH] 적립/레벨/퀘스트 진행 동시 업데이트 손실  
왜: `award()`가 기존 행을 조회한 뒤 객체 필드를 `+=`로 갱신합니다. `@Version`, pessimistic lock, atomic update가 없어 같은 사용자에게 이벤트 2개가 동시에 들어오면 마지막 커밋이 이전 커밋을 덮어 points/exp/level/progress가 누락됩니다.  
근거: `GamificationService.java:67-72`, `UserGameStat.java:55-58`, `UserQuestProgress.java:57-66`, `UserGameStatRepository.java:10`, `UserQuestProgressRepository.java:13`  
수정: 사용자 stat/quest 행을 `PESSIMISTIC_WRITE`로 잠그거나 `@Version` + 재시도, 또는 DB atomic increment/update로 바꾸세요.

[HIGH] 보상 멱등키가 없어 좋아요 토글로 EXP 무한 적립 가능  
왜: 보상 입력은 `userId/action/amount`뿐이고 원본 이벤트 ID나 actor/target 키가 없습니다. 게시글/댓글 좋아요는 취소 후 다시 좋아요하면 매번 새 `NotificationEvent`가 발행되고, 수신자는 매번 `COMMUNITY_REACTION_RECEIVED` 보상을 받습니다.  
근거: `NotificationEvent.java:7-13`, `GamificationService.java:62-72`, `CommunityService.java:131-151`, `CommunityService.java:172-192`  
수정: `(action, sourceType, sourceId, actorId, recipientId)` 같은 award ledger 유니크 키를 저장하고 최초 1회만 적립하세요.

[HIGH] 동행 신청 중복 방어가 주석과 다르게 DB에서 보장되지 않음  
왜: 서비스는 `saveAndFlush()` 유니크 충돌을 기대하지만 `CompanionApplication`에는 `(companion_post_id, user_id)` 유니크 제약이 없습니다. 동시 신청이 둘 다 `exists...`를 통과하면 중복 신청과 중복 알림/보상이 발생합니다. 취소는 row 삭제라 재신청 보상 반복도 가능합니다.  
근거: `CompanionService.java:150-166`, `CompanionService.java:170-175`, `CompanionService.java:278`, `CompanionApplication.java:12-13`  
수정: 신청 엔티티에 보상 기준이 되는 멱등 키를 두고, 취소/반려 후 재신청 정책과 보상 정책을 분리하세요.

[MEDIUM] 퀘스트 보상 EXP가 정의만 있고 지급되지 않음  
왜: `QuestCatalog`와 `QuestDto`는 `rewardExp`를 노출하지만 완료 시 `progress/completed/completedAt`만 갱신합니다. 사용자는 퀘스트 완료 보상을 절대 받지 못합니다.  
근거: `QuestCatalog.java:19`, `QuestCatalog.java:25`, `QuestCatalog.java:29`, `GamificationService.java:95-100`, `UserQuestProgress.java:64-67`, `GamificationService.java:235`  
수정: 미완료→완료 전이 시점에 한 번만 `rewardExp`를 stat에 적립하고, 동시 완료 레이스도 같은 트랜잭션/락으로 막으세요.

[MEDIUM] “이번 달 챌린지”가 전체 누적 장소 수로 계산됨  
왜: summary는 `countPlacesByUserId()` 전체 장소 수를 그대로 챌린지 current로 사용합니다. 월 필터가 없어서 과거 장소로 새 달 챌린지가 즉시 완료됩니다. FE는 “이번 달”/“6월 1일~6월 30일”로 표시합니다.  
근거: `GamificationService.java:52`, `GamificationService.java:136`, `PlanRepository.java:49-50`, `GamificationService.java:167-177`, `ChallengeDetailView.vue:30`, `ChallengeDetailView.vue:65`  
수정: 현재 월 범위로 집계하거나 문구를 전체 누적으로 바꾸세요. “여행자 뱃지”도 실제 BadgeCatalog에 추가하거나 보상 문구를 제거해야 합니다.

[MEDIUM] `/badges` 화면이 BE level/quests 계약을 사실상 사용하지 않음  
왜: 화면은 summary를 로드하지만 뱃지만 매핑하고, 레벨/XP/퀘스트는 하드코딩되어 있습니다. `/api/gamification/quests` API는 정의만 있고 이 화면에서 호출되지 않습니다. 실제 적립/레벨 계산과 UI가 불일치합니다.  
근거: `BadgesView.vue:9-19`, `BadgesView.vue:132-139`, `BadgesView.vue:143-198`, `frontend/src/api/index.js:91-93`  
수정: `summary.level`, `summary.stats`, `summary.quests` 또는 `/quests` 응답으로 렌더링하고 하드코딩 데이터를 제거하세요.

[MEDIUM] gamification summary 캐시가 적립 후 무효화되지 않음  
왜: `loaded`가 true면 `load()`가 즉시 반환합니다. MyPage/Badges/Challenge 진입도 `force=false`라 한 번 로드한 뒤 보상이 발생해도 같은 세션에서는 오래된 points/level/badges가 계속 보입니다. 실패도 catch에서 조용히 숨깁니다.  
근거: `frontend/src/stores/gamification.js:14-23`, `BadgesView.vue:123`, `ChallengeDetailView.vue:107`, `MyPageView.vue:572`  
수정: 진입 시 force refresh, TTL, 또는 보상/알림 이벤트 수신 시 gamification store invalidation을 넣으세요.

RECOMMENDATION: REQUEST CHANGES.