# 04. 여행 편집/담기 UX — Codex GPT-5.5(xhigh) 제안

> `codex exec`로 plan-editing·"일정에 담기" UX를 설계 검토. 사용자 피드백(드래그 느림/안됨, 담을 위치 선택, UX 재고)에 대한 응답.

# Triip Itinerary Editing Proposal

## 1. Editing Container

Recommendation: move itinerary editing into its own full-screen plan detail/edit screen.

Current inline expansion is acceptable for quick preview, but it is the wrong primary editing container on mobile. At ~390px, expanded cards create nested scanning, long vertical pages, drag/scroll conflicts, and weak task focus. Editing an itinerary is closer to managing a document than opening an accordion.

Use this model:

- `PlanView`: plan cards only, optimized for browsing.
- Tap plan card → `PlanDetailView`.
- `PlanDetailView`: dedicated itinerary editor with:
  - sticky top plan title/date summary
  - segmented day tabs: `1일차 / 2일차 / 3일차`
  - selected day’s place list
  - per-day map below or behind a collapsible “지도 보기”
  - sticky bottom action bar: `장소 추가`, `경로 보기`, optional `완료`

Trade-off:

- Inline expand is faster for preview, but breaks down as soon as users edit, reorder, add, delete, or inspect maps.
- Full-screen adds one navigation step, but gives room for clear controls, better drag behavior, day switching, and future features like move-to-day or visit-time editing.

Keep inline expansion only as a read-only preview if needed. Do real editing in the full-screen plan editor.

## 2. Reorder UX

Use a visible drag handle, not whole-row drag.

Whole-row drag sounds simpler, but on mobile it conflicts with vertical scrolling and tapping place rows. Long-press is cleaner technically, but discoverability is weak, especially for a student demo audience. The current handle is the right base, but it needs stronger affordance and feedback.

Recommended row layout:

`≡  1  Place name  10:30  ⋯`

- Make the handle touch target at least 44px wide.
- Use a proper grip icon, not tiny text.
- Show helper text once per day: `≡ 길게 눌러 순서 변경`
- On drag start:
  - row lifts with shadow
  - slight scale, e.g. `1.02`
  - placeholder remains in list
  - haptic-like visual feedback if native vibration is unavailable
- During drag:
  - auto-scroll near top/bottom
  - animate neighboring rows into place
- After drop:
  - optimistic order stays immediately
  - small toast: `순서가 변경됐어요`
  - background save failure reverts with toast: `저장에 실패했어요. 다시 시도해 주세요`

Gesture decision:

- Drag starts only from the handle.
- Normal row scroll remains vertical page scroll.
- Tapping row opens place detail.
- `⋯` opens row actions.

Cross-day move: support it, but not primarily through drag across day headers.

Dragging across day tabs/headers on mobile is fragile: users must hold, scroll, hover over a tab, and understand invisible drop zones. It is high implementation effort and easy to demo poorly.

Use a row action instead:

- Tap `⋯` → bottom sheet:
  - `방문 시간 설정`
  - `다른 날로 이동`
  - `삭제`
- `다른 날로 이동` → day picker sheet:
  - `1일차`, `2일차`, `3일차`
  - show current day disabled
  - after selection, append to end of that day
  - optional second step later: choose position

This is more reliable, discoverable, and demo-safe.

## 3. “일정에 담기” From Place Detail

Current behavior, auto-adding to first plan day 1, should be removed. The user must choose destination.

Use a bottom sheet from place detail.

Primary flow:

1. Tap `일정에 담기`
2. Bottom sheet opens: `어느 일정에 담을까요?`
3. If user has plans:
   - show recent/upcoming plans as selectable rows
   - each row: plan title, trip dates, destination, day count
   - default-select most recent active plan if obvious
4. After selecting plan:
   - same sheet transitions to day picker
   - segmented/chip list: `1일차`, `2일차`, `3일차`
   - each day shows date and place count, e.g. `5월 3일 · 4곳`
5. Optional visit time:
   - show compact control before final add:
     - `방문 시간` row
     - default: `나중에 정하기`
     - optional quick chips: `오전`, `점심`, `오후`, `저녁`
     - detailed time picker behind `시간 선택`
6. CTA: `1일차에 담기`

Minimize taps with smart defaults:

- If user has exactly one plan:
  - skip plan list visually by preselecting it
  - show day picker immediately
- If user has one plan and one day:
  - show confirmation sheet with CTA `일정에 담기`
- If user recently edited a plan:
  - preselect that plan, but still let user change it

No plan case:

Bottom sheet title: `먼저 여행 계획을 만들어 주세요`

Actions:

- Primary: `새 계획 만들기`
- Secondary: `취소`

After creating a plan, return directly to the same add flow with the new plan selected.

Where visitTime should be set:

- In the add flow as optional, not required.
- Also editable later from itinerary row action.
- Do not force a full time picker during add; it slows down the main action.

## 4. Quick Wins vs Larger Refactors

| Rank | Change | Impact | Effort | Notes |
|---:|---|---|---|---|
| 1 | Replace auto-add with bottom-sheet plan/day picker | High | M | Directly fixes explicit user complaint |
| 2 | Improve drag handle touch target + drag feedback + helper text | High | S | Makes existing optimistic reorder feel intentional |
| 3 | Add row `⋯` action sheet with `방문 시간 설정 / 다른 날로 이동 / 삭제` | High | M | Safer than cross-day drag |
| 4 | Move editing into dedicated `PlanDetailView` | High | M/L | Best structural UX improvement |
| 5 | Add segmented day tabs in plan editor | Medium/High | M | Reduces vertical overload |
| 6 | Add toast/error handling for optimistic reorder save | Medium | S | Important trust signal |
| 7 | Collapsible per-day map or sticky map toggle | Medium | M | Prevents map from burying edit controls |
| 8 | Full cross-day drag | Low/Medium | L | Avoid for student demo; fragile on touch |

## 5. Implement Next: Top 3

### 1. Add Plan/Day Picker For `일정에 담기`

Vue-level approach:

- Create `AddToItinerarySheet.vue`.
- Props:
  - `place`
  - `plans`
  - `recentPlanId?`
- Emits:
  - `confirm({ planId, dayIndex, visitTime? })`
  - `create-plan`
  - `close`
- State:
  - `selectedPlanId`
  - `selectedDayIndex`
  - `visitTimeMode: 'none' | 'quick' | 'exact'`
- Use one bottom sheet with two internal steps: `plan` → `day`.
- If only one plan exists, initialize `selectedPlanId` and start at `day`.

This should be the first implementation priority because it fixes the most obvious product flaw.

### 2. Upgrade Reorder Interaction Without Changing Data Model

Vue-level approach:

- Keep current within-day optimistic reorder.
- Increase drag handle hit area with CSS: `width: 44px; min-height: 44px`.
- Add drag state:
  - `draggingPlaceId`
  - `isSavingOrder`
- Apply classes:
  - `.place-row--dragging`
  - `.place-row--drop-target`
  - `.place-row--saving`
- Add one-time helper text above list: `≡ 길게 눌러 순서 변경`.
- Add toast on success/failure.
- On failure, restore previous order from a snapshot captured before drag.

This gives users a faster-feeling, more understandable interaction without rebuilding the editor.

### 3. Add Row Action Sheet With Move-To-Day

Vue-level approach:

- Add `PlaceActionSheet.vue`.
- Open from a `⋯` button on each place row.
- Actions:
  - `방문 시간 설정`
  - `다른 날로 이동`
  - `삭제`
- For move:
  - show day list inside the same sheet
  - call existing update API or create a small move endpoint/client method
  - optimistic update: remove from current day, append to target day
  - rollback on failure

Do this before attempting cross-day drag. It solves the user need with less gesture risk and a clearer mobile pattern.
