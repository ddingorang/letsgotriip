#!/usr/bin/env bash
# 도메인별 Codex 적대적 코드 리뷰 (read-only, 병렬). 결과: reviews/codex-<name>.md
set -u
mkdir -p reviews

run() {
  local name="$1"; local scope="$2"
  codex exec --skip-git-repo-check -s read-only -o "reviews/codex-${name}.md" \
"당신은 매우 비판적인 적대적 코드 리뷰어다. 이 저장소(Triip — Spring Boot BE + Vue3 FE, docker compose 배포)에서 [${name}] 부분만 집중 리뷰하라.

범위: ${scope}

찾을 것(적대적으로): 실제 버그, NPE/Optional 미처리, 입력 검증 누락, 엣지케이스, 동시성/레이스컨디션, 트랜잭션 경계·격리·롤백 문제, JPA N+1, 보안 취약점(인가 우회/IDOR/인젝션/시크릿 노출/CORS·CSRF), 잘못된 HTTP 상태코드·에러처리, FE-BE DTO/계약 불일치, 깨진 이미지/외부의존, 데드코드.

규칙: 추측 금지 — 반드시 file:line 근거. 각 항목 = [심각도 high/med/low] 제목 / 왜 문제인지 / 재현 또는 수정 방향. 심각도순 정렬. 한국어로 간결히. **코드는 수정하지 말고 리뷰만** 한다." \
    > "reviews/codex-${name}.log" 2>&1 &
  echo "launched: ${name} (pid $!)"
}

run companion "BE/src/main/java/com/trip/companion/** , FE frontend/src/views/Companion*.vue, frontend/src/stores/companion.js, 동행 신청/승인/취소/채팅방 연동. (참고: travelDate 등 LocalDate 잘못된 입력이 500 나던 이슈 있었음)"
run chat "BE/src/main/java/com/trip/chat/** , BE/src/main/java/com/trip/global/config/WebSocketBrokerConfig.java, BE/src/main/java/com/trip/global/interceptor/StompSessionInterceptor.java, FE frontend/src/api/stomp.js, frontend/src/stores/chat.js, frontend/src/views/ChatRoom*.vue. STOMP/RabbitMQ 릴레이/MongoDB 영속."
run community "BE/src/main/java/com/trip/community/** , FE frontend/src/views/CommunityView.vue,PostDetailView.vue,PostWriteView.vue,Hotplace*.vue, frontend/src/components/community/*, frontend/src/stores/posts.js,hotplace.js. 이미지 업로드(base64 vs /uploads)·핫플 승인."
run plan-ai "BE/src/main/java/com/trip/plan/** , BE/src/main/java/com/trip/recommend/** , FE frontend/src/views/PlanView.vue,PlanReportView.vue,AiPlanInputView.vue,AiInputView.vue,AiResultView.vue, frontend/src/stores/plan.js,recommend.js. Spring AI/멱등성/낙관락/동선리포트."
run auth-sec "BE/src/main/java/com/trip/user/** , BE/src/main/java/com/trip/global/security/** , BE/src/main/java/com/trip/global/util/JwtUtil.java,CookieUtil.java, BE/src/main/java/com/trip/global/config/SecurityConfig.java, FE frontend/src/stores/auth.js, frontend/src/views/LoginView.vue,SignupView.vue,ProfileEditView.vue,PreferenceSurveyView.vue. JWT/리프레시회전/OAuth2/쿠키/인가규칙."

wait
echo "==== ALL CODEX REVIEWS DONE ===="
