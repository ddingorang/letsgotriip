# 01. UI/UX 개선 findings (병렬 에이전트 3 + 종합)

3개 분석 에이전트(general-purpose)가 화면을 영역별로 검토한 결과를 종합. 모두 기존 디자인
토큰(`--color-*`, `--radius-*`, `--shadow-card`)·패턴 재사용 기준, 데모 품질 향상 + 저위험.

## 가장 큰 테마
1. **가짜데이터/스텁 티(demo 신뢰도 직격)** — 하드코딩 값·동작 안 하는 버튼·alert 스텁.
2. **빈/로딩 상태 누락** — 빈 화면이 "고장난 것처럼" 보임.
3. **폼/인증 기본기** — `<form>` 미사용, 자동완성/엔터제출 약함, 비번 표시 토글 없음.
4. **AI 어시스턴트 첫인상** — 빈 채팅에 추천 프롬프트 칩 없음, 실패 시 재시도 없음.
5. **마이크로 인터랙션/접근성** — :active 눌림, aria-label, alt, 이미지 폴백.

## 채택해 구현하는 항목 (P0/P1, 저위험)

### 가짜데이터·스텁 제거
- PlaceDetailView: "다시 추천" `alert()` 스텁 제거/대체, "담기" 결과라벨 1.5s 후 리셋
- HotplaceDetailView: 하드코딩 `사진 1/24` 제거, 동작 안 하는 "길찾기" 버튼 정리
- CommunityView: 하드코딩 "도보 12분" 제거, 죽은 북마크/정렬 버튼 정리, 공유게시판 빈상태
- GroupsView: `사용자 #{id}` → 닉네임, 숫자 아바타 → 이니셜
- ChatRoomListView: 리터럴 "최 0" → "읽음"

### 빈/로딩 상태
- ChatRoomListView(방 0개), ChatRoomView(빈 스레드), PostDetailView(스켈레톤), HomeView("뜨는 여행지" 스켈레톤)

### 폼/인증
- Login/Signup: `<form>` + submit 버튼(자동완성/엔터), 비번 표시 토글, 유효성 기반 버튼 비활성
- ProfileEdit: 닉네임 인라인 검증 / PreferenceSurvey: 선택 개수 표시 / Notifications: 재시도

### AI/이미지/모션
- AssistantView: 추천 프롬프트 칩 + 에러 재시도 + enterkeyhint
- AiResultView: 장소 썸네일에 attraction 이미지 사용
- SearchView: 썸네일 이미지 폴백 아이콘 / CompanionDetail: 빈 아바타 person 아이콘
- 공통: 주요 CTA `:active` 눌림, aria-label 보강
- PlanView: 펼침 트랜지션 `max-height:600px` → 콘텐츠 잘림 수정

## 구현 방식
- 파일 단위로 3개 클러스터(A 가짜데이터·코어상세 / B 채팅·소셜·게시글 / C AI·인증·온보딩)로 분할,
  **병렬 구현 에이전트**가 disjoint 파일을 담당. PlanView/HomeView는 메인에서 직접 처리.
- 구현 후 전체 빌드 1회 + codex로 diff 검수 → `02-codex-review.md`.
