# 관통 여행 — 디자인 시스템 가이드

> Claude Design(claude.ai/design)에서 제작한 디자인 시스템 핸드오프 문서.
> 원본 Figma: `docs/design/figma/관통 여행 UI.fig` (2026-06-12 export)

## 보는 방법

디자인 파일은 `frontend/public/design/`에 있어 Vite 개발 서버가 정적 파일로 그대로 서빙한다.

```bash
cd frontend
npm run dev
# 브라우저에서 열기:
# http://127.0.0.1:5173/design/ui_kits/app/index.html   ← 전체 화면 인덱스
```

인덱스에서 14개 화면 카드를 클릭하면 각 화면 프로토타입으로 이동한다.
HTML 파일은 **프로토타입**이며, 실제 구현 시 Vue 컴포넌트로 재작성한다(시각 결과만 일치시키고 내부 구조는 복사하지 않음).

## 파일 구조

```
frontend/public/design/
├── styles.css            ← 토큰 진입점 (이 하나만 import)
├── tokens/
│   ├── colors.css        ← 색상 (primary/neutral/semantic/surface/text/border)
│   ├── typography.css    ← Pretendard + 타입 스케일
│   └── spacing.css       ← 스페이싱, 반경, 그림자, z-index, 트랜지션, 레이아웃 상수
├── guidelines/           ← 파운데이션 스펙 카드 (색상, 타입, 스페이싱, 그림자)
├── components/
│   ├── core/             ← buttons, inputs, badges, cards
│   └── navigation/       ← tabbar, navbar
└── ui_kits/app/          ← 화면 프로토타입 14개 + index.html
```

## 핵심 토큰 요약

| 항목 | 값 |
|---|---|
| Primary | `#F78F57` (오렌지, `--color-primary-500`) — 사용자 확정값 |
| Primary 50 | `#FEF4EC` — 선택 상태 배경, 뱃지 배경 |
| 텍스트 | `#1A1A1A` / 보조 `#666` / 비활성 `#AAA` |
| 배경 | `#FFF` / 서브 `#F8F8F8` |
| 시맨틱 | Success `#34C759`, Warning `#FFCC00`, Error `#FF3B30`, Info `#007AFF` |
| 폰트 | Pretendard 단일 패밀리 (CDN @import), 11–32px, weight 400–800 |
| 스페이싱 | 4px 그리드, 화면 수평 패딩 20px |
| 반경 | 8(sm)/12(md)/16(lg)/20(xl)/24(2xl)/9999(full) |
| 기준 해상도 | 390×844 (iPhone 14), StatusBar 54px, NavBar 52px, TabBar 83px |
| 인터랙션 | 호버·탭 `opacity .75`, 150ms ease / 전환 250ms cubic-bezier(0.4,0,0.2,1) |

Vue에서 사용하려면 전역 CSS에서 `@import '/design/styles.css';` 한 줄로 토큰 전체를 가져올 수 있다
(추후 `src/assets/styles/`로 토큰을 옮겨 빌드에 포함하는 것을 권장).

## 화면 ↔ 도메인 매핑

| # | 화면 파일 | 화면 | 도메인 (docs/domain-architecture.md 기준) |
|---|---|---|---|
| 01 | `01_home.html` | 홈 (배너·카테고리·추천) | 2. 관광 콘텐츠 탐색 |
| 02 | `02_search.html` | 검색 (필터·결과 리스트) | 2. 관광 콘텐츠 탐색 |
| 03 | `03_detail.html` | 상세 (히어로·편의시설) | 2. 관광 콘텐츠 탐색 |
| 04 | `04_payment.html` | 결제 (스텝·결제 수단) | (확장 — 예약/결제) |
| 05 | `05_mypage.html` | 마이페이지 (프로필·내역) | 1. 사용자·인증 / 10. 마이페이지 |
| 06 | `06_review.html` | 리뷰 (평점·리뷰 카드) | 8. 커뮤니티·UGC |
| 07 | `07_confirmation.html` | 예약 완료 (티켓) | (확장 — 예약/결제) |
| 08 | `08_ai_input.html` | AI 추천 입력 (조건 폼) | 4. AI 추천 |
| 09 | `09_ai_result.html` | AI 추천 결과 (일정 초안·계획 담기) | 4. AI 추천 |
| 10 | `10_plan_edit.html` | 계획 편집 (타임라인·지도 미리보기) | 3. 여행 계획 |
| 11 | `11_community.html` | 커뮤니티 (후기 게시판·핫플) | 8. 커뮤니티·UGC |
| 12 | `12_companion.html` | 동행 모집 (상태별 카드·신청) | 9. 동행·그룹·단체할인 |
| 13 | `13_badges.html` | 뱃지·퀘스트 (레벨·뱃지 그리드) | 10. 마이페이지·기록·게임화 |
| 14 | `14_checklist.html` | 체크리스트 (D-day·카테고리별 항목) | 11. 체크리스트 |

아직 화면이 없는 도메인: 5(여행 평가·최적화), 6(여행 맥락 정보), 7(데이터 수집·STT), 12(운영·관리), 그리고 온보딩/로그인 화면.

## 콘텐츠·아이콘 규칙

- 한국어 메인, "해요체", 이모지 미사용, CTA는 짧게("지금 예약하기", "결제하기")
- 통화 `₩000,000`, 날짜 `YYYY.MM.DD(요일)`
- 아이콘: 24×24 outline(2px stroke) 인라인 SVG, CDN 미사용
  활성 `var(--color-primary-500)` / 비활성 `var(--text-tertiary)`

## 관련 파일

- 디자인 핸드오프 대화 기록: [handoff-chat.md](handoff-chat.md)
- 도메인 아키텍처: [../domain-architecture.md](../domain-architecture.md)
- 디자인 시스템 원문 README: `frontend/public/design/README.md`
