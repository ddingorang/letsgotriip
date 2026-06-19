수정 없이 코드만 리뷰했습니다.

**Findings**
1. [high] 동선 리포트 실패가 “거리 기반 적용” 성공처럼 보임  
   `frontend/src/views/PlanReportView.vue:229`, `:232`, `:476`, `:564`  
   왜: `/route-report` 실패를 삼키고 `report=null`이면 FE Haversine 재정렬을 활성화해 서버에 저장까지 합니다.  
   수정: 리포트 로드 실패 상태를 분리하고, 실패 시 적용 버튼 비활성/에러 표시.

2. [high] 계획 목록 API 실패를 빈 상태로 위장  
   `frontend/src/views/PlanView.vue:38`, `:322`, `:326`  
   왜: BE 불가/인증 만료도 “아직 여행 계획이 없어요”로 보입니다.  
   수정: `loading/error/empty` 상태를 분리.

3. [high] 동행 상세 실패 시 가짜 기본 글이 렌더링됨  
   `frontend/src/views/CompanionDetailView.vue:217`, `:367`, `:175`  
   왜: 상세 조회 실패/404에도 fallback 객체로 “동행 모집” 화면과 참여 버튼이 살아있습니다.  
   수정: 실제 데이터 없으면 not-found/error 화면으로 전환하고 CTA 비활성화.

4. [med] “AI 동선 최적화” UI가 실제 구현을 과장  
   `frontend/src/views/PlanView.vue:207`, `frontend/src/views/PlanReportView.vue:49`, `:232`, `BE/src/main/java/com/trip/plan/util/RouteCalculator.java:9`  
   왜: 실제는 외부 라우팅/AI가 아니라 좌표 Haversine + nearest-neighbor입니다.  
   수정: 문구를 “좌표 기반 동선 추정”으로 바꾸거나 실제 AI/라우팅 엔진 연결.

5. [med] PlanSummary DTO와 FE 필드 불일치  
   `frontend/src/views/PlanView.vue:79`, `:81`, `BE/src/main/java/com/trip/plan/dto/PlanSummaryResponseDto.java:10`  
   왜: BE 목록 DTO에는 `destination`, `spots`가 없어 `undefined`/빈 칩이 렌더됩니다.  
   수정: DTO에 필드 추가 또는 FE에서 존재 필드만 렌더.

6. [med] PlanView 동행 섹션이 하드코딩 목업  
   `frontend/src/views/PlanView.vue:316`, `:317`, `:248`  
   왜: 실제 동행 API와 무관한 모집글이 진짜처럼 보이고 참여는 generic 탭 이동뿐입니다.  
   수정: 실제 목록 API 연동 또는 섹션 제거/샘플 명시.

7. [med] TripMap 마커 클릭이 PlanReport에서 죽어 있음  
   `frontend/src/views/PlanReportView.vue:95`, `frontend/src/components/common/TripMap.vue:119`, `:91`  
   왜: `TripMap`은 `select/detail`을 emit하지만 부모가 받지 않아 클릭해도 변화가 없습니다.  
   수정: `@select`, `selectedId`, `@detail`을 연결하거나 마커 클릭 affordance 제거.

8. [med] 비교 모달 absolute 기준점 불명확  
   `frontend/src/views/PlanView.vue:556`, `:1127`  
   왜: `.compare-overlay`는 `position:absolute`인데 `.page`에 `position:relative`가 없습니다. 앱 쉘에 따라 시트가 페이지 밖 기준으로 깔릴 수 있습니다.  
   수정: 모달은 `fixed`/Teleport 사용 또는 `.page { position: relative }`.

9. [low] `day.summary`는 응답 필드가 아님  
   `frontend/src/views/PlanView.vue:109`, `frontend/src/views/PlanReportView.vue:132`, `BE/src/main/java/com/trip/plan/dto/DayResponseDto.java:7`  
   왜: BE는 `memo`만 내려줍니다. summary UI는 영원히 표시되지 않습니다.  
   수정: `day.memo` 사용 또는 DTO에 summary 추가.

10. [low] 동행 상세 공유 버튼이 일부 브라우저에서 무반응  
   `frontend/src/views/CompanionDetailView.vue:16`, `:419`  
   왜: `navigator.share` 없으면 아무 동작도 없습니다.  
   수정: clipboard fallback 또는 미지원 시 버튼 숨김/비활성.