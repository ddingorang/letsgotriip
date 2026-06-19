#!/usr/bin/env bash
# 신규 구현 도메인 Codex 적대적 리뷰 (read-only, 병렬). 결과: reviews/codex-new-<name>.md
set -u
cd "$(dirname "$0")/.."
mkdir -p reviews

run() {
  local name="$1"; local scope="$2"
  codex exec --skip-git-repo-check -s read-only -o "reviews/codex-new-${name}.md" \
"당신은 매우 비판적인 적대적 코드 리뷰어다. 이 저장소(Triip — Spring Boot 3.5 BE + Vue3 FE, docker compose)에서 방금 새로 구현된 [${name}] 부분만 집중 리뷰하라.

범위: ${scope}

찾을 것(적대적으로): 실제 버그, NPE/Optional 미처리, 입력검증 누락, 엣지케이스, 동시성/레이스, 트랜잭션 경계·롤백·@Transactional 누락, 외부 API(Open-Meteo/OpenAI/Whisper) 장애·타임아웃·키노출, JPA N+1·LAZY 초기화, 보안(인가우회/IDOR/소유자검증 누락/인젝션/시크릿/CORS/공개돼선 안되는 엔드포인트), 잘못된 HTTP 상태(특히 400이어야 하는데 500), FE-BE DTO/계약 불일치, 멀티파트/파일검증, mock이 실패를 숨김, 라우터 가드, 데드코드.

규칙: 추측 금지 — 반드시 file:line 근거. 각 항목 = [심각도 high/med/low] 제목 / 왜 문제인지 / 수정 방향. 심각도순. 한국어로 간결히. **코드 수정 금지, 리뷰만.**" \
    > "reviews/codex-new-${name}.log" 2>&1 &
  echo "launched: ${name} (pid $!)"
}

run checklist "BE com.trip.checklist/**(entity ChecklistItem, repository, service ChecklistService+ChecklistTemplates, controller, dto) + FE frontend/src/views/ChecklistView.vue. 소유자검증·템플릿적용·plan연결·정렬."
run context "BE com.trip.context/**(client OpenMeteoClient[RestClient 타임아웃], service WeatherService/EvStationService/NewsService, controller, dto) + FE frontend/src/views/HomeView.vue,PlaceDetailView.vue(contextApi 위젯). 외부 Open-Meteo 호출 실패/타임아웃/좌표검증·공개 엔드포인트 적정성."
run plan-extras "BE com.trip.plan: TripPlan(shareToken), PlanRepository.findByShareToken, PlanService(createShare/getShared/compare/getBudget), PlanController(share/shared/compare/budget), dto(PlanShareResponseDto/PlanCompareResponseDto/PlanBudgetResponseDto) + FE frontend/src/views/PlanSharedView.vue,PlanView.vue,PlanReportView.vue. 공유토큰 추측가능성/공개조회 IDOR/예산추정 정확성."
run auth-notice "BE com.trip.user(AuthController/AuthService 비밀번호재설정, PasswordResetToken entity+repo, dto, User.updatePassword) + com.trip.notice(NoticeController/NoticeService admin CRUD, dto) + FE frontend/src/views/PasswordResetView.vue. 토큰만료/재사용/계정열거/ADMIN 권한강제(SecurityConfig hasRole)·비번정책."
run reco-stt "BE com.trip.recommend.service.RecommendService(저장 취향 반영 변경분) + com.trip.preprocessing/**(STT 실패→성공 버그수정, PII 마스킹 정규식). 취향 로딩 NPE/캐시해시 영향, STT 실패판정·마스킹 정규식 오탐/누락·트랜잭션."
run story-group "BE com.trip.story/**(TravelStory CRUD) + com.trip.group/**(TravelGroup/GroupMember, 생성/가입/탈퇴/멤버/단체할인) + FE frontend/src/views/StoriesView.vue,GroupsView.vue. 소유자검증·정원/중복가입 레이스·그룹 GET 공개여부·멤버권한."
run notif-sttfe "BE 알림확장 — com.trip.community.service.CommunityService(toggleLike 알림), com.trip.companion.service.CompanionService(approve/reject 알림) + FE frontend/src/views/AnalysisUploadView.vue(STT/카톡 업로드). 알림 수신자 정확성/본인제외/이벤트 트랜잭션, 업로드 파일타입·크기검증·진행상태."

wait
echo "==== ALL CODEX NEW-DOMAIN REVIEWS DONE ===="
