#!/usr/bin/env bash
# 전체 워크플로우 페이지별 Codex 적대적 리뷰 (read-only, 병렬). 결과: reviews/codex-<name>.md
set -u
cd "$(dirname "$0")/.."
mkdir -p reviews

run() {
  local name="$1"; local scope="$2"
  codex exec --skip-git-repo-check -s read-only -o "reviews/codex-${name}.md" \
"당신은 매우 비판적인 적대적 코드 리뷰어다. 이 저장소(Triip — Spring Boot BE + Vue3 FE, docker compose 배포)에서 [${name}] 페이지/기능만 집중 리뷰하라.

범위: ${scope}

찾을 것(적대적으로): 실제 버그, NPE/Optional 미처리, 입력검증 누락, 엣지케이스, 동시성/레이스, 트랜잭션 경계·롤백, 외부 API(TourAPI/OpenAI/Redis Vector) 장애·타임아웃·키노출, JPA N+1, 보안(인가우회/IDOR/인젝션/시크릿/CORS), 잘못된 HTTP 상태·에러처리, FE-BE DTO/계약 불일치, mock이 실패를 숨김, 라우터 가드/토큰갱신, 깨진 이미지·하드코딩, 데드코드.

규칙: 추측 금지 — 반드시 file:line 근거. 각 항목 = [심각도 high/med/low] 제목 / 왜 문제인지 / 수정 방향. 심각도순. 한국어로 간결히. **코드 수정 금지, 리뷰만.**" \
    > "reviews/codex-${name}.log" 2>&1 &
  echo "launched: ${name} (pid $!)"
}

# 신규(미리뷰) — 최우선
run rag-assistant "BE com.trip.assistant/**(AssistantConfig,Controller,Service,dto), com.trip.document/**(controller,service,entity,repository,dto), com.trip.rag/**(RagConfig,IngestionService,UserDataIndexer) + FE views/AssistantView.vue,DocumentsView.vue, stores/assistant.js,documents.js. RAG: 사용자격리 필터(userId), 벡터삭제(tagSafe docId), PDF/텍스트 추출, @Tool 함수콜링, 멀티파트 검증, 임베딩 호출 실패처리."

# 페이지별
run pg-home-explore "FE views/HomeView.vue,ExploreView.vue,PlaceDetailView.vue + stores/attraction.js,places.js. 관광지 목록/필터/지도/상세/사진. BE /api/attractions 계약."
run pg-plan "FE views/PlanView.vue,PlanReportView.vue + stores/plan.js. BE /api/plans/**. 일자별 일정/장소 추가·삭제/동선리포트 계약·엣지."
run pg-ai "FE views/AiPlanInputView.vue,AiInputView.vue,AiResultView.vue + stores/recommend.js. BE /api/recommendations/**. 추천 생성/저장/담기."
run pg-community "FE views/CommunityView.vue,PostWriteView.vue,PostDetailView.vue + stores/community.js. BE /community/**. 게시글 CRUD/댓글/좋아요/이미지업로드."
run pg-hotplace "FE views/HotplaceRegisterView.vue,HotplaceDetailView.vue. BE /community/hotplaces/**. 등록/승인/지도."
run pg-companion "FE views/CompanionWriteView.vue,CompanionDetailView.vue,CompanionApplicantsView.vue + stores/companion.js. BE /companion/posts/**. 모집/신청/승인."
run pg-chat "FE views/ChatRoomListView.vue,ChatRoomView.vue + stores/chat.js. BE com.trip.chat/**(STOMP,history,membership). 실시간/참여자/나가기/첨부."
run pg-mypage "FE views/MyPageView.vue,BadgesView.vue,ChallengeDetailView.vue,AlbumDetailView.vue,ProfileEditView.vue,NotificationsView.vue + stores/gamification.js,notification.js. 게임화/앨범/알림."
run pg-auth "FE views/LoginView.vue,SignupView.vue,OAuthCallbackView.vue,PreferenceSurveyView.vue + stores/auth.js. BE /auth/**, users/me. 토큰/OAuth/취향설문."
run pg-misc "FE views/ChecklistView.vue,PaymentView.vue,ConfirmationView.vue. 체크리스트(로컬상태?)/결제데모/확인."

wait
echo "==== ALL CODEX REVIEWS (3) DONE ===="
