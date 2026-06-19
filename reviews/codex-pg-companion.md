리뷰만 수행했고 코드 수정은 하지 않았습니다.

1. [high] 승인 전 채팅방 IDOR
   근거: `SecurityConfig.java:65`, `CompanionPostResponse.java:26,53`, `StompSessionInterceptor.java:29-40`, `ChatStompController.java:32-39`, `ChatService.java:61-62`  
   왜: 공개 상세 조회가 `chatRoomId`를 내려주고, STOMP는 CONNECT JWT만 검증합니다. 멤버십/승인 검증 없이 임의 방 구독·전송이 가능합니다.  
   수정: 미승인 사용자에게 `chatRoomId` 숨김, SUBSCRIBE/SEND마다 active membership 검증, path `roomId`와 body `chatRoomId` 일치 검증.

2. [high] 동시 중복 신청 레이스
   근거: `CompanionService.java:150-164`, `CompanionApplication.java:12-13`  
   왜: `exists...` 후 `saveAndFlush` 구조인데 `companion_applications`에 unique 제약이 없습니다. 주석처럼 DB 충돌로 막히지 않아 동시 요청 시 중복 PENDING 신청이 생길 수 있습니다.  
   수정: `(post, applicant)` 생명주기 모델을 DB 제약/락으로 보장하고, 재신청은 기존 REJECTED row 갱신 방식으로 처리.

3. [high] 탈퇴 멤버가 정원·내 방에 계속 포함됨
   근거: `ChatService.java:101-113`, `ChatRoomMembership.java:59-60`, `CompanionService.java:201-205`, `CompanionService.java:288-299`, `CompanionService.java:303-305`  
   왜: 채팅방 나가기는 `leftAt`만 세팅하지만 companion은 전체 membership을 count/list 합니다. 탈퇴자가 정원을 계속 차지하고 `/my`에도 남습니다.  
   수정: active membership 전용 count/list 쿼리(`leftAt is null`, `isBanned=false`)로 교체하고 승인/목록/내방 모두 같은 기준 사용.

4. [med] 만석 모집글에도 신청 가능
   근거: `CompanionService.java:147-153`, `CompanionService.java:199-205`, `CompanionDetailView.vue:120-122`, `companion.js:20-27`  
   왜: 신청 단계는 상태/중복만 보고 정원은 승인 단계에서만 막습니다. FE도 남은 자리 0명이어도 신청 버튼을 노출합니다.  
   수정: 신청 시 active currentMembers >= maxMembers면 409, 만석 시 자동 CLOSED 또는 FE 비활성화.

5. [med] 신청자 관리 직접 진입 시 글 정보가 비어 있음
   근거: `CompanionApplicantsView.vue:12`, `CompanionApplicantsView.vue:19`, `CompanionApplicantsView.vue:66`, `CompanionApplicantsView.vue:80-82`  
   왜: 화면은 `companionStore.getById()`만 보는데 mount 시 `getApplications()`만 호출합니다. 새로고침/직접 URL 진입 시 제목·정원이 빈 값입니다.  
   수정: mount에서 `getDetail(postId)`도 호출하거나 신청자 API가 글 요약을 함께 반환.

6. [med] 승인된 신청자에게도 “승인 대기 중” 배너 노출
   근거: `CompanionDetailView.vue:60-66`, `CompanionDetailView.vue:126-130`, `CompanionDetailView.vue:180`  
   왜: 배너 조건이 `isApplied && !isOwner`라 APPROVED도 대기 상태로 표시됩니다. 하단 CTA는 채팅 입장이라 상태가 충돌합니다.  
   수정: 배너 조건을 `isApplied && !isApproved && !isOwner`로 제한.

7. [med] 운영 토큰 갱신이 API baseURL을 우회
   근거: `http.js:31`, `http.js:63`, `auth.js:28-30`, `.env.production:15`  
   왜: 일반 API는 `VITE_API_BASE_URL`을 쓰지만 refresh는 bare `axios.post('/auth/refresh')`입니다. 운영에서 정적 FE origin으로 호출되어 companion 생성/신청/승인 재시도가 깨집니다.  
   수정: refresh 전용 axios 인스턴스에도 동일 baseURL 적용하고 interceptor 재귀만 방지.

8. [med] 모집 생성 DTO 검증이 DB 제약과 불일치
   근거: `CompanionPostCreateRequest.java:15,17,20`, `CompanionPost.java:40,49`, `CompanionService.java:66,69`  
   왜: `duration`, `description`은 null 허용 DTO인데 엔티티는 nullable=false입니다. API 직접 호출 시 400이 아니라 DB 예외/500 경로가 됩니다. 과거 날짜도 `@NotNull`만 있어 허용됩니다.  
   수정: `@NotBlank`/기본값 처리, `@FutureOrPresent` 등 도메인 검증 추가.

9. [med] 목록·신청자 조회 N+1
   근거: `CompanionService.java:100-102`, `CompanionService.java:303-305`, `CompanionPostSummaryResponse.java:32`, `CompanionService.java:184-185`, `CompanionApplicationResponse.java:25-31`  
   왜: 게시글마다 인원 count, author lazy 접근, 신청자마다 applicant lazy 접근이 발생합니다. 데이터가 늘면 companion 목록/관리 화면이 급격히 느려집니다.  
   수정: fetch join/entity graph, chatRoomId별 count batch query, applicant fetch join 적용.

10. [low] 승인/거절 실패가 화면에 표시되지 않음
   근거: `CompanionApplicantsView.vue:84-91`, `CompanionApplicantsView.vue:94-100`, `companion.js:221`, `companion.js:238`  
   왜: store에는 에러가 남지만 view는 렌더링하지 않습니다. 정원 초과/이미 처리됨이 사용자에게 침묵 실패처럼 보입니다.  
   수정: 관리 화면에 `companionStore.error` 표시, 처리 중 버튼 비활성화.

11. [low] 모집 등록 더블클릭 중복 생성
   근거: `CompanionWriteView.vue:11`, `CompanionWriteView.vue:128`, `CompanionWriteView.vue:188-212`  
   왜: `submitting` ref가 unused이고 버튼 disabled가 `!isValid`뿐입니다. 빠른 연타로 중복 모집글/채팅방 생성이 가능합니다.  
   수정: submit in-flight guard와 버튼 disabled에 `submitting || loading` 반영.

12. [med] docker compose 기본 계정/비밀번호 노출
   근거: `docker-compose.yml:18`, `docker-compose.yml:57-58`, `docker-compose.yml:95`, `docker-compose.yml:100-101`, `application.yaml:29-30`, `application.yaml:57-58`  
   왜: DB `password`, RabbitMQ `guest/guest`가 배포 기본값입니다. companion 데이터와 승인 후 채팅 브로커가 같이 위험해집니다.  
   수정: 배포 compose에서 필수 env secret 사용, 기본 guest 비활성화, 관리 포트 외부 노출 제한.