코드 수정 없이 리뷰만 했습니다. 범위 내 BE 결제/예약/체크리스트 구현은 사실상 없고, 문제는 FE mock과 라우팅/상태 계약 쪽에 집중됩니다.

1. [high] 결제/확인 라우트 인증 가드 누락 / `frontend/src/router/index.js:67-68`, `frontend/src/router/index.js:81-83`  
   `/payment`, `/confirmation`에 `requiresAuth`가 없어 비로그인 사용자가 직접 접근해 예약/결제 완료 화면을 볼 수 있습니다. `ConfirmationView.vue:12-14`, `ConfirmationView.vue:41-42`는 실제 완료처럼 표시합니다.  
   수정 방향: 두 라우트에 인증 요구를 걸고, confirmation은 서버 검증된 booking/payment id 없으면 진입 차단 또는 오류 화면으로 보내야 합니다.

2. [high] 결제 성공/예약 생성 없이 완료 화면으로 이동 / `frontend/src/views/PaymentView.vue:127-129`, `frontend/src/views/PaymentView.vue:142-144`, `frontend/src/views/ConfirmationView.vue:98-101`  
   결제 버튼은 PG/예약 API 호출 없이 `/confirmation`으로 라우팅만 합니다. 실패, 취소, 타임아웃, 롤백, 중복 결제 같은 핵심 상태를 전혀 표현하지 못해 mock이 실패를 숨깁니다.  
   수정 방향: 서버 예약 생성과 결제 승인 API를 분리하고, 성공 응답의 id로 confirmation을 조회해야 합니다.

3. [med] 체크리스트 상태가 사용자/여행별로 저장되지 않음 / `frontend/src/views/ChecklistView.vue:193-195`, `frontend/src/views/ChecklistView.vue:202-217`  
   데이터는 `ref` 배열이고 토글은 메모리의 `checked`만 바꿉니다. 새로고침, 재방문, 다른 기기에서 전부 초기화됩니다. `/checklist`는 인증 라우트인데(`router/index.js:72`) 사용자별 데이터가 아닙니다.  
   수정 방향: plan/user 키로 API 또는 최소 localStorage 저장/복원 계약을 추가해야 합니다.

4. [med] 체크리스트 카운트가 실제 데이터와 불일치 / `frontend/src/views/ChecklistView.vue:52`, `frontend/src/views/ChecklistView.vue:163-164`, `frontend/src/views/ChecklistView.vue:211`, `frontend/src/views/ChecklistView.vue:220`  
   화면은 “긴급 2개”라 표시하지만 `urgent: true`는 1개뿐입니다. “여행 당일 0/4”도 표시만 있고 `totalItems`에는 교통+필수만 포함됩니다.  
   수정 방향: 모든 그룹을 하나의 데이터 모델로 만들고 완료/긴급/전체 카운트를 computed로 산출해야 합니다.

5. [med] 날짜/요일/D-day 하드코딩 오류 / `frontend/src/views/ChecklistView.vue:29-32`, `frontend/src/views/PaymentView.vue:152-153`, `frontend/src/views/ConfirmationView.vue:126-127`  
   2026-06-15는 토요일이 아니라 월요일이고, 2026-06-17은 월요일이 아니라 수요일입니다. 현재일 2026-06-19 기준 `D-3`도 이미 틀립니다.  
   수정 방향: ISO 날짜를 원천으로 요일과 D-day를 계산하고, 정적 문자열을 제거해야 합니다.

6. [med] 결제 약관 동의가 기본 true / `frontend/src/views/PaymentView.vue:111-119`, `frontend/src/views/PaymentView.vue:129`, `frontend/src/views/PaymentView.vue:147-148`  
   `agreed`가 처음부터 `true`라 사용자가 동의 행위를 하지 않아도 결제 버튼이 활성화됩니다. 약관/개인정보 문구도 `span`이라 실제 문서로 이동하지 않습니다.  
   수정 방향: 기본값은 `false`, 약관 링크는 실제 라우트/URL, 결제 요청 전 서버에서도 동의 여부를 검증해야 합니다.

7. [low] 예약 금액/숙박 기간 표시 불일치 / `frontend/src/views/PaymentView.vue:152-155`, `frontend/src/views/PaymentView.vue:164-168`  
   체크인~체크아웃은 2박인데 객실은 “1박”, 금액 행은 “숙박비 (2박)”입니다.  
   수정 방향: 숙박 일수와 금액 라벨을 같은 booking source에서 계산해야 합니다.

8. [low] 체크리스트의 눌리는 UI가 실제 동작 없음 / `frontend/src/views/ChecklistView.vue:11-17`, `frontend/src/views/ChecklistView.vue:61-66`, `frontend/src/views/ChecklistView.vue:146-151`, `frontend/src/views/ChecklistView.vue:177-188`  
   더보기, 빠른 추가, 항목 추가, 알림 설정이 버튼/칩처럼 보이지만 핸들러가 없습니다.  
   수정 방향: 기능을 구현하거나 비활성/숨김 처리해서 죽은 컨트롤을 제거해야 합니다.