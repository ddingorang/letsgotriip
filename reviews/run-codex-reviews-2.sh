#!/usr/bin/env bash
# 나머지 전 영역 Codex 적대적 리뷰 (read-only, 병렬). 결과: reviews/codex-<name>.md
set -u
mkdir -p reviews

run() {
  local name="$1"; local scope="$2"
  codex exec --skip-git-repo-check -s read-only -o "reviews/codex-${name}.md" \
"당신은 매우 비판적인 적대적 코드 리뷰어다. 이 저장소(Triip — Spring Boot BE + Vue3 FE, docker compose 배포)에서 [${name}] 부분만 집중 리뷰하라.

범위: ${scope}

찾을 것(적대적으로): 실제 버그, NPE/Optional 미처리, 입력 검증 누락, 엣지케이스, 동시성/레이스컨디션, 트랜잭션 경계·격리·롤백 문제, 배치/스케줄러 중복실행·실패처리, 외부 API(TourAPI/OpenAI) 장애·타임아웃·키 노출, JPA N+1, 보안 취약점(인가 우회/IDOR/인젝션/시크릿·CORS·CSRF), 잘못된 HTTP 상태·에러처리, FE-BE DTO/계약 불일치, 라우터 가드/인터셉터/토큰갱신 결함, 깨진 이미지/외부의존, 데드코드, docker/배포 설정 결함.

규칙: 추측 금지 — 반드시 file:line 근거. 각 항목 = [심각도 high/med/low] 제목 / 왜 문제인지 / 재현 또는 수정 방향. 심각도순 정렬. 한국어로 간결히. **코드는 수정하지 말고 리뷰만** 한다." \
    > "reviews/codex-${name}.log" 2>&1 &
  echo "launched: ${name} (pid $!)"
}

run attraction "BE/src/main/java/com/trip/attraction/** (client=TourAPI, service, controller, dto, entity, repository) + FE frontend/src/views/ExploreView.vue,PlaceDetailView.vue,HomeView.vue(관광지부분), frontend/src/stores/attraction.js,places.js. TourAPI 호출/캐시/스냅샷/좌표."
run festival "BE/src/main/java/com/trip/festival/** (batch=FestivalSyncJobConfig/Reader/Processor/Writer, scheduler, client=TourApiClient, service, controller, dto, entity, repository) + FE frontend/src/api/festival.js, frontend/src/stores/festival.js. 배치 멱등성/중복/스케줄러/지역코드체계/만료정리."
run newdomains "BE/src/main/java/com/trip/notice/** , BE/src/main/java/com/trip/notification/** (event/listener 포함) , BE/src/main/java/com/trip/gamification/** + FE frontend/src/views/NotificationsView.vue,BadgesView.vue,ChallengeDetailView.vue, frontend/src/stores/notification.js,gamification.js. 신규 도메인 인가·이벤트 처리·시더."
run preproc-global "BE/src/main/java/com/trip/preprocessing/** (STT/전처리) , BE/src/main/java/com/trip/global/config/** (WebConfig, Redis/RabbitMQ/Mongo/Batch/AI config, WebSocketBrokerConfig 제외-이미리뷰됨) , BE/src/main/java/com/trip/global/error/** , BE/src/main/java/com/trip/global/interceptor/** , BE/src/main/java/com/trip/global/util/**(JwtUtil/CookieUtil 제외-이미리뷰됨). 설정/예외/인터셉터/유틸 결함, 인프라 빈 구성."
run fe-infra "frontend/src/main.js, frontend/src/router/index.js(전체 가드/메타), frontend/src/api/http.js(axios 인터셉터·401 refresh single-flight), frontend/src/api/index.js(전체 엔드포인트 래퍼), frontend/vite.config.js, frontend/nginx.conf, frontend/Dockerfile, BE/Dockerfile, docker-compose.yml, frontend/src/components/common/*. 라우팅/토큰갱신/프록시/배포 결함."
run fe-views "frontend/src/views/HomeView.vue, ChecklistView.vue, PaymentView.vue, ConfirmationView.vue, AlbumDetailView.vue, OAuthCallbackView.vue, PreferenceSurveyView.vue, MyPageView.vue. (이미 리뷰된 Companion/Chat/Community/Post/Plan/Ai/Login/Signup/Hotplace/Explore/PlaceDetail 제외) 각 화면 데이터소스·mock·계약 불일치·하드코딩."

wait
echo "==== ALL CODEX REVIEWS (2) DONE ===="
