# 관통 여행 Design System

> 관통 여행 모바일 앱을 위한 공식 디자인 시스템입니다.

**소스:** `uploads/관통 여행 UI.fig` (Figma, 2026-06-12 export)  
**플랫폼:** iOS / Android 모바일 앱 (390×844px 기준)  
**폰트:** Pretendard (오픈소스 한글 폰트)

---

## 파일 구조

```
/
├── styles.css              ← 전체 토큰 진입점 (이 하나만 import)
├── tokens/
│   ├── colors.css          ← 색상 토큰 (primary, neutral, semantic, surface)
│   ├── typography.css      ← 폰트 + 타입 스케일 토큰
│   └── spacing.css         ← 스페이싱, 반경, 그림자, z-index, 트랜지션 토큰
├── guidelines/             ← 파운데이션 스펙 카드 (DS 탭에 표시)
├── components/
│   ├── core/               ← 버튼, 인풋, 배지/태그, 카드
│   └── navigation/         ← 탭바, 네비게이션바
├── ui_kits/app/            ← 앱 핵심 화면 HTML
│   ├── 01_home.html
│   ├── 02_search.html
│   ├── 03_detail.html
│   ├── 04_payment.html
│   ├── 05_mypage.html
│   ├── 06_review.html
│   └── 07_confirmation.html
└── SKILL.md
```

---

## VISUAL FOUNDATIONS

### 색상 시스템
- **Primary Orange** `#F78F57` — 브랜드 포인트 컬러. CTA 버튼, 링크, 활성 상태, 아이콘 등 전반에 사용
- **Primary 50** `#FFF4EC` — 선택 상태 배경, 뱃지 배경으로 사용
- **Neutral** — 텍스트(`#1A1A1A`), 보조 텍스트(`#666`), 비활성(`#AAA`), 배경(`#F8F8F8`)
- **시맨틱** — Success(녹색), Warning(노랑), Error(빨강), Info(파랑)

### 타이포그래피
- **Pretendard** 단일 패밀리 사용. 폰트 사이즈: 11–32px, 웨이트: 400/500/600/700/800
- 본문: 14px/Regular, 소제목: 16–18px/SemiBold, 헤딩: 20–32px/Bold
- 자간: 헤딩 `-0.02em~-0.03em`, 본문 기본값
- 행간: 헤딩 `1.2`, 본문 `1.5`, 느슨한 설명 `1.75`

### 스페이싱
- **4px 베이스 그리드** (4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80px)
- 화면 수평 패딩: `20px`
- 컴포넌트 내부 패딩: `12–18px`

### 반경 (Border Radius)
- 버튼/인풋: `8px(sm)`, `12px(md)`
- 카드/모달: `16px(lg)`, `20px(xl)`, `24px(2xl)`
- 알약형(필터 태그, 배지): `9999px(full)`

### 그림자
- 카드 기본: `shadow-sm` (0 2px 8px rgba(0,0,0,0.08))
- 모달/바텀시트: `shadow-lg` (0 8px 32px rgba(0,0,0,0.12))
- 오렌지 FAB: `0 4px 12px rgba(255,98,0,0.35)` (컬러 그림자)

### 인터랙션
- **호버/탭**: `opacity: 0.75` + 트랜지션 `150ms ease`
- **활성 상태**: 오렌지 테두리 + 오렌지 50 배경
- **전환 애니메이션**: `250ms cubic-bezier(0.4, 0, 0.2, 1)`

### 레이아웃 고정 요소
- 상단 StatusBar: 54px, NavBar: 52px
- 하단 TabBar: 83px (홈 인디케이터 포함), BottomActionBar: 54px+34px

---

## CONTENT FUNDAMENTALS

- **언어**: 한국어 메인, 영어 보조 (기술 용어)
- **톤**: 친근하고 간결함. 과도한 경어 지양, "해요체" 사용
- **호칭**: 사용자를 "님"으로 호칭, 직접 서술 선호 ("예약하기", "확인하기")
- **숫자**: 한국식 통화 표기 `₩000,000`, 날짜 `YYYY.MM.DD(요일)`
- **이모지**: 사용하지 않음
- **CTA**: 짧고 명확하게. "지금 예약하기", "결제하기", "확인"

---

## ICONOGRAPHY

- **스타일**: Outline(2px stroke), 24×24px 기본
- **컬러**: 활성 상태 `var(--color-primary-500)`, 비활성 `var(--text-tertiary)`
- **CDN 미사용**: 인라인 SVG 직접 포함 방식 사용
- **주요 아이콘**: home, search, heart, user, map-pin, calendar, arrow-left, chevron-right, plus, check, bell, credit-card

---

## COMPONENTS

| 컴포넌트 | 위치 | 설명 |
|---|---|---|
| Button | `components/core/buttons.card.html` | 6종 variant, 5가지 size |
| Input | `components/core/inputs.card.html` | 상태별(기본/포커스/에러), 아이콘 포함 |
| Badge / Tag | `components/core/badges.card.html` | 상태 배지, 필터 태그, 정보 칩 |
| Card | `components/core/cards.card.html` | 여행 카드, 리스트 아이템 |
| TabBar | `components/navigation/tabbar.card.html` | 5탭 + 중앙 FAB 스타일 |
| NavBar | `components/navigation/navbar.card.html` | 뒤로가기 + 타이틀 + 액션 |

---

## UI 킷 화면

| 화면 | 파일 | 설명 |
|---|---|---|
| 홈 | `ui_kits/app/01_home.html` | 메인 홈, 검색, 배너, 추천 |
| 검색 | `ui_kits/app/02_search.html` | 검색 결과, 필터, 지도 전환 |
| 상세 | `ui_kits/app/03_detail.html` | 숙소 상세, 편의시설, 예약 정보 |
| 결제 | `ui_kits/app/04_payment.html` | 예약 정보 입력, 결제 수단 |
| 마이페이지 | `ui_kits/app/05_mypage.html` | 내 정보, 예약 내역, 설정 메뉴 |
| 리뷰 | `ui_kits/app/06_review.html` | 리뷰 목록, 평점, 리뷰 작성 |
| 예약 완료 | `ui_kits/app/07_confirmation.html` | 예약 완료 확인, 티켓 |
