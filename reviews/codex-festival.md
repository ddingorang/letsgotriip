검토 결과, 코드 수정 없이 festival 경로만 읽기 전용 리뷰했습니다.

1. [high] 배치 Reader 싱글턴 상태 재사용으로 2회차 sync가 전체 종료 처리  
   근거: `FestivalSyncJobConfig.java:55-57`, `FestivalItemReader.java:23-25`, `FestivalItemReader.java:56`, `FestivalSyncJobConfig.java:91-96` / 첫 실행 후 `areaCodeIndex`가 끝까지 간 상태로 남아 다음 실행은 read 0건이 되고, cleanup이 기존 행사를 전부 `ENDED`로 바꿀 수 있음 / `sync`를 연속 2회 실행해 재현. Reader를 `@StepScope`로 만들거나 실행마다 상태를 reset하고, cleanup은 완전 성공한 sync에만 수행.

2. [high] 일반 인증 사용자도 수동 동기화 가능하고 동시 실행 차단이 없음  
   근거: `SecurityConfig.java:54`, `FestivalController.java:36-38`, `FestivalSyncScheduler.java:26-30`, `FestivalItemReader.java:23-25` / 아무 로그인 사용자나 외부 API 호출·DB 갱신·cleanup을 유발할 수 있고, 매번 다른 `syncAt`으로 별도 JobInstance가 만들어져 동시 실행도 가능함 / ADMIN 또는 내부 작업으로 제한하고, 실행 중이면 409 반환하거나 DB/ShedLock 기반 락 적용.

3. [high] TourAPI 장애/오류 응답을 “빈 결과”로 처리해 데이터가 ENDED로 오염될 수 있음  
   근거: `TourApiClient.java:43-45`, `FestivalItemReader.java:45-49`, `FestivalSyncJobConfig.java:91-96`, `TourApiResponse.java:67-75` / null body, 인증키 오류, quota, 응답 스키마 변경이 빈 페이지처럼 처리되면 모든 지역을 “수신 없음”으로 간주하고 stale cleanup이 실행됨 / TourAPI header/resultCode를 파싱해 실패는 Job 실패로 처리하고, 성공한 전체 sync 마커가 있을 때만 stale cleanup 수행.

4. [high] 배치 실패도 컨트롤러는 항상 200 성공처럼 응답  
   근거: `FestivalSyncScheduler.java:30-33`, `FestivalController.java:37-39` / `jobLauncher.run()`의 `JobExecution` 상태를 확인하지 않고 “완료” 로그를 찍으며, 예외도 삼켜서 `/sync` 호출자는 실패를 알 수 없음 / `JobExecution.getStatus()/ExitStatus`를 반환값으로 판단해 FAILED는 5xx, 이미 실행 중은 409, 비동기 시작이면 202로 분리.

5. [med] 필수 필드 `title` 미검증으로 외부 데이터 1건이 배치를 중단시킬 수 있음  
   근거: `FestivalItemProcessor.java:21-22`, `FestivalItemProcessor.java:50-51`, `Festival.java:20`, `FestivalSyncJobConfig.java:50-51` / `contentId`만 검사하고 `title` null은 그대로 저장되어 JPA nullable 제약 위반으로 chunk/job 실패 가능 / `title`도 nonblank 검증 후 skip하거나 skip policy를 명시하고 실패 카운터를 로깅.

6. [med] 지역코드 체계가 축제와 추천 경로에서 충돌  
   근거: `FestivalItemProcessor.java:43-47`, `FestivalItemProcessor.java:59`, `AttractionTourApiClient.java:34,42`, `AiPlanInputView.vue:205-212`, `RecommendService.java:304` / 축제는 `lDongRegnCd` 예: `26`을 `area_code`에 저장하지만 AI 추천 입력은 구형 TourAPI `areaCode` 예: `6`을 사용해 `findByAreaCode("6")`가 부산 축제를 못 찾음 / legacy areaCode와 lDongRegnCd를 별도 컬럼으로 저장하거나 매핑 테이블을 두고 조회 전 변환.

7. [med] 날짜 기준이 JVM 기본 TZ, DB `CURDATE()`, 스케줄러 KST로 분산됨  
   근거: `FestivalSyncScheduler.java:23-24`, `TourApiClient.java:26`, `FestivalItemProcessor.java:27`, `FestivalService.java:27`, `FestivalSyncJobConfig.java:78` / 컨테이너/JVM/DB timezone이 다르면 00:00~08:59 KST 구간에 조회일·상태·cleanup 기준이 어긋날 수 있음 / `Clock` 또는 `ZoneId.of("Asia/Seoul")`를 주입하고 SQL도 `CURDATE()` 대신 애플리케이션 기준 날짜 파라미터 사용.

8. [med] TourAPI RestClient에 timeout이 없음  
   근거: `RestClientConfig.java:17-20`, `TourApiClient.java:28-41` / 외부 API 연결·응답 지연 시 스케줄러/수동 sync 요청 스레드가 장시간 묶이고, 동시 실행 문제와 결합해 장애 전파 가능 / connect/read timeout, 제한적 retry/backoff, 실패 시 Job 실패 처리를 명시.

9. [med] 공개 목록 조회가 무제한이고 festival 조회 인덱스가 없음  
   근거: `FestivalService.java:22`, `FestivalService.java:45-47`, `FestivalRepository.java:19-26`, `BE/docs/schema.sql:25-39`, `frontend/src/api/festival.js:28-31` / 전체 리스트를 한 번에 반환하고 `area_code/status/end_date` 필터용 인덱스도 없어 데이터 증가 시 풀스캔과 큰 payload가 됨 / `Pageable`/limit/sort 추가, `(area_code,status,end_date)`, `(status,end_date)` 인덱스 추가.

10. [low] `status=ENDED` API 계약이 실제 조회 동작과 맞지 않음  
    근거: `frontend/src/api/festival.js:18-20`, `FestivalService.java:26`, `FestivalService.java:36-38`, `FestivalSyncJobConfig.java:75-79` / FE 문서는 `ENDED` 조회를 허용하지만 BE는 항상 `endDate >= today`를 걸어 일반 종료 행사는 조회되지 않음 / `ENDED`를 거부·문서화하거나 종료 이력용 별도 조회를 제공.