코드는 수정하지 않았고, 요청 범위 중심으로 근거 있는 결함만 정리합니다.

1. `[high] 401 refresh single-flight가 탭 간 레이스를 막지 못함`
   - 근거: `frontend/src/api/http.js:47`, `frontend/src/api/http.js:61`, `BE/src/main/java/com/trip/user/service/AuthService.java:128`, `BE/src/main/java/com/trip/user/service/AuthService.java:159`
   - 왜 문제인지: `refreshPromise`는 브라우저 탭별 JS 메모리라 다중 탭 동시 401을 막지 못한다. BE도 refresh를 원자적으로 CAS 처리하지 않는다고 주석상 인정되어 있어, 두 탭이 같은 refresh token을 동시에 회전시키면 마지막 Redis write만 남고 다른 탭의 새 쿠키가 다음 refresh에서 재사용 탐지로 세션 삭제를 유발할 수 있다.
   - 재현/수정: 같은 계정 2개 탭에서 access token 만료 후 보호 API 동시 호출. BE Lua/CAS로 refresh 회전을 원자화하거나 FE에 `BroadcastChannel`/storage-lock 기반 cross-tab single-flight를 추가.

2. `[high] VITE_API_BASE_URL 운영 배포에서 silent refresh가 다른 origin으로 나감`
   - 근거: `frontend/src/api/http.js:31`, `frontend/src/stores/auth.js:29`, `frontend/.env.example:6`
   - 왜 문제인지: 일반 API는 `http`의 `baseURL`을 타지만, `auth.refresh()`는 raw `axios.post('/auth/refresh')`를 써서 `VITE_API_BASE_URL`을 무시한다. 정적 FE와 BE origin을 분리한 운영 배포에서는 `/auth/refresh`가 FE origin으로 나가 404/프록시 누락이 된다.
   - 재현/수정: `VITE_API_BASE_URL=https://api.example.com` 빌드 후 새로고침 또는 401 발생. refresh도 같은 `http` 인스턴스 또는 동일 baseURL 설정을 쓰되 refresh endpoint는 인터셉터 재진입 제외.

3. `[high] docker-compose가 기본 인프라 계정을 호스트에 노출함`
   - 근거: `docker-compose.yml:18`, `docker-compose.yml:20`, `docker-compose.yml:48`, `docker-compose.yml:50`, `docker-compose.yml:83`, `docker-compose.yml:89`
   - 왜 문제인지: MySQL `root/password`, RabbitMQ `guest/guest`가 그대로 설정되고 MySQL/RabbitMQ 관리 UI/STOMP 포트가 호스트에 열린다. 이 compose를 서버에 올리면 외부에서 기본 계정으로 접근 가능하다.
   - 재현/수정: `localhost:13306` MySQL root 접속 또는 `localhost:15672` RabbitMQ guest 로그인. 비밀번호를 `.env` 강제값으로 바꾸고 운영 compose에서는 DB/RabbitMQ 포트 host publish 제거.

4. `[med] 업로드 파일이 컨테이너 재생성 시 유실됨`
   - 근거: `BE/src/main/resources/application.yaml:105`, `BE/src/main/resources/application.yaml:107`, `BE/src/main/java/com/trip/global/config/WebConfig.java:44`, `frontend/nginx.conf:59`, `docker-compose.yml:64`, `docker-compose.yml:121`
   - 왜 문제인지: BE는 `/uploads/**`를 컨테이너 로컬 `uploads` 디렉터리에서 서빙하고 nginx는 이를 프록시하지만, backend 서비스에는 업로드 볼륨이 없다. DB 볼륨만 정의되어 이미지 파일은 재빌드/재생성 때 사라진다.
   - 재현/수정: 이미지 업로드 후 `docker compose up -d --build --force-recreate backend`; 기존 `/uploads/...` URL 404. backend에 `uploads:/app/uploads` 같은 볼륨을 추가하거나 오브젝트 스토리지 사용.

5. `[med] 채팅 라우트가 인증 가드를 우회함`
   - 근거: `frontend/src/router/index.js:36`, `frontend/src/router/index.js:37`, `frontend/src/views/ChatRoomView.vue:133`, `frontend/src/stores/chat.js:57`, `frontend/src/stores/chat.js:78`, `BE/src/main/java/com/trip/global/config/SecurityConfig.java:70`
   - 왜 문제인지: `/chat`, `/chat/:id`는 `requiresAuth`가 없지만 화면은 보호 REST 히스토리와 access token 기반 STOMP 연결을 바로 시도한다. 비로그인 사용자는 로그인으로 보내지지 않고 빈/오류 채팅 화면에 머문다.
   - 재현/수정: 로그아웃 상태에서 `/chat/1` 직접 진입. 두 라우트에 `requiresAuth: true` 추가.

6. `[med] 설문 라우트가 인증 없이 열리고 저장 실패를 숨김`
   - 근거: `frontend/src/router/index.js:53`, `frontend/src/views/PreferenceSurveyView.vue:127`, `frontend/src/views/PreferenceSurveyView.vue:133`, `BE/src/main/java/com/trip/user/controller/UserController.java:40`
   - 왜 문제인지: `/survey`는 보호되지 않지만 저장은 `/users/me/preferences` 인증 API다. 실패를 catch에서 무시하고 `/home`으로 이동해 사용자는 저장된 줄 알 수 있다.
   - 재현/수정: 로그아웃 상태에서 `/survey` 완료. 라우트에 `requiresAuth: true`를 붙이거나 비로그인 설문은 로컬 임시 저장 후 로그인 후 동기화.

7. `[med] 인터셉터가 로그인 실패 401에도 refresh를 시도함`
   - 근거: `frontend/src/api/http.js:54`, `frontend/src/api/http.js:61`, `frontend/src/stores/auth.js:55`, `BE/src/main/java/com/trip/global/error/ResponseCode.java:36`, `BE/src/main/java/com/trip/user/service/AuthService.java:68`
   - 왜 문제인지: 모든 401을 refresh 대상으로 처리한다. 비밀번호 오류도 401이므로 로그인 실패 때 `/auth/refresh`를 추가 호출하고, 원래 로그인 오류를 refresh 오류로 덮을 수 있다.
   - 재현/수정: 잘못된 비밀번호 로그인. `/auth/login`, `/auth/signup`, `/auth/refresh` 등 auth endpoint는 refresh retry 대상에서 제외.

8. `[med] 이미지 업로드 계약을 쓰지 않고 base64를 게시글 JSON에 넣음`
   - 근거: `BE/src/main/java/com/trip/community/controller/CommunityController.java:34`, `BE/src/main/java/com/trip/community/controller/CommunityController.java:39`, `frontend/src/views/PostWriteView.vue:119`, `frontend/src/views/PostWriteView.vue:149`, `frontend/src/api/index.js:96`
   - 왜 문제인지: BE는 multipart `/community/images` 업로드 후 `imageUrl`을 반환하는 계약을 제공하지만 FE wrapper에는 업로드 함수가 없고, 작성 화면은 FileReader data URL을 `imageUrls`에 직접 넣는다. 큰 이미지는 JSON payload/DB를 비대하게 만들고 업로드 파일 서빙 경로와도 어긋난다.
   - 재현/수정: 게시글 작성에서 이미지 선택 후 payload 확인. 먼저 multipart 업로드 API를 호출해 반환 URL만 `createPost`에 전달.

9. `[low] api/index.js가 실제 BE 엔드포인트 전체를 감싸지 못함`
   - 근거: `frontend/src/api/index.js:58`, `frontend/src/api/index.js:111`, `BE/src/main/java/com/trip/plan/controller/PlanController.java:55`, `BE/src/main/java/com/trip/companion/controller/CompanionController.java:71`, `BE/src/main/java/com/trip/companion/controller/CompanionController.java:112`
   - 왜 문제인지: `route-report`, companion 수정/삭제/마감/신청자 승인·거절·취소 등이 wrapper에 없어 stores가 직접 `http.*`를 호출한다. prefix rewrite나 에러 정책이 바뀔 때 래퍼와 직접 호출이 쉽게 드리프트한다.
   - 재현/수정: `rg "http\\.(get|post|patch|put|delete)" frontend/src`로 직접 호출 확인. BE 컨트롤러 기준으로 wrapper를 완성하고 store는 wrapper만 사용.