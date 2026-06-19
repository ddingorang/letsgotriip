리뷰 결과: 수정 없이 정적 코드만 확인했습니다.

1. [high] 비밀번호 재설정 요청이 계정 존재 여부와 토큰을 노출함  
근거: `AuthService.java:222-238`, `SecurityConfig.java:49`, `PasswordResetView.vue:179-184`  
문제: 존재하지 않는 이메일은 `token/expiresAt=null`, 존재하는 이메일은 실제 UUID 토큰을 반환합니다. 공개 엔드포인트라 API 응답만으로 계정 열거가 가능하고, reset token도 그대로 탈취 표면에 노출됩니다.  
수정: 모든 요청에 동일한 응답을 반환하고 토큰은 이메일로만 전달. 데모 토큰 노출은 dev profile/admin-only로 격리. DB에는 토큰 원문 대신 해시 저장.

2. [high] 비밀번호 재설정 토큰 단일 사용 보장이 동시성에서 깨짐  
근거: `AuthService.java:248-260`, `PasswordResetTokenRepository.java:10`, `PasswordResetToken.java:34-52`  
문제: `findByToken` 후 `used` 확인, 비밀번호 변경, `markUsed`가 락 없이 진행됩니다. 같은 토큰으로 병렬 요청 2개가 들어오면 둘 다 `used=false`를 통과할 수 있습니다.  
수정: `used=false AND expiresAt>now` 조건부 원자 update, pessimistic lock, 또는 `@Version` 기반 낙관락으로 한 요청만 성공하게 처리.

3. [high] 기본 설정으로 알려진 관리자 계정이 생성됨  
근거: `application.yaml:126-127`, `DataSeeder.java:35`, `DataSeeder.java:85-89`  
문제: `SEED_ENABLED` 기본값이 `true`이고 `admin@triip.com / admin1234`가 생성됩니다. `hasRole("ADMIN")` 자체는 맞지만, 기본 배포에서 공지 CRUD 권한이 공개된 자격증명으로 뚫립니다.  
수정: seed 기본값 false, 관리자 초기 비밀번호 env/secret 주입, 최초 로그인 강제 변경, 운영 profile에서 seeder 비활성화.

4. [med] reset 경로의 서버 비밀번호 정책 누락  
근거: `PasswordResetConfirm.java:8-12`, `AuthService.java:259`, `SignupRequestDto.java:18-19`, `PasswordResetView.vue:202-204`  
문제: FE는 8자 이상을 검사하지만 서버 DTO는 `@NotBlank`뿐입니다. 직접 API 호출로 1자 비밀번호 설정이 가능합니다.  
수정: signup과 동일한 서버 검증을 reset DTO에도 적용하고, 가능하면 공통 password policy validator로 통합.

5. [med] 비밀번호 변경 후 기존 세션/refresh token이 유지됨  
근거: `AuthService.java:111`, `AuthService.java:259-260`  
문제: 로그인 세션은 Redis에 저장되지만, reset 완료 시 사용자 기존 세션 삭제나 JWT 무효화가 없습니다. 탈취된 기존 refresh/access token은 비밀번호 변경 후에도 계속 유효합니다.  
수정: 사용자별 refresh 세션 추적 후 reset 시 전부 폐기, 또는 `passwordChangedAt/tokenVersion` 클레임으로 기존 access token 거부.

6. [med] Notice 입력 길이 검증 누락으로 400 대신 500 가능  
근거: `NoticeCreateRequest.java:8-16`, `NoticeUpdateRequest.java:8-16`, `Notice.java:25-29`, `GlobalExceptionHandler.java:165-180`  
문제: DTO는 `@NotBlank`만 있고 entity는 `category length=20`, `title length=200`입니다. 초과 입력이 DB 제약에서 터지면 전역 핸들러의 catch-all로 500이 됩니다.  
수정: DTO에 `@Size(max=20/200)` 추가, category enum/allowlist 검증, `DataIntegrityViolationException`을 400/409로 매핑.

7. [med] reset-request가 무제한 토큰을 발급하고 기존 토큰을 유지함  
근거: `SecurityConfig.java:49`, `AuthService.java:227-236`, `PasswordResetTokenRepository.java:10`  
문제: 공개 엔드포인트에서 요청마다 새 토큰을 저장하며 이전 미사용 토큰을 폐기하지 않습니다. 계정별 다수 활성 토큰과 DB 누적/스팸이 가능합니다.  
수정: IP/email rate limit, 사용자별 기존 미사용 토큰 폐기, 만료 토큰 cleanup job 추가.

8. [low] FE-BE invalid reset token 에러 코드 계약 불일치  
근거: `AuthService.java:248-253`, `ResponseCode.java:30`, `PasswordResetView.vue:216-220`  
문제: BE는 무효/만료 토큰에 code `"400"`을 반환하지만 FE는 `USER404`/`USER401`만 토큰 오류 메시지로 처리합니다. 실제 invalid token은 일반 오류로 표시됩니다.  
수정: FE가 `"400"` 또는 전용 `PASSWORD_RESET_TOKEN_INVALID` 코드를 처리하게 맞추기.

9. [low] Notice 생성/삭제 HTTP 상태가 어색함  
근거: `NoticeController.java:41-45`, `NoticeController.java:58-61`  
문제: 생성은 200, 삭제도 200입니다. 클라이언트 계약상 큰 장애는 아니지만 REST 의미상 생성은 201, 삭제는 204가 맞습니다.  
수정: create는 `201 Created`와 Location, delete는 `204 No Content`.

참고: `hasRole("ADMIN")` 매처와 JWT 권한 부여는 `SecurityConfig.java:63-65`, `JwtAuthenticationFilter.java:37-38` 기준으로 정합성 문제가 보이지 않았습니다.