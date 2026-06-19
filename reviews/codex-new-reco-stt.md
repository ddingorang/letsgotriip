검토 기준: 현재 HEAD, 지정 범위만. 코드 수정 없음.

1. [high] Whisper 호출 타임아웃 없음 / `WhisperSTTManager`는 주입된 `RestClient`로 동기 호출하지만 `RestClientConfig`는 timeout을 전혀 설정하지 않습니다. 근거: `BE/src/main/java/com/trip/preprocessing/client/impl/WhisperSTTManager.java:33`, `:48-54`, `BE/src/main/java/com/trip/global/config/RestClientConfig.java:17-20`. 게다가 `PreprocessingService.uploadAndProcess()`는 `@Transactional` 안에서 DB 저장 후 STT를 호출합니다. 근거: `PreprocessingService.java:42`, `:60-62`, `:72`. 외부 API 지연 시 요청 스레드와 DB 트랜잭션이 오래 묶입니다. / Whisper 전용 `RestClient`에 connect/read timeout을 두고, 외부 STT 호출은 DB 트랜잭션 밖에서 수행하세요.

2. [high] 원본 PII 파일이 마스킹 없이 디스크에 남음 / 업로드 원본을 `temp_uploads`에 저장하고 경로까지 DB에 넣은 뒤, 마스킹은 `rawText`에만 적용합니다. 근거: `PreprocessingService.java:49-57`, `:69-80`. 성공/실패 어느 경로에도 파일 삭제가 없어 주민번호·전화번호·이메일이 원본 파일로 계속 남습니다. / 처리 후 `finally`에서 원본을 삭제하거나, 영구 보관이 필요하면 암호화·수명관리·접근통제를 적용하세요.

3. [high] 서버 측 multipart 검증 부재 / 컨트롤러는 `MultipartFile`만 받고, 서비스는 빈 파일·확장자·MIME·매직바이트·원본 파일명 검증 없이 바로 저장/처리합니다. 근거: `PreprocessingController.java:24-38`, `PreprocessingService.java:49-51`. 전역 제한도 20MB뿐입니다. 근거: `BE/src/main/resources/application.yaml:7-10`. 임의 파일이 Whisper로 전달되거나 빈 카카오 txt가 성공 저장될 수 있습니다. / 엔드포인트별 allowlist, `file.isEmpty()`, 크기, 파일명 길이/문자, 실제 포맷 검증을 추가하고 400/413으로 거절하세요.

4. [med] STT 사용자 입력 오류가 500으로 반환됨 / Whisper 빈 응답은 `_INTERNAL_SERVER_ERROR`로 던지고, 모든 RestClient 예외도 500으로 래핑합니다. 근거: `WhisperSTTManager.java:61-70`, `PreprocessingService.java:75-78`, `ResponseCode.java:29`. 무음/비음성/지원불가 포맷 같은 입력 문제와 외부 API 장애가 모두 서버 오류로 보입니다. / 로컬 검증 실패는 400/422, OpenAI/Whisper 장애는 502/503 계열로 분리하세요.

5. [med] 저장 취향 fallback이 캐시 해시에 반영되지 않음 / 캐시 해시는 원 요청의 `req.themes()`로 먼저 계산됩니다. 근거: `RecommendService.java:126-132`, `:540-550`. 저장 취향은 그 뒤 `resolveThemes()`에서 프롬프트에만 반영됩니다. 근거: `RecommendService.java:166-169`, `:411-418`. 사용자가 온보딩 취향을 바꿔도 5분 내 같은 요청은 예전 취향 결과를 캐시로 받을 수 있습니다. / fallback된 `effectiveThemes` 또는 사용자 취향 버전/스냅샷을 hash와 requestJson에 포함하세요.

6. [med] 온보딩 취향 key와 추천 테마 매핑 불일치 / 온보딩은 `nature`, `cafe`, `night`, `resort`, `festival` 등을 저장합니다. 근거: `frontend/src/views/PreferenceSurveyView.vue:75-107`, `UserService.java:61-65`. 그런데 추천 매핑은 `sea`, `mountain`, `food`, `history`, `activity`, `shopping`만 처리하고 나머지는 원문 그대로 프롬프트에 넣습니다. 근거: `RecommendService.java:75-82`, `:426-428`. 저장 취향 반영이 상당 부분 깨집니다. / 공통 enum/매핑 테이블을 만들고 저장·추천 양쪽에서 같은 key만 허용하세요.

7. [med] PII 정규식 오탐·누락 큼 / 전화번호는 하이픈/무구분만 처리하고 공백·점·`+82`·일반 유선번호를 놓칩니다. 주민번호는 경계·날짜·성별자리 검증 없이 13자리 숫자를 마스킹합니다. 근거: `PreprocessingService.java:35-40`, `:92-94`. 긴 주문번호/식별자 일부가 주민번호처럼 마스킹되거나 실제 전화번호가 남을 수 있습니다. / lookaround 경계, 구분자 정규화, RRN 날짜/체크 검증, 케이스별 단위 테스트를 추가하세요.