#!/usr/bin/env bash
# 최종: 최근 웨이브 신규 코드 Codex 적대적 리뷰 (read-only, 병렬). 결과: reviews/codex-fin-<name>.md
set -u
cd "$(dirname "$0")/.."
mkdir -p reviews
run() {
  local name="$1"; local scope="$2"
  codex exec --skip-git-repo-check -s read-only -o "reviews/codex-fin-${name}.md" \
"당신은 매우 비판적인 적대적 코드 리뷰어다. Triip(Spring Boot 3.5 BE + Vue3 FE, docker). 최근 구현된 [${name}]만 집중 리뷰.

범위: ${scope}

찾을 것: 실제 버그, NPE/Optional, 입력검증 누락, 동시성/레이스, 트랜잭션 경계·이벤트리스너 AFTER_COMMIT 정합, SSE 리소스 누수(emitter 미정리)·스레드 안전, 외부 LLM/TourAPI 실패·타임아웃, JPA N+1·LAZY, 보안(인가우회/IDOR/소유자검증 누락/공개 부적절), 멱등성(즐겨찾기/리뷰/적립 중복), 잘못된 HTTP 상태(400 vs 500), FE-BE 계약 불일치, mock 폴백이 실패 은폐, 라우터 가드, 데드코드.
규칙: 추측 금지 file:line 근거. [심각도] 제목/왜/수정. 심각도순. 한국어 간결. **리뷰만, 수정 금지.**" \
    > "reviews/codex-fin-${name}.log" 2>&1 &
  echo "launched: ${name} (pid $!)"
}
run gamification "BE com.trip.gamification/**(UserGameStat/EarnedBadge/UserQuestProgress 엔티티·리포지토리, GamificationService 적립 award/레벨계산, GamificationEventListener(NotificationEvent 소비), QuestCatalog, controller summary/quests). 적립 멱등성·동시성·이벤트 중복·레벨계산."
run fav-review "BE com.trip.favorite/** + com.trip.review/**(엔티티/리포지토리/서비스/컨트롤러/dto). 1인1리뷰·소유자검증·즐겨찾기 토글 멱등·unique 제약·평균계산."
run search-sse "BE com.trip.search/**(통합검색, 타도메인 리포지토리 주입) + com.trip.notification/**(SseEmitterRegistry, /stream 컨트롤러, NotificationEventListener push). SSE emitter 누수/스레드안전/타임아웃, 검색 외부호출 실패처리·상한."
run stt-pref-doc "BE com.trip.preprocessing/**(STT→취향 추출 ChatClient 호출·저장) + com.trip.document/**(오디오 STT 업로드·전사·색인, 매직바이트). LLM 실패격리·트랜잭션·취향 병합·전사 실패판정."
run assistant-chat "BE com.trip.assistant/**(SSE chatStream Flux, @Tool getMyTravelPlans/createChecklistFromText, 인젝션가드) + com.trip.chat/**(설정·강퇴·초대·방장위임·이미지, StompSessionInterceptor SUBSCRIBE 인가). 스트리밍 자원/타임아웃·툴 부작용·채팅 인가."
run profile-user "BE com.trip.user/**(프로필 이미지 업로드, 비밀번호 재설정, PasswordResetToken) + FileStorageService(매직바이트). 업로드 검증·토큰·NOT NULL."
run fe-new1 "FE frontend/src/views/PlaceDetailView.vue,CompanionApplicantsView.vue,CompanionDetailView.vue,BadgesView.vue,NotificationsView.vue,SearchView.vue,AdminView.vue + stores/companion.js,notification.js. 리뷰/찜/수락거절/게임화실데이터/SSE구독해제/검색/admin 계약·에러처리."
run fe-new2 "FE frontend/src/views/ChatRoomView.vue,ProfileEditView.vue,ExploreView.vue,HomeView.vue,MyPageView.vue + stores/attraction.js,hotplace.js,places.js,posts.js,festival.js + api/index.js,router/index.js. 채팅/프로필이미지·축제연결·mock폴백 정직화·SSE fetch 헬퍼·라우트/가드 정합."
wait
echo "==== ALL FINAL CODEX REVIEWS DONE ===="
