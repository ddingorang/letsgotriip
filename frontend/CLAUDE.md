# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm run dev       # dev server at http://127.0.0.1:5173
npm run build     # production build → dist/
npm run preview   # preview the built output
```

No test runner or linter is configured.

## Architecture

Vue 3 + Vite SPA for the 관통여행 travel platform. Mobile-first UI (390×844 design base) implementing the "관통 여행" design system. Currently all data is static seed data inside each view; no backend integration yet (backend lives in `../backend`).

### Design system

- Token source of truth: `src/assets/styles/tokens/{colors,typography,spacing}.css`, imported via `src/assets/design.css` (global). Primary brand color `#F78F57`, Pretendard font, 4px spacing grid. Use `var(--...)` tokens in scoped styles — never hardcode colors/sizes that have a token.
- Design prototypes (the spec to match): `public/design/ui_kits/app/*.html`, index at `/design/ui_kits/app/index.html` on the dev server. Docs: `../docs/design/design-system.md`, domain map: `../docs/domain-architecture.md`.

### App shell

`App.vue` renders `.app-frame` (max-width 430px, centered on desktop) with `<RouterView>` plus the global `AppTabBar`. Routes with `meta: { tabBar: false }` (detail/payment/confirmation/ai-result/plan) hide the tab bar for full-screen flows.

- `src/components/layout/AppTabBar.vue` — fixed bottom 5-tab bar: 홈(/), 탐색(/search), 중앙 FAB AI추천(/ai), 커뮤니티(/community), 마이(/mypage)
- `src/components/layout/AppNavBar.vue` — shared back+title top bar (`title` prop, `#action` slot). Screens with custom navs (search input, dark hero, gradient profile header) keep their nav inline instead.

### Views (src/views/, one per design screen)

| Route | View | Design prototype |
|---|---|---|
| `/` | HomeView | 01_home |
| `/search` | SearchView | 02_search |
| `/detail/:id?` | DetailView | 03_detail |
| `/payment` | PaymentView | 04_payment |
| `/mypage` | MyPageView | 05_mypage |
| `/review` | ReviewView | 06_review |
| `/confirmation` | ConfirmationView | 07_confirmation |
| `/ai` | AiInputView | 08_ai_input |
| `/ai/result` | AiResultView | 09_ai_result |
| `/plan` | PlanEditView | 10_plan_edit |
| `/community` | CommunityView | 11_community |
| `/companion` | CompanionView | 12_companion |
| `/badges` | BadgesView | 13_badges |
| `/checklist` | ChecklistView | 14_checklist |

Conversion conventions (when adding screens from prototypes): drop the iOS status bar, drop the per-page tab bar (shell owns it), keep inline SVGs and Korean copy verbatim, repeated items become local `const` arrays + `v-for`, fixed bottom bars use `left: 50%; transform: translateX(-50%); max-width: 430px`.

### State & API

- Pinia is installed; legacy stores `src/stores/tripUi.js` / `festival.js` and API clients `src/api/client.js` / `festival.js` (TourAPI 4.0, `VITE_KOREA_TOURISM_API_KEY`) remain from the previous prototype and are currently unused by the new views — reuse `src/api/festival.js` when wiring real festival data.
- `src/legacy/` holds the pre-redesign App.vue/router/main.js (split-screen rail prototype) for reference only. Old components (`BackdropScene`, `ExploreWorkspace`, `PageWorkspace`, `RailHeader`, `FestivalWorkspace`) are unmounted but kept.

### Adding a new page

1. Create `src/views/XxxView.vue` following the conversion conventions above
2. Register the route in `src/router/index.js` (add `meta: { tabBar: false }` for full-screen flows)
3. If it's a main tab destination, add it to `AppTabBar.vue`
