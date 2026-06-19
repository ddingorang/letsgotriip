# 신규 코드 행위 QA (Triip)

대상: STT/PII, 추천 취향, 알림 확장, Plan 예산/비교, 날씨, 체크리스트, 비밀번호 재설정
방식: 코드 정독 + PII 정규식 실측(JVM 실행). 소스 미수정(read-only).

---

## 🔴 HIGH — 실제 동작 결함

### 1. PII 이메일 정규식 ReDoS (서비스 행 멈춤 가능)
- 파일: `BE/src/main/java/com/trip/preprocessing/service/PreprocessingService.java:40` (`EMAIL_PATTERN`)
- 패턴: `[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}`
- 문제: `[A-Za-z0-9.-]+` 와 뒤따르는 `\.` 가 `.` 문자에서 겹쳐 **2차(quadratic) 백트래킹** 발생.
- 실측(JVM): 입력 `a@a.a.a.…` (`a.` 반복)에 대해
  - n=10,000 → 1,442ms / n=20,000 → 5,865ms / n=40,000 → 27,927ms (입력 2배 → 시간 ≈ 4배, 명백한 2차 폭증).
- 공격 벡터: `maskPii`는 **사용자 업로드 KakaoTalk 파일 전문**(`Files.readString`)에 적용됨. 업로드 한도는 `application.yaml`의 `max-file-size: 20MB`. 20MB 텍스트로 `x@y.` 류 패턴을 채우면 요청 스레드가 수 분~사실상 무한 점유 → DoS.
- 재현 입력: 약 1~20MB 크기의 `aaaa@` + `a.` 수십만~수백만 회 반복 텍스트를 `POST /analysis/upload/kakao` 로 업로드.
- 수정 방향(참고): 소유 그룹화 `[A-Za-z0-9_%+\-]+@[A-Za-z0-9\-]+(?:\.[A-Za-z0-9\-]+)*\.[A-Za-z]{2,}` 또는 길이 상한/possessive quantifier 적용.

---

## 🟢 WORKS — 주장대로 동작 확인

### 2. STT 실패/빈 전사 → 성공 저장 안 됨 + 마스킹 선적용
- `WhisperSTTManager.java:56-70`: text 비었거나 호출 실패 시 가짜 성공 문자열 반환 없이 `GeneralException`(RuntimeException) throw. ✔
- `PreprocessingService.java:75-78`: STT 결과 null/blank 시에도 throw. `uploadAndProcess`가 `@Transactional`이고 `GeneralException extends RuntimeException`(확인됨)이라 **초기 `save(analysisData)` 포함 전체 롤백** → 빈 결과가 성공으로 영속되지 않음. ✔ (엔티티에 status 필드 자체가 없으므로 "FAILED 마킹"은 비대상, 미영속으로 충족)
- `PreprocessingService.java:80`: `maskPii(sttResult)` 결과를 `updateRawText`로 세팅 후 커밋 → **마스킹이 영속 전에 적용**됨. KakaoTalk 경로(line 70)도 동일. ✔
- PHONE `01[016789]-?\d{3,4}-?\d{4}` 실측: `010-1234-5678`, `01012345678`, `010-123-4567`, `011-1234-5678` 모두 마스킹. ✔
- RRN `\d{6}-?\d{7}` 실측: `900101-1234567`, 하이픈 없는 13자리 모두 마스킹. RRN을 PHONE보다 먼저 치환(line 92-94)해 충돌 없음. ✔
  - 경미: 16자리 연속숫자가 앞 13자리만 `[주민번호]`로 과치환되어 뒤 3자리 잔존(`…456`) — PII 보호 관점에선 무해(과마스킹).

### 3. 추천 취향(요청 테마 빈 경우 저장 취향 사용)
- `RecommendService.java:407-419` `resolveThemes`: `requestThemes` 비면 `userRepository.findById(userId)`(주입·사용 확인, line 108) → `User.getPreferredInterests`(User.java:63 존재 확인) 콤마 분리, null/blank/빈요소 안전 처리, 둘 다 없으면 `List.of()`. ✔
- 캐시 해시: `serializeRequest`(line 549)는 `req.themes()` 원본만 사용 → resolveThemes가 해시에 영향 없음. ✔
- 트랜잭션: `process`는 `@Transactional(NOT_SUPPORTED)`. `findById`는 트랜잭션 밖 단발 조회로 깨짐 없음. ✔

### 4. 알림 확장
- 좋아요(`CommunityService.java:144-151`): 수신자=글 작성자, `liked && !author.equals(userId)`로 본인 제외, `@Transactional` 메서드 내부 publish → AFTER_COMMIT 정상 발화. (댓글 좋아요 184-192, 댓글 작성 210-217도 동일하게 적정)
- 동행 수락(`CompanionService.java:234-239`)·반려(`253-258`): 수신자=신청자(`application.getApplicant().getId()`), 신청(`170-175`)은 작성자에게. 모두 `@Transactional` 내부 publish. ✔
- 리스너 `NotificationEventListener.java:21` `@TransactionalEventListener(AFTER_COMMIT)` + 예외 흡수, `NotificationService.create:29` `recipientId==null` 가드 & REQUIRES_NEW. author/applicant는 생성 시 항상 세팅되어 NPE 위험 없음. ✔

### 5. Plan 예산/비교/공유 (IDOR)
- `getBudget`(151-176): 소유 검증(`verifyOwner`), 장소 0개면 stream 합계 0(NPE 없음), `TripPlace.attraction`은 `nullable=false`(엔티티 확인)라 `getAttraction()` 비null, `estimatePlaceCost(Integer)`가 null contentType→10000 폴백. ✔
- `compare`(135-141): a·b **둘 다** `verifyOwner` → IDOR 차단. ✔
- `getShared`(122-126): 소유 검증 없음(공개 의도), `createShare`/`compare`/`getBudget`/`getRouteReport`는 소유 검증 있음. ✔

### 6. 컨텍스트 날씨
- `OpenMeteoClient.java:33-43`: `@PostConstruct`에서 `SimpleClientHttpRequestFactory` connect 4s/read 5s 설정 후 전용 RestClient 빌드 → **타임아웃 실적용**. ✔
- 부분 JSON 방어: `toWeatherResponse`에서 `current`/`daily`/리스트 null 체크, `get(list,i)` 경계 검사로 NPE/IOOBE 없음. raw null → 예외 변환. ✔
- 좌표 검증: `WeatherService.java:28` lat[-90,90]/lng[-180,180] 범위 밖 `_BAD_REQUEST`. ✔

### 7. 체크리스트
- `update`/`toggle`/`delete`(52,60,68): 모두 `findByIdAndUserId(id, userId)` 선검증, 미소유 `_FORBIDDEN`. ✔
- `applyTemplate`(81): `ChecklistTemplates.byKey(templateKey)` 없으면 `_BAD_REQUEST` → 템플릿 키 검증됨. ✔
- (`create`는 본인 userId로만 생성하므로 소유 이슈 없음)

### 8. 비밀번호 재설정
- `resetPassword`(245-261): 토큰 미존재→`_BAD_REQUEST`, `isUsed()` 또는 `isExpired(now)`→`_BAD_REQUEST`, `user.updatePassword(encode(...))`(User.java:149 존재) + `markUsed()`를 `@Transactional` 내에서 dirty-check 영속. ✔
- `requestPasswordReset`(217-239): 이메일 미존재 시에도 token/expiresAt=null 동일 응답 → **계정 열거 방지**. 만료 30분, used=false 초기화. ✔

---

## 결론
- 실제 BUG 1건(HIGH): PII 이메일 정규식 ReDoS.
- 나머지 6개 영역(STT/마스킹·추천 취향·알림·Plan·날씨·체크리스트·비번 재설정)은 주장대로 동작.
