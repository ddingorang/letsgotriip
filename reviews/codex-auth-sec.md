**Findings**

1. [high] 이전 Refresh Token이 새 Refresh Token으로 승격됨  
   근거: `BE/src/main/java/com/trip/user/service/AuthService.java:148-156`, `167-190`, `193-198`  
   문제: overlap window 안의 `prevRtHash`를 허용한 뒤에도 항상 새 refresh token을 발급/저장한다. 탈취된 이전 RT가 5분 안에 사용되면 공격자가 “현재 RT”를 새로 받아 세션을 계속 연장할 수 있다.  
   재현/수정: RT A로 정상 refresh 후, 이전 RT A를 같은 `sessionId`로 재호출하면 새 RT가 내려간다. `prevRtHash` 경로는 새 RT 발급 금지, Redis Lua/CAS로 read-compare-write 원자화, 재사용 1회성/idempotent 처리 필요.

2. [high] 회원 탈퇴 후 다른 기기 세션으로 계속 refresh 가능  
   근거: `BE/src/main/java/com/trip/user/service/UserService.java:82-100`, `BE/src/main/java/com/trip/user/service/AuthService.java:136-138`, `172-190`, `BE/src/main/java/com/trip/user/service/AlbumService.java:53-58`  
   문제: 탈퇴는 현재 `sessionId`만 삭제하고, refresh는 Redis 세션만 믿고 DB `status`를 확인하지 않는다. 다른 기기의 refresh cookie는 최대 7일 동안 access token 재발급 가능하다. 일부 user 기능은 `findById`만 사용해 inactive 사용자도 통과한다.  
   재현/수정: A/B 두 브라우저 로그인 → A에서 탈퇴 → B에서 `/auth/refresh`. userId→sessionId 역색인으로 전체 세션 삭제, refresh 시 active user DB 검증 필요.

3. [high] Docker Compose 배포가 인증 쿠키를 `Secure=false`로 강제  
   근거: `BE/src/main/java/com/trip/global/util/CookieUtil.java:42-49`, `BE/src/main/resources/application.yaml:101-103`, `docker-compose.yml:101`  
   문제: `refreshToken`/`sessionId`가 HttpOnly여도 `Secure=false`면 HTTPS 환경에서도 HTTP 전송 가능 쿠키가 된다. compose 배포값이 코드의 운영 기본값을 무력화한다.  
   재현/수정: compose로 실행 시 Set-Cookie에 Secure가 없다. 운영 compose는 `COOKIE_SECURE=true`, HTTPS 강제, prod에서 false면 부팅 실패 처리.

4. [high] 실제 시크릿 값이 작업트리 `.env`에 존재  
   근거: `BE/.env:7-11`  
   문제: JWT secret, 외부 API key, Google client secret이 실제 값으로 들어 있다. 보고서에는 값을 반복하지 않지만, 현재 워크트리 공유/압축/로그 노출만으로 유출된다.  
   재현/수정: 해당 라인 열람 가능. 즉시 키 회전, 로컬 `.env` 제거/재발급, secret manager 또는 배포 환경변수만 사용.

5. [med] 로그인 API가 사용자 존재 여부를 상태코드로 노출  
   근거: `BE/src/main/java/com/trip/user/service/AuthService.java:64-68`, `BE/src/main/java/com/trip/global/error/ResponseCode.java:36-37`, `frontend/src/views/LoginView.vue:149-151`  
   문제: FE는 같은 메시지를 보여도 API는 없는 이메일 `404`, 비밀번호 오류 `401`로 구분된다. 이메일 enumeration 가능.  
   재현/수정: `/auth/login`에 존재/비존재 이메일로 직접 호출. 인증 실패는 동일한 401/code/message로 통일.

6. [med] FE에 Kakao 로그인 버튼이 있지만 BE OAuth는 Google만 지원  
   근거: `frontend/src/views/LoginView.vue:86`, `165`, `BE/src/main/java/com/trip/global/security/oauth2/OAuth2Attribute.java:21-27`, `BE/src/main/resources/application.yaml:9-18`  
   문제: Kakao 버튼은 `/oauth2/authorization/kakao`로 보내지만 설정/attribute mapper는 google만 있다. 실제 로그인 플로우가 깨진다.  
   재현/수정: Kakao 버튼 클릭. Kakao registration/attribute mapper를 추가하거나 버튼 제거.

7. [med] OAuth 속성 null 처리 없음  
   근거: `BE/src/main/java/com/trip/global/security/oauth2/OAuth2Attribute.java:30-39`, `42-51`, `BE/src/main/java/com/trip/user/entity/User.java:54`, `106-117`  
   문제: `name/email/picture/oauthKey` 검증 없이 `Map.of`에 넣는다. `Map.of`는 null 불가이고, `profileImageUrl`도 nullable=false라 OAuth 응답 일부가 빠지면 NPE/DB 예외로 실패한다.  
   재현/수정: picture/name 없는 OAuth 응답. 필수 속성 검증 후 `OAuth2AuthenticationException`, 기본 프로필 이미지 fallback.

8. [med] 프로필/취향 수정 DTO 검증 부재로 DB 제약 위반 가능  
   근거: `BE/src/main/java/com/trip/user/controller/UserController.java:30-44`, `BE/src/main/java/com/trip/user/dto/UserUpdateRequestDto.java:3-7`, `BE/src/main/java/com/trip/user/dto/PreferenceUpdateRequestDto.java:10-13`, `BE/src/main/java/com/trip/user/entity/User.java:41-66`  
   문제: `@Valid`도 DTO 제약도 없다. 긴 nickname/bio/interests/companion 또는 임의 관심사 key가 DB 길이/도메인 계약을 깨며 400 대신 500/데이터 오염 가능.  
   재현/수정: `PATCH /users/me`에 1KB nickname, `/users/me/preferences`에 긴 리스트 전송. DTO 제약과 enum/whitelist 검증 추가.

9. [med] 공개 업로드가 클라이언트 MIME과 원본 확장자만 신뢰  
   근거: `BE/src/main/java/com/trip/user/service/AlbumStorageService.java:22`, `31-37`, `63-70`, `BE/src/main/java/com/trip/global/config/SecurityConfig.java:65`  
   문제: `Content-Type`은 클라이언트가 조작 가능하고 확장자는 원본 파일명에서 온다. 공개 `/uploads/**`에 HTML/스크립트성 파일이 저장/서빙될 여지가 있다.  
   재현/수정: `Content-Type: image/png`, 파일명 `x.html`로 업로드 시 `.html` 저장 가능. magic byte 검증, 서버가 확장자 결정, 이미지 재인코딩 권장.

10. [low] 앨범 목록 조회 N+1 쿼리  
   근거: `BE/src/main/java/com/trip/user/service/AlbumService.java:30-37`  
   문제: 앨범마다 `countByAlbumId`와 `findAllByAlbumId...`를 호출한다. 앨범 N개면 최소 1+2N 쿼리.  
   재현/수정: 앨범 여러 개 보유 후 `/users/me/albums`. count/thumbnail projection 쿼리나 fetch/join 집계로 통합.

수정은 하지 않았고, 요청대로 읽기 전용 리뷰만 수행했다.