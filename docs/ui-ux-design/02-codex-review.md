# 02. Codex GPT-5.5 (xhigh) — 세션 diff 검수

> `codex exec --skip-git-repo-check -s read-only`로 프론트 변경 2225줄 diff를 인라인 검수. 아래 지적은 모두 반영함(must-fix 4 + RISK들).

## Findings

**BUG** — `frontend/src/views/PlanView.vue`  
Context: `window.addEventListener('pointermove', onPointerMove...)`, `pointerup`, `pointercancel` in `onHandlePointerDown`, but no unmount cleanup.  
Fix: import/use `onBeforeUnmount`, remove `pointermove`, `pointerup`, and `pointercancel`, reset `pointerActive`, and `clearDrag()`. Also remove both `pointerup` and `pointercancel` inside `onPointerUp`; with `{ once: true }`, the event that does not fire remains registered.

**BUG** — `frontend/src/views/PlanView.vue`  
Context: `const items = Array.isArray(data) ? data : (data?.items ?? data?.content ?? [])` then `items.filter(...)`.  
If the attraction API returns a paginated Spring shape like `{ content: [...], ... }`, this is fine, but if `items/content` is missing or non-array, `.filter` throws at runtime.  
Fix: normalize defensively:
```js
const raw = Array.isArray(data) ? data : data?.items ?? data?.content ?? []
addPlace.value.results = Array.isArray(raw) ? raw.filter(i => i.contentId) : []
```

**BUG** — `frontend/src/views/PlanView.vue`  
Context: `const contentType = Number(item.contentTypeId ?? item.contentType)` and request `{ contentId: String(item.contentId), contentType }`.  
Many TourAPI-style attraction records use `contenttypeid` / `contentid` lowercase. Search results may display but fail with “정보 부족”.  
Fix: accept all known field names:
```js
const contentId = item.contentId ?? item.contentid
const contentType = Number(item.contentTypeId ?? item.contenttypeid ?? item.contentType)
```

**BUG** — `frontend/src/views/PlanView.vue`  
Context: `selectedDay.value = 1` when opening a new plan.  
If a plan’s first day number is not `1`, the map section renders chips but selected day has no matching data, so users see an empty day until tapping a chip.  
Fix: after detail load, set `selectedDay` to the first actual day:
```js
selectedDay.value = planStore.current?.days?.[0]?.dayNo ?? 1
```

**RISK** — `frontend/src/views/PlanView.vue`  
Context: `loadRoutePath(plan.id)` is fired without `await`, and route state is keyed only after response.  
If the user quickly opens plan A then plan B, A’s slower route response can overwrite `routePath` while B is selected. Computeds guard by `planId`, so the UI mostly hides it, but loading/error state can still become misleading.  
Fix: capture the requested `planId` and only assign/clear if it is still the selected plan.

**RISK** — `frontend/src/views/PlanView.vue`  
Context: `await planStore.addPlace(...)`; then `if (planStore.error) { ... return }`.  
This can surface a stale store error even if the add succeeded, depending on whether `addPlace` clears `error` synchronously.  
Fix: rely on thrown errors or have `planStore.addPlace` return success/failure; clear `planStore.error` before the request if that is the existing store pattern.

**RISK** — `frontend/src/views/PlanView.vue`  
Context: “add place to day” has no duplicate check before calling `planStore.addPlace`.  
Users can add the same attraction repeatedly if the backend allows it, or get a late backend error after tapping.  
Fix: check the selected day’s existing places by attraction/content id and show an inline “이미 추가된 장소예요” message before calling the API.

**RISK** — `frontend/src/views/PlanView.vue`  
Context: drag reorder uses `:key="place.id ?? idx"`.  
When unsaved/imported places lack `id`, using the index as key during reordering can cause DOM reuse glitches and wrong row visual state.  
Fix: prefer a stable domain key, e.g. `place.id ?? place.attraction?.contentId ?? place.contentId`, with index only as a last resort.

**RISK** — `frontend/src/views/CompanionWriteView.vue`  
Context: `for (const place of day.places ?? []) { const a = place.attraction; const url = a?.imageUrl }`.  
If plan detail places use image fields directly on `place` or TourAPI names like `firstimage`, this silently shows no image candidates.  
Fix: normalize image fields from both `place.attraction` and `place`.

**NIT** — `frontend/src/views/HomeView.vue`  
Context: `<PlaceCard v-else v-for="place in places" ... />`.  
`v-if/v-else` with `v-for` on the same component is easy to misread and can be fragile.  
Fix: wrap the list in a `<template v-else>` and put `v-for` on `PlaceCard`.

## Must-Fix Before Ship

1. Add pointer-drag listener cleanup in `PlanView.vue`, including the untriggered `{ once: true }` listener.
2. Harden add-place search result normalization and support lowercase TourAPI field names.
3. Initialize `selectedDay` from the actual loaded plan days.
4. Add duplicate-place handling before submitting the add request.
