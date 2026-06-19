[CRITICAL] 공개 API로 임의 계정 비밀번호 재설정 가능  
왜: `/auth/**`가 공개(`SecurityConfig.java:49`)이고, 재설정 요청이 이메일만으로 토큰을 생성해 응답에 그대로 반환합니다(`AuthService.java:219-238`). 이후 확정 API는 토큰+새 비밀번호만 검증하고 바로 비밀번호를 바꿉니다(`AuthService.java:248-260`). FE도 토큰을 화면에 노출/복사합니다(`PasswordResetView.vue:67-78`).  
수정: 토큰을 응답하지 말고 검증된 이메일 채널로만 전달. 데모 노출은 non-prod/admin 전용으로 차단. 토큰은 해시 저장, 동일 응답, rate limit 적용.

[HIGH] 재설정 토큰 single-use 레이스  
왜: `findByToken()` 후 `isUsed()` 검사, 비밀번호 변경, `markUsed()`가 분리되어 있고(`AuthService.java:248-260`), 저장소에는 락이 없습니다(`PasswordResetTokenRepository.java:10`). 엔티티도 `@Version`이 없습니다(`PasswordResetToken.java:21-35`). 동시 요청 2개가 모두 `used=false`를 읽으면 둘 다 성공할 수 있습니다.  
수정: `PESSIMISTIC_WRITE` 조회 또는 `UPDATE ... WHERE token=? AND used=false AND expires_at>now` 조건부 갱신으로 1건 성공만 허용.

[HIGH] 비밀번호 재설정은 BE에서 최소 길이 정책이 빠짐  
왜: 회원가입 비밀번호는 `@Size(min=8,max=60)`이 있는데(`SignupRequestDto.java:18-19`), 재설정 DTO는 `@NotBlank`뿐입니다(`PasswordResetConfirm.java:8-12`). FE만 8자 검사를 합니다(`PasswordResetView.vue:202`). 직접 API 호출로 1자 비밀번호 설정 가능.  
수정: 재설정 DTO에도 동일한 서버 측 비밀번호 정책을 적용.

[MEDIUM] 프로필 PATCH가 업로드 매직바이트 검증을 우회함  
왜: 업로드 경로는 `FileStorageService.validate()`로 매직바이트를 봅니다(`FileStorageService.java:57-70`). 하지만 `/users/me` PATCH는 `profileImageUrl` 문자열을 그대로 받아(`UserUpdateRequestDto.java:3-6`) 엔티티에 저장합니다(`User.java:127-132`). FE는 그 값을 그대로 `<img :src>`에 씁니다(`ProfileEditView.vue:19`).  
수정: 일반 프로필 PATCH에서 `profileImageUrl` 제거 또는 서버가 발급한 업로드 URL/기본 URL만 허용.

[MEDIUM] OAuth 프로필 이미지 null이면 NPE 또는 NOT NULL 실패  
왜: Google `picture`를 그대로 읽고(`OAuth2Attribute.java:34`), 신규 사용자 생성에 그대로 전달합니다(`CustomOAuth2UserService.java:59-65`). `User.ofOAuth()`는 기본 이미지 fallback 없이 `profileImageUrl`을 저장합니다(`User.java:106-117`)인데 컬럼은 `nullable=false`입니다(`User.java:54-55`). 또한 `Map.of(... "picture", picture ...)`는 null 값을 받으면 NPE입니다(`OAuth2Attribute.java:42-50`).  
수정: OAuth picture가 null/blank면 `DEFAULT_PROFILE_IMAGE_URL`로 정규화하고 nullable 값은 `Map.of`에 넣지 않기.

[MEDIUM] 프로필 이미지 FE-BE 계약/연결 불일치  
왜: FE API 주석은 업로드 응답을 `{ imageUrl }`로 적었지만(`frontend/src/api/index.js:22-27`), BE는 `UserProfileResponseDto`를 반환합니다(`UserController.java:42-47`). 실제 프로필 편집 화면의 사진 버튼에는 파일 input/click 연결이 없고(`ProfileEditView.vue:22-28`), 저장은 PATCH만 호출합니다(`ProfileEditView.vue:90-94`).  
수정: 업로드 버튼을 실제 multipart 호출에 연결하고, 응답 타입을 BE와 FE 중 한쪽으로 통일.

리뷰만 수행했고 수정은 하지 않았습니다.