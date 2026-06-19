검토 범위 내 코드 수정은 하지 않았습니다.

1. [high] planId 소유권/존재 검증 없이 체크리스트가 연결됨  
   근거: [ChecklistService.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/service/ChecklistService.java:40), [ChecklistService.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/service/ChecklistService.java:89), [ChecklistItem.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/entity/ChecklistItem.java:34)  
   `request.planId()`/`planId`를 그대로 저장하고, 엔티티도 FK/TripPlan 관계가 아닌 Long 컬럼뿐입니다. 임의 planId를 내 체크리스트에 연결할 수 있어 plan 소유권 경계가 깨집니다.  
   수정 방향: planId가 있으면 PlanService/PlanRepository로 존재 및 소유자 검증 후 저장. 가능하면 FK/관계 매핑 또는 최소 DB 제약도 추가.

2. [high] 빈 체크리스트에서는 수동 첫 항목 추가가 불가능함  
   근거: [ChecklistView.vue](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/frontend/src/views/ChecklistView.vue:70), [ChecklistView.vue](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/frontend/src/views/ChecklistView.vue:76), [ChecklistView.vue](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/frontend/src/views/ChecklistView.vue:111), [ChecklistView.vue](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/frontend/src/views/ChecklistView.vue:140)  
   하단 “항목 추가”는 `startAdd(defaultCategory)`만 호출하지만, 입력 행은 기존 group 내부에서만 렌더링됩니다. `groups.length === 0`이면 입력 UI가 생기지 않습니다.  
   수정 방향: empty-state에도 add row를 렌더링하거나, 추가 모드일 때 fallback group을 임시로 렌더링.

3. [med] 템플릿 적용이 매번 중복 생성되고 정렬도 0부터 재시작됨  
   근거: [ChecklistService.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/service/ChecklistService.java:85), [ChecklistService.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/service/ChecklistService.java:94), [ChecklistService.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/service/ChecklistService.java:98), [ChecklistView.vue](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/frontend/src/views/ChecklistView.vue:48)  
   같은 템플릿을 다시 누르면 동일 항목을 전부 다시 저장합니다. 새 템플릿 항목 sortOrder도 항상 0부터라 기존 항목과 섞입니다.  
   수정 방향: 템플릿 적용 이력/중복 방지 정책을 두고, 추가 생성 시 현재 max sortOrder 이후로 append.

4. [med] 수동 생성 정렬 계약이 BE/FE에서 서로 다름  
   근거: [ChecklistService.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/service/ChecklistService.java:45), [ChecklistItemRepository.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/repository/ChecklistItemRepository.java:12), [ChecklistView.vue](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/frontend/src/views/ChecklistView.vue:311)  
   BE는 sortOrder 미지정 시 0으로 저장하고 조회는 sortOrder/id 오름차순입니다. FE는 생성 응답을 맨 뒤에 append합니다. 즉 즉시 화면 순서와 reload 후 순서가 달라질 수 있습니다.  
   수정 방향: 서버에서 append용 sortOrder를 계산하거나, FE가 생성 후 목록을 재조회/정렬.

5. [med] PATCH에서 빈 제목 저장 가능  
   근거: [ChecklistItemCreateRequest.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/dto/ChecklistItemCreateRequest.java:10), [ChecklistItemUpdateRequest.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/dto/ChecklistItemUpdateRequest.java:9), [ChecklistItem.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/entity/ChecklistItem.java:72)  
   생성은 `@NotBlank`인데 수정은 `@Size`만 있어 `""` 또는 공백 제목이 통과합니다.  
   수정 방향: “null이면 유지, 값이 있으면 not blank” 검증을 DTO 커스텀 validator나 서비스에서 적용.

6. [med] toggle은 동시 요청에서 lost update 가능  
   근거: [ChecklistService.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/service/ChecklistService.java:60), [ChecklistItem.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/entity/ChecklistItem.java:67)  
   read-modify-write로 `checked`를 뒤집고, 엔티티에 `@Version`/락/원자적 update가 없습니다. 두 요청이 동시에 false를 읽으면 둘 다 true로 저장될 수 있습니다.  
   수정 방향: DB 원자 update 쿼리 또는 optimistic locking을 적용하고 충돌 응답을 명확히 처리.

7. [med] planId/dayNo/sortOrder 입력 범위 검증 누락  
   근거: [ChecklistItemCreateRequest.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/dto/ChecklistItemCreateRequest.java:12), [ChecklistItemCreateRequest.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/dto/ChecklistItemCreateRequest.java:13), [ChecklistItemCreateRequest.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/dto/ChecklistItemCreateRequest.java:14), [ChecklistItemUpdateRequest.java](C:/Users/te/Desktop/next/springaitrip/.claude/worktrees/elated-leakey-81a1ee/BE/src/main/java/com/trip/checklist/dto/ChecklistItemUpdateRequest.java:12)  
   음수 planId/dayNo/sortOrder가 API 레벨에서 막히지 않습니다. 특히 dayNo는 plan의 실제 일차 범위와도 검증되지 않습니다.  
   수정 방향: `@Positive`, `@Min(1)`, sortOrder 하한 검증, plan 기간/day 존재 검증 추가.