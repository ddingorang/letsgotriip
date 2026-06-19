검토 결과, 코드 수정 없이 읽기 전용 리뷰만 했습니다. 실제 근거가 있는 항목만 심각도순으로 정리합니다.

1. [high] 실패한 로그인 401이 기존 쿠키 세션을 되살림 / `frontend/src/api/http.js:54-73`, `frontend/src/stores/auth.js:28-33`, `frontend/src/stores/auth.js:55` / `/auth/login`도 공용 인터셉터를 타므로 비밀번호 오류 401 때 refresh 쿠키가 남아 있으면 `refresh()`가 성공해 store에 accessToken을 세팅한다. 원 로그인은 다시 실패해도 사용자는 인증 상태가 될 수 있다. / `/auth/login`, `/auth/signup`, `/auth/refresh` 같은 auth endpoint는 401 refresh 인터셉터 대상에서 제외.

2. [high] refresh token 회전이 원자적이지 않아 정상 세션도 재사용 공격으로 삭제될 수 있음 / `BE/src/main/java/com/trip/user/service/AuthService.java:128-130`, `BE/src/main/java/com/trip/user/service/AuthService.java:136-190`, `frontend/src/api/http.js:47-67` / Redis read-compare-write가 분리돼 있고 FE single-flight는 탭 단위뿐이다. 두 탭/요청이 같은 RT로 동시에 refresh하면 마지막 Redis write와 마지막 Set-Cookie 응답 순서가 어긋나 다음 refresh가 `SESSION_REUSE_DETECTED`로 세션 삭제될 수 있다. / Redis Lua CAS, WATCH/MULTI, 세션 버전 등으로 회전을 원자화.

3. [high] 재사용 탐지로 Redis 세션을 삭제해도 access token은 계속 통과 / `BE/src/main/java/com/trip/user/service/AuthService.java:159-163`, `BE/src/main/java/com/trip/global/security/JwtAuthenticationFilter.java:30-41`, `BE/src/main/java/com/trip/global/util/JwtUtil.java:74` / 공격 탐지 후 Redis 세션은 지우지만 필터는 JWT 서명만 검증하고 `familyId/currentAccessJti`를 Redis와 대조하지 않는다. 탈취된 access token은 만료까지 계속 API 접근 가능하다. / reuse 감지 세션의 access jti/familyId denylist 또는 Redis 세션 대조를 추가.

4. [high] 카카오 로그인 버튼은 실제로 동작하지 않음 / `frontend/src/views/LoginView.vue:86`, `frontend/src/views/LoginView.vue:166`, `BE/src/main/resources/application.yaml:15-24`, `BE/src/main/java/com/trip/global/security/oauth2/OAuth2Attribute.java:21-27` / FE는 `/oauth2/authorization/kakao`로 보내지만 BE OAuth registration과 attribute mapper는 Google만 지원한다. / 카카오 registration/provider/attribute mapper를 추가하거나 버튼 제거.

5. [high] 운영 분리 배포에서 silent refresh/OAuth callback만 잘못된 origin으로 호출됨 / `frontend/src/api/http.js:29-32`, `frontend/src/stores/auth.js:29`, `frontend/.env.production:15`, `frontend/src/views/OAuthCallbackView.vue:31` / 일반 API는 `VITE_API_BASE_URL`을 쓰지만 `auth.refresh()`는 raw `axios.post('/auth/refresh')`라 FE 정적 호스트로 나갈 수 있다. / refresh도 공용 baseURL 인스턴스를 쓰고 refresh 요청은 인터셉터 재진입만 막기.

6. [high] compose 배포가 데모/관리자 계정과 로그인 프리필을 같이 켬 / `docker-compose.yml:117`, `docker-compose.yml:132-134`, `BE/src/main/java/com/trip/global/config/DataSeeder.java:46`, `BE/src/main/java/com/trip/global/config/DataSeeder.java:79`, `BE/src/main/java/com/trip/global/config/DataSeeder.java:88-89`, `frontend/src/views/LoginView.vue:126-128` / compose 기본값으로 seed가 켜지고 FE 로그인 폼에 데모 계정이 박힌다. 관리자 기본 계정도 생성된다. / 배포 compose는 `SEED_ENABLED=false`, 데모 프리필 제거, 관리자 생성은 별도 일회성 절차로 분리.

7. [high] 인증 쿠키 Secure가 배포 compose에서도 꺼져 있음 / `BE/src/main/java/com/trip/global/util/CookieUtil.java:43-49`, `BE/src/main/resources/application.yaml:120`, `docker-compose.yml:115` / refresh/session 쿠키가 HttpOnly여도 `Secure=false`면 HTTPS 운영에서도 HTTP 전송 가능 쿠키가 된다. / prod profile/compose에서 `COOKIE_SECURE=true` 강제, HTTP 접근 차단.

8. [high] 실제 시크릿이 작업공간 env 파일에 평문 존재 / `.env:2-6`, `BE/.env:7-11`, `docker-compose.yml:109-113` / JWT/OpenAI/Tour/Google 값이 평문 파일에 있다. 값은 재기재하지 않지만 유출 전제로 봐야 한다. / 키 즉시 회전, env 파일 저장소/공유 금지, secret manager 또는 배포 환경 변수로만 주입.

9. [med] OAuth 기본/compose URL이 localhost에 고정돼 원격 compose 배포에서 깨짐 / `BE/src/main/resources/application.yaml:21`, `BE/src/main/resources/application.yaml:114-117`, `docker-compose.yml:120-122` / Google redirect URI와 FE callback/login/CORS 값이 localhost 기본값이다. 서버에 compose로 올리면 사용자 브라우저가 자기 PC의 localhost로 돌아갈 수 있다. / `OAUTH_REDIRECT_URI`, `FE_*`, CORS origin을 배포 도메인 필수 env로 강제.

10. [med] 취향설문은 인증 가드가 없고 저장 실패를 성공처럼 삼킴 / `frontend/src/router/index.js:57`, `frontend/src/router/index.js:81-83`, `frontend/src/views/PreferenceSurveyView.vue:127-135`, `BE/src/main/java/com/trip/user/controller/UserController.java:40-45` / 비로그인도 `/survey`에 들어가고, `/users/me/preferences` 실패를 무시한 채 `/home`으로 이동한다. 사용자는 저장됐다고 오인한다. / `/survey`에 `requiresAuth`, 저장 실패 표시/재시도 처리.

11. [med] 취향설문 DTO 검증이 없어 임의 값·긴 값으로 DB 오류/오염 가능 / `BE/src/main/java/com/trip/user/dto/PreferenceUpdateRequestDto.java:10-12`, `BE/src/main/java/com/trip/user/service/UserService.java:61-67`, `BE/src/main/java/com/trip/user/entity/User.java:62`, `BE/src/main/java/com/trip/user/entity/User.java:66` / 관심사/동행 값 allowlist와 길이 제한이 없다. 긴 배열은 255 컬럼 초과로 500이 날 수 있다. / DTO에 size/pattern/allowlist 검증 추가.

12. [med] 가입 성공 후 자동 로그인 실패를 “가입 실패”로 표시 / `frontend/src/views/SignupView.vue:169-183`, `BE/src/main/java/com/trip/user/service/AuthService.java:47-48`, `BE/src/main/java/com/trip/user/service/AuthService.java:102` / 회원 생성은 성공했는데 Redis/로그인만 실패하면 사용자에게 가입 오류가 뜨고 재시도 시 이미 가입된 계정이 된다. / signup 성공과 auto-login 실패를 분리해 로그인 화면으로 안내.

13. [med] 로그인 API가 계정 존재 여부를 상태코드로 노출 / `BE/src/main/java/com/trip/user/service/AuthService.java:64-71`, `BE/src/main/java/com/trip/global/error/ResponseCode.java:36-38`, `frontend/src/views/LoginView.vue:149-152` / FE는 같은 메시지로 숨기지만 API는 없는 이메일 404, 비번 오류 401을 구분한다. / 인증 실패는 동일 401/동일 코드/동일 지연으로 통일.

14. [med] OAuth 사용자 정보 null 처리 누락으로 NPE/DB 오류 가능 / `BE/src/main/java/com/trip/global/security/oauth2/OAuth2Attribute.java:31-39`, `BE/src/main/java/com/trip/global/security/oauth2/OAuth2Attribute.java:42-51`, `BE/src/main/java/com/trip/user/entity/User.java:54-55`, `BE/src/main/java/com/trip/user/entity/User.java:106-117` / `picture/name/email` null 가능성을 허용한 뒤 `Map.of(...)`와 nullable=false `profileImageUrl`에 그대로 넣는다. / null 필드 기본값 적용, 필수 claim 검증, `Map.of` 대신 null-safe map 구성.

15. [low] FE와 BE 회원가입 닉네임 검증 계약 불일치 / `frontend/src/views/SignupView.vue:58`, `frontend/src/views/SignupView.vue:137-138`, `BE/src/main/java/com/trip/user/dto/SignupRequestDto.java:9-11` / FE는 2~20자라고 안내하지만 BE는 `max=20`만 검증해 1자 닉네임을 API로 직접 가입시킬 수 있다. / BE DTO에도 `@Size(min=2,max=20)` 적용.