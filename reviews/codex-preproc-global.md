코드는 수정하지 않고 지정 범위 중심으로 리뷰만 했습니다. 심각도순입니다.

1. [high] STOMP 채팅방 인가 우회  
   근거: `BE/src/main/java/com/trip/global/interceptor/StompSessionInterceptor.java:29-40`, `BE/src/main/java/com/trip/chat/controller/ChatStompController.java:32-39`, `BE/src/main/java/com/trip/chat/service/ChatService.java:53-54`, 비교 근거 `ChatHistoryController.java:45-48`  
   왜 문제인지: CONNECT 때 JWT만 검증하고 SUBSCRIBE/SEND 대상 방 멤버십은 검사하지 않습니다. REST 히스토리는 멤버십을 검사하지만, STOMP는 임의 `/topic/chat.room.{id}` 구독/`/pub/chat.message.{id}` 송신이 가능합니다.  
   재현/수정: 로그인한 임의 사용자로 피해 roomId에 SUBSCRIBE/SEND. `StompSessionInterceptor` 또는 메시지 핸들러에서 roomId 멤버십과 destination/body roomId 일치를 검증하세요.

2. [high] 기본 실행 시 고정 관리자 계정 생성  
   근거: `BE/src/main/java/com/trip/global/config/DataSeeder.java:35`, `DataSeeder.java:85-89`, `BE/src/main/resources/application.yaml:109-110`, `docker-compose.yml:103`  
   왜 문제인지: `SEED_ENABLED=true` 기본 compose 실행으로 `admin@triip.com / admin1234` 관리자 계정이 생성됩니다. 운영/공유 환경에 올라가면 즉시 권한 탈취 경로입니다.  
   재현/수정: 기본 compose 부팅 후 해당 계정 로그인. seed 기본값을 false로 두고 관리자 생성은 환경별 수동/일회성 절차로 분리하세요.

3. [high] STT 실패가 성공 데이터로 저장됨  
   근거: `BE/src/main/java/com/trip/preprocessing/client/impl/WhisperSTTManager.java:62-65`, `BE/src/main/java/com/trip/preprocessing/service/PreprocessingService.java:61-62`, `BE/src/main/java/com/trip/preprocessing/entity/UserAnalysisData.java:35`, `PreprocessingController.java:38-39`  
   왜 문제인지: Whisper 호출 실패를 예외로 던지지 않고 `"STT 변환 실패: ..."` 문자열로 반환해 `rawText`에 저장하고 HTTP 200/dataId를 반환합니다.  
   재현/수정: OpenAI 키 오류 또는 5xx 유도 후 음성 업로드. 실패는 502/503 계열 예외로 전파하거나 실패 상태 컬럼으로 분리하세요.

4. [high] 실제 시크릿 파일이 워크트리에 존재  
   근거: `.env:2-7`, `BE/.env:8`, `docker-compose.yml:95-99`  
   왜 문제인지: JWT/OpenAI/Tour/Google/Kakao 계열 값이 평문 env 파일에 설정되어 있고 compose가 그대로 주입합니다. 값은 보고서에 적지 않았지만, 작업공간 접근자에게 노출됩니다.  
   재현/수정: 해당 env 파일 열람. 노출 키는 폐기/재발급하고 secret manager 또는 개인 로컬 env로만 관리하세요.

5. [med] 외부 HTTP 호출에 timeout이 없음  
   근거: `BE/src/main/java/com/trip/global/config/RestClientConfig.java:16-20`, `WhisperSTTManager.java:46-52`, `BE/src/main/java/com/trip/festival/client/TourApiClient.java:28-41`, `PreprocessingService.java:31`  
   왜 문제인지: TourAPI/Whisper 호출 deadline이 없고, STT는 DB 트랜잭션 안에서 동기 호출됩니다. 외부 API 지연 시 요청 스레드와 트랜잭션이 장시간 점유됩니다.  
   재현/수정: 응답 없는 `openai.api.url`로 음성 업로드. connect/read timeout, bounded retry, 외부 호출과 저장 트랜잭션 분리를 적용하세요.

6. [med] 카카오 업로드 검증 부재로 사용자 입력 오류가 500 처리  
   근거: `BE/src/main/java/com/trip/preprocessing/service/PreprocessingService.java:38-40`, `PreprocessingService.java:57-58`, `BE/src/main/java/com/trip/global/error/exception/handler/GlobalExceptionHandler.java:170-178`  
   왜 문제인지: 빈 파일/크기/확장자/charset 검증 없이 UTF-8 전체 문자열로 읽습니다. 바이너리나 비 UTF-8 파일은 서버 예외로 떨어져 500이 됩니다.  
   재현/수정: `/analysis/upload/kakao`에 바이너리 업로드. multipart 검증과 디코딩 실패 400 매핑을 추가하세요.

7. [med] DataSeeder 부분 시드가 고착됨  
   근거: `BE/src/main/java/com/trip/global/config/DataSeeder.java:50-67`, `DataSeeder.java:52-54`, `DataSeeder.java:76-89`  
   왜 문제인지: 전체 시드가 트랜잭션 없이 실행되고 모든 예외를 삼킵니다. 사용자 생성 뒤 후속 시드가 실패하면 다음 부팅 때 demo 계정 존재만 보고 전체 skip되어 부분 데이터가 복구되지 않습니다.  
   재현/수정: `seedUsers()` 이후 예외 발생 후 재시작. `run()` 트랜잭션화 또는 데이터별 idempotent 복구 로직이 필요합니다.

8. [med] 원인 예외 메시지가 API 응답으로 누출됨  
   근거: `BE/src/main/java/com/trip/global/error/ResponseCode.java:108-110`, `BE/src/main/java/com/trip/global/error/GeneralException.java:71-72`, `BE/src/main/java/com/trip/global/error/exception/handler/GlobalExceptionHandler.java:157-162`, 사용처 `CompanionService.java:165-166`, `CompanionService.java:226-227`  
   왜 문제인지: `DataIntegrityViolationException` 같은 DB 예외 메시지가 `ResponseCode` 메시지 뒤에 붙어 클라이언트로 반환될 수 있습니다. 제약명/SQL 세부정보 노출 위험입니다.  
   재현/수정: 중복 신청/정원 race로 DB 제약 위반 유도. 클라이언트 응답은 고정 메시지로 두고 cause는 서버 로그에만 남기세요.