코드 수정 없이 리뷰만 했습니다.

1. [high] approve/reject 동시 처리 시 상반된 알림이 둘 다 나갈 수 있음  
   근거: `CompanionService.approveApplication`은 post만 락(`BE/src/main/java/com/trip/companion/service/CompanionService.java:190-193`) 잡고 수락 알림을 발행합니다(`:233-239`). `rejectApplication`은 락 없이 같은 신청을 읽고 반려 알림을 발행합니다(`:245-258`). `CompanionApplication`에는 `@Version`도 없습니다(`BE/src/main/java/com/trip/companion/entity/CompanionApplication.java:20-35`).  
   왜 문제인지: 같은 PENDING 신청을 동시에 approve/reject하면 채팅 멤버십은 생성됐는데 최종 status는 REJECTED가 되거나, 수락/반려 알림이 모두 수신될 수 있습니다.  
   수정 방향: application row를 approve/reject 양쪽에서 동일하게 lock하거나 `@Version`으로 상태 전이를 원자화하고, 성공한 상태 전이에 대해서만 알림을 발행하세요.

2. [high] 업로드 파일명이 path traversal에 열려 있음  
   근거: 클라이언트 제공 `file.getOriginalFilename()`을 그대로 붙입니다(`BE/src/main/java/com/trip/preprocessing/service/PreprocessingService.java:49`), 그 값을 `directory.resolve(fileName)` 후 저장합니다(`:50-51`).  
   왜 문제인지: multipart filename에 경로 구분자와 `../`가 들어오면 `temp_uploads` 밖으로 파일이 기록될 수 있습니다. UUID prefix만으로는 정규화된 경로 이탈을 막지 못합니다.  
   수정 방향: 원본명은 basename만 추출하고, 허용 문자로 재생성한 뒤 `normalize()` 결과가 upload dir 하위인지 검사하세요.

3. [high] 민감한 카톡/음성 원본 파일이 남음  
   근거: 임시 업로드 디렉터리(`:32`)에 파일을 저장하고(`:49-51`), `storagePath`까지 DB에 저장합니다(`:53-58`). 처리 후 삭제가 없습니다(`:62-64`).  
   왜 문제인지: 카톡 대화와 통화 음성은 PII 덩어리인데, 성공/실패 모두 원본 파일 잔존 가능성이 있습니다. 트랜잭션 롤백도 파일 시스템 저장은 되돌리지 않습니다.  
   수정 방향: 처리 후 `finally`에서 삭제하거나, 보관이 필요하면 암호화·보존기간·접근통제를 명시하세요.

4. [med] BE 업로드 입력검증 누락으로 잘못된 파일이 500이 됨  
   근거: controller는 `MultipartFile`을 바로 service로 넘깁니다(`BE/src/main/java/com/trip/preprocessing/controller/PreprocessingController.java:24-38`). service는 빈 파일/타입/확장자 검증 없이 저장 후, 카톡은 UTF-8로 읽고(`BE/src/main/java/com/trip/preprocessing/service/PreprocessingService.java:67-70`), 음성은 STT로 보냅니다(`:71-72`). 미처리 예외는 전역 catch-all에서 500입니다(`BE/src/main/java/com/trip/global/error/exception/handler/GlobalExceptionHandler.java:167-179`).  
   왜 문제인지: 빈 파일, 바이너리 `.txt`, 잘못된 음성 파일 같은 사용자 입력 오류가 400/415가 아니라 500으로 노출됩니다.  
   수정 방향: `file.isEmpty`, size, 확장자, MIME, 필요 시 magic bytes를 서버에서 검증하고 `GeneralException(_BAD_REQUEST)` 또는 413/415로 매핑하세요.

5. [med] multipart 용량 초과도 500 가능  
   근거: Spring multipart 제한은 20MB/25MB입니다(`BE/src/main/resources/application.yaml:7-10`). 그런데 `MaxUploadSizeExceededException`/`MultipartException` 전용 핸들러가 없고, catch-all은 500입니다(`GlobalExceptionHandler.java:167-179`).  
   왜 문제인지: 용량 초과는 클라이언트 입력 오류인데 서버 장애처럼 처리됩니다. FE는 413 문구를 준비했지만(`frontend/src/views/AnalysisUploadView.vue:337-339`) BE가 413을 보장하지 않습니다.  
   수정 방향: multipart 용량 예외를 413으로 명시 처리하세요.

6. [med] Whisper 외부 호출 타임아웃 없음 + DB 트랜잭션 안에서 호출  
   근거: STT 호출은 `@Transactional` 메서드 안에서 실행됩니다(`PreprocessingService.java:42`, `:72`). Whisper는 주입된 `RestClient`로 호출합니다(`BE/src/main/java/com/trip/preprocessing/client/impl/WhisperSTTManager.java:48-54`). 공용 `RestClient` 설정에는 timeout이 없습니다(`BE/src/main/java/com/trip/global/config/RestClientConfig.java:17-20`).  
   왜 문제인지: Whisper/GMS 장애 시 servlet thread와 DB transaction이 오래 묶이고, FE timeout 이후에도 서버 작업이 계속될 수 있습니다.  
   수정 방향: STT용 RestClient에 connect/read timeout을 두고, 외부 호출은 DB 트랜잭션 밖으로 분리하거나 짧은 트랜잭션으로 저장하세요.

7. [med] 좋아요 토글 동시성으로 likeCount/HTTP 상태가 깨질 수 있음  
   근거: `toggleLike`는 조회 후 저장/삭제 방식입니다(`CommunityService.java:131-140`). `PostLike`에는 unique 제약이 있지만(`BE/src/main/java/com/trip/community/entity/PostLike.java:9-12`), `Post.likeCount`에는 버전/락 없이 단순 증감만 있습니다(`BE/src/main/java/com/trip/community/entity/Post.java:36-38`, `:62-68`).  
   왜 문제인지: 서로 다른 사용자의 동시 좋아요는 lost update로 count가 틀어질 수 있고, 같은 사용자의 더블클릭 레이스는 DB unique 충돌이 catch되지 않아 500으로 갈 수 있습니다.  
   수정 방향: post row lock/atomic update/`@Version` 중 하나로 count를 보호하고, unique 충돌은 409 또는 idempotent 결과로 변환하세요.

8. [low] FE 파일검증/진행상태가 실제 계약과 맞지 않음  
   근거: UI는 음성 `m4a/mp3/wav`라고 안내합니다(`frontend/src/views/AnalysisUploadView.vue:146-150`) but 검증은 모든 `audio/*`를 허용합니다(`:222-226`). 카톡도 `.txt` 안내(`:154-159`)와 달리 `text/plain`이면 확장자 없이도 통과합니다(`:227`). size 검증은 없습니다(`:213-228`). 진행률도 실제 upload progress가 아니라 interval 가짜 진행입니다(`:251-258`), `processing`은 응답을 받은 뒤에야 켜집니다(`:263-266`) and 곧 idle로 바뀝니다(`:274-278`).  
   왜 문제인지: 사용자는 서버가 거절하거나 실패할 파일을 사전에 걸러내지 못하고, 긴 STT 처리 중 “실제 진행”을 볼 수 없습니다.  
   수정 방향: FE/BE 공통 허용 타입·최대 크기를 상수화하고, axios `onUploadProgress` 또는 서버 job/status API로 진행상태를 분리하세요.