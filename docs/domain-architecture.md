# 관통 여행 — 도메인 아키텍처

> 기능명세서(12개 대분류 · 47개 중분류 · 84개 소분류, 총 27.3주 규모)를
> 메뉴 단위가 아닌 **도메인 경계** 기준으로 재정리한 문서.
> 개발 패키지·DB·API 설계의 기준 문서로 사용한다.

## 도메인 요약

| 도메인 | 포함 기능 ID | 핵심 책임 | 주요 대상 |
|---|---:|---|---|
| 1. 사용자·인증 | 1, 8.1 | 회원가입, 로그인, 토큰, 비밀번호 재설정, 프로필, 계정 삭제 | 비회원, 회원 |
| 2. 관광 콘텐츠 탐색 | 2, 7 | 관광지·축제 검색, 필터, 지도/목록, 상세, 사진 갤러리 | 비회원, 회원 |
| 3. 여행 계획 | 4, 7.3.2, 12.3.1 | 일자별 일정 생성, 장소 추가, 저장, 공유, 경로 지도 | 회원 |
| 4. AI 추천 | 5, 8.2, 8.4.2 | 조건 기반 추천, 취향 반영, AI 일정 초안, 계획 담기 | 회원 |
| 5. 여행 평가·최적화 | 6 | 계획 비교, 예산, 이동 시간, 비효율 탐지, 개선 제안, 리포트 | 회원 |
| 6. 여행 맥락 정보 | 3, 5.3, 6.4 | 날씨, 일출/일몰, EV 충전소, 뉴스, 운영정보, 참고 링크 | 비회원, 회원 |
| 7. 데이터 수집·STT | 8.2, 8.3, 8.4 | 취향 설문, 카톡/통화 업로드, STT, 개인정보 마스킹 | 회원 |
| 8. 커뮤니티·UGC | 9 | 게시판 CRUD, 댓글, 좋아요, 핫플 등록·조회 | 비회원, 회원 |
| 9. 동행·그룹·단체할인 | 10 | 동행 모집·신청, 그룹 생성·권한, 그룹 퀘스트, 단체할인 | 회원 중심 |
| 10. 마이페이지·기록·게임화 | 11 | 여행 스토리, 앨범, 뱃지, 퀘스트, 챌린지, 보상 | 회원, 관리자 |
| 11. 체크리스트 | 12 | 준비 체크리스트, 템플릿, 일자/장소 연결, 알림 | 회원 |
| 12. 운영·관리 | 3.3, 9.2 승인, 11.3 | 뉴스 크롤링, 핫플 승인, 공지 CRUD, 운영 데이터 | 관리자 |

## 도메인 상세

### 1. 사용자·인증
회원 식별과 접근 제어를 담당하는 기반 도메인. 다른 모든 회원 기능의 전제 조건.

- **기능**: 회원가입, 로그인/로그아웃, 비밀번호 재설정, 프로필 입력/수정, 개인정보·서비스 데이터 삭제
- **엔티티**: `User`, `Credential`, `EmailVerification`, `AuthToken`, `PasswordResetToken`, `UserProfile`, `DataDeletionRequest`

### 2. 관광 콘텐츠 탐색
공공 API(TourAPI, Kakao Maps, 관광사진 API) 기반 공식 콘텐츠 도메인.
여행 계획·AI 추천·축제 담기의 **원천 데이터** 역할.

- **기능**: 지역 선택, 콘텐츠 유형 필터, 지도 핀, 목록 페이지네이션/정렬, 관광지 상세, 추천 사진 갤러리, 3D 카드 갤러리, 축제/행사 검색·핀·상세
- **엔티티**: `Region`, `TouristContent`, `ContentType`, `TouristPhoto`, `Festival`, `MapMarker`, `SearchFilter`

### 3. 여행 계획 (핵심 도메인)
`TripPlan → TripDay → TripPlace` 구조가 가장 중요한 모델.
추천·평가·커뮤니티 공유·앨범·체크리스트가 모두 이 데이터를 참조한다.

- **기능**: 일자 생성/수정, 장소 추가/삭제/순서 변경, 계획 저장·목록·상세, 공유게시판 게시, 공유 링크, 순서 기반 경로 지도, 축제 일정 담기, 체크리스트 연결
- **엔티티**: `TripPlan`, `TripDay`, `TripPlace`, `PlanRoute`, `PlanVersion`, `ShareLink`, `PlanVisibility`, `PlanChecklistLink`

### 4. AI 추천
지역·기간·예산·동행인·관심사 기반 일정 초안 생성. LLM API + TourAPI + 축제 API 결합,
TourAPI 결과를 컨텍스트로 제공해 환각을 줄이는 **RAG 방식**.
탐색 + 취향 + 계획 도메인을 조합하는 **응용 도메인**.

- **기능**: 추천 조건 폼, 요청 이력 저장/불러오기, 일자별 초안 생성, 계획에 담기, 축제 추천 카드, 운영 정보, 참고 링크, 취향 기반 개인화
- **엔티티**: `RecommendationRequest`, `RecommendationResult`, `RecommendationHistory`, `PreferenceProfile`, `AIItineraryDraft`, `RecommendedPlace`, `RecommendationContext`

### 5. 여행 평가·최적화
작성된 계획의 품질을 판단·개선하는 **분석 도메인** (작성 기능 아님).

- **기능**: 비교 대상 선택·결과 요약, 예산 산출/보정, 이동 시간 추정, 비효율 구간 탐지, 순서 변경/대체 제안, 관광지별·일자별 리포트
- **엔티티**: `PlanComparison`, `BudgetEstimate`, `TravelTimeEstimate`, `RouteInefficiency`, `OptimizationSuggestion`, `PlaceReport`, `DayReport`

### 6. 여행 맥락 정보
외부 API 의존도가 높음 → 내부 핵심 로직과 분리해 `integration`/`context` 계층으로 분리 권장.

- **기능**: 기간별 날씨, 일출/일몰, 주변 EV 충전소, 뉴스 수집 배치·목록, 운영 정보, 참고 링크, 리포트 보조 정보
- **엔티티**: `WeatherForecast`, `SunriseSunsetInfo`, `EVChargingStation`, `TravelNews`, `ExternalReferenceLink`, `OperationInfo`

### 7. 데이터 수집·STT
개인정보 리스크가 큼 → 추천 도메인과 직접 섞지 말고 **별도 수집·처리 파이프라인**으로 분리.

- **기능**: 취향 설문, 카카오톡 대화 텍스트 업로드, 통화 녹음 업로드, STT 변환·상태 표시, 텍스트 검수/수정, 개인정보 마스킹, 원본 삭제
- **엔티티**: `PreferenceSurvey`, `UploadedTextFile`, `UploadedAudioFile`, `STTJob`, `STTTranscript`, `TranscriptRevision`, `MaskingResult`

### 8. 커뮤니티·UGC
탐색 도메인(공공 API 공식 콘텐츠)과 달리 **사용자 생성 콘텐츠** — 신뢰도·승인 정책이 다르므로 분리.

- **기능**: 게시글 CRUD·목록·검색, 댓글/대댓글, 좋아요, 핫플 등록(지도 위치 선택), 핫플 지도/목록/상세
- **엔티티**: `Post`, `Comment`, `Like`, `Tag`, `Hotplace`, `HotplacePhoto`, `HotplaceApproval`

### 9. 동행·그룹·단체할인
권한·신청 상태·멤버십·모집 마감 등 상태 관리가 많아 커뮤니티와 별도 도메인으로 분리.

- **기능**: 동행 모집 글, 참여 신청, 승인/거절, 그룹 생성/초대, 멤버 권한, 그룹 이벤트·퀘스트, 단체할인 게시/조회, 문의 연결
- **엔티티**: `CompanionPost`, `CompanionApplication`, `TravelGroup`, `GroupMember`, `GroupRole`, `GroupEvent`, `GroupQuest`, `GroupDiscount`

### 10. 마이페이지·기록·게임화
뱃지·퀘스트는 여러 도메인의 활동 이벤트를 받아야 함 → **내부 이벤트 기반 설계** 권장
(예: "계획 저장", "게시글 작성", "핫플 등록" 이벤트가 게임화 도메인으로 전달).

- **기능**: 여행 전/후 스토리, 앨범 생성/공유, 뱃지 목록/상세, 퀘스트/챌린지 참여, 활동 기반 진행 갱신, 공지 조회
- **엔티티**: `TravelStory`, `Album`, `AlbumPhoto`, `Badge`, `Quest`, `Challenge`, `ActivityEvent`, `NoticeReadStatus`

### 11. 체크리스트
독립 기능처럼 보이지만 실제 가치는 여행 계획과 연결될 때 발생 → `TripPlan`과 **느슨하게 연결**.

- **기능**: 항목 추가/수정/삭제/완료, 기본 템플릿, 사용자 정의 템플릿, 일자/장소 연결, 여행 당일 알림
- **엔티티**: `Checklist`, `ChecklistItem`, `ChecklistTemplate`, `ChecklistCategory`, `ChecklistPlanLink`, `ChecklistReminder`

### 12. 운영·관리
사용자-facing 기능과 분리. 크롤링·승인·공지·운영 로그는 **관리자 백오피스** 성격.

- **기능**: 공지 CRUD, 뉴스 크롤링 대상 관리·수집 배치, 핫플 승인, 비속어/신고/콘텐츠 관리, 외부 API 키·호출 상태 관리
- **엔티티**: `AdminUser`, `Notice`, `NewsCrawlingJob`, `CrawlSource`, `ModerationQueue`, `ApprovalRequest`, `ExternalApiUsageLog`

## MVP 단계 구분

> ── 현행 구현 상태 (2026-06-19) ──────────────────────────────────────────────────
> 본 단계 구분은 기획 시점의 로드맵입니다. 실제 코드는 이를 앞질러, **1차 확장(커뮤니티·핫플·축제·앨범)**과
> **2차 확장의 동행·채팅까지 이미 구현·가동 중**입니다. 따라서 아래 표의 '확장' 표기는 스코프 의도이며,
> 구현 현황과 다릅니다(아래 '구현 상태' 열 참조).
>
> - **구현됨(컨트롤러·서비스·DB까지)**: 사용자·인증, 관광 탐색(attraction), 여행 계획(plan), AI 추천(recommend),
>   커뮤니티·UGC(community + 핫플), 동행(companion), 채팅(chat), 축제(festival), 취향설문/STT(preprocessing).
> - **부분/데모**: 마이페이지·앨범(앨범 API 연동, 계획↔앨범 매핑은 백로그), 여행 평가·최적화(계획 동선 리포트로 일부 구현).
> - **미구현(백로그)**: 그룹·단체할인, 게임화(뱃지/퀘스트/챌린지), 운영 백오피스(공지/승인 UI), 체크리스트 BE,
>   여행 맥락 정보(날씨/일출·일몰/EV/뉴스). 종합 판정은 `../ANALYSIS.md §10` 참조.
> ─────────────────────────────────────────────────────────────────────────────

| 단계 | 포함 도메인 | 이유 | 구현 상태(2026-06-19) |
|---|---|---|---|
| **MVP 필수** | 사용자·인증, 관광 콘텐츠 탐색, 여행 계획, AI 추천, 데이터/취향 일부 | 기본 흐름: 가입 → 탐색 → 추천 → 계획 저장 | ✅ 구현됨 |
| **MVP 보강** | 여행 평가·최적화, 체크리스트, 여행 맥락 정보 일부 | 계획 품질·실사용성 강화 | ⚠️ 평가·최적화 일부(동선 리포트), 체크리스트/맥락정보 백로그 |
| **1차 확장** | 커뮤니티·UGC, 축제/행사, 마이페이지·앨범 | 체류·콘텐츠 축적 | ✅ 커뮤니티·핫플·축제 구현됨 / ⚠️ 앨범 부분 |
| **2차 확장** | 동행·그룹·단체할인, 게임화 고도화, 운영 백오피스 | 네트워크 효과·장기 운영 | ✅ 동행·채팅 구현됨 / ❌ 그룹·게임화·백오피스 백로그 |

## 개발 패키지 구조 (권장)

```text
travel-platform
├─ auth                  # 도메인 1 (인증)
├─ user                  # 도메인 1 (프로필)
├─ travel-content        # 도메인 2
├─ travel-context        # 도메인 6
├─ trip-plan             # 도메인 3
├─ trip-recommendation   # 도메인 4
├─ trip-evaluation       # 도메인 5
├─ ingestion-stt         # 도메인 7
├─ community             # 도메인 8
├─ companion-group       # 도메인 9
├─ gamification          # 도메인 10
├─ checklist             # 도메인 11
├─ admin                 # 도메인 12
└─ external-integration  # 외부 API 어댑터 공통
```

## 관련 문서

- 디자인 시스템·화면 매핑: [design/design-system.md](design/design-system.md)
- UI 프로토타입: `frontend/public/design/ui_kits/app/index.html` (dev 서버에서 `/design/ui_kits/app/index.html`)
