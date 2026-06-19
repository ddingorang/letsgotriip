# 관통 여행(Triip) — 워크플로우 구현 상태 (됨/안됨)

> ⚠️ **OUTDATED 배너 유지(역사적 맥락)** — 아래 §1~§5 상태표는 **2차 수정(2026-06-19) 반영본**으로
> 갱신되었습니다. 단, 본 문서는 2026-06-18 라이브 테스트를 기점으로 작성된 스냅샷이며, 일부 라이브
> 호출 결과(§2)는 수정 전 기록입니다. 결함 #1~#29의 종합 판정은 `ANALYSIS.md`(§10, 2026-06-19)를
> 기준으로 삼으세요. 본 문서는 화면/시나리오 단위 현황 추적용입니다.

> 기준: `tree/WORKFLOW.md`(Figma 45화면 IA·전이도·시나리오)
> 대상: `trip-fe`(통합 FE) + `springaitrip/BE`(Spring Boot, 9090)
> 방법: BE 라이브 API 호출 테스트 + codex 4종 워크플로우 검사 + 코드 대조
> 최초 작성: 2026-06-18 · 상태표 갱신: 2026-06-19(2차 수정 반영)

## 범례
- ✅ **동작**: BE 연동까지 라이브 검증됨
- ⚠️ **부분**: 화면/일부만 동작 (mock 데이터이거나 표시 버그)
- ❌ **깨짐**: 호출 시 에러(500/403 등) 또는 미구현
- 🔌 **BE만**: BE 엔드포인트는 있으나 FE가 아직 호출 안 함

---

## 1. 시나리오별 한눈 요약 (WORKFLOW.md §5)

| # | 시나리오 | 상태 | 비고 (2026-06-19 기준) |
|---|---|---|---|
| 1 | 탐색 → 일정 (home→place-detail→plan→report) | ✅ | 관광지/장소담기 ✅, PlanView BE 배선+반응성 수정·인라인 편집(삭제/순서이동) ✅, 동선리포트(Haversine 거리+도보/차량 추정, 대체동선 적용) ✅ |
| 2 | AI 계획 (ai-planner→plan→report) | ✅ | AI 생성·결과·저장 ✅, draft→plan contentType 추론(detailCommon2) ✅. 입력+생성이 한 화면인 점은 설계상 유지(백로그 아님) |
| 3 | 커뮤니티 글 (community→post→comment/menu→editor) | ✅ | 작성/상세/댓글/좋아요 ✅, FE 표시 필드(작성자/이미지/카테고리 한글 라벨) 매핑 ✅ |
| 4 | 핫플 등록 (explore→hotspot→create→duplicate) | ⚠️ | 등록 시 status=APPROVED 자동승인→즉시 노출 ✅, 지도 핀 실 lat/lng ✅. 중복확인 화면은 여전히 없음(백로그) |
| 5 | 동행 (community→list→detail→apply→manage→chat) | ✅ | 모집/신청/취소(DELETE)/승인 ✅, isApplied·myApplicationId 서버 조회 ✅, 채팅버튼 chatRoomId 사용 ✅, 신청자 관리(message·ageGroup) ✅. tripCount·mannerScore는 통계모델 부재로 null(백로그) |
| 6 | 챌린지 (my→empty→list→chat→leave) | ❌ | BE 엔드포인트 미구현(백로그). FE 화면 일부만 |
| 7 | 마이/기록 (my→profile/album/badge/notices) | ⚠️ | 내정보/프로필수정(bio)/앨범 ✅, MyPage 통계 실데이터(/api/plans)·빈상태 정직 표시 ✅. 뱃지/챌린지/공지는 BE 미구현→0/준비중 정직 표기(백로그) |
| 8 | 인증 (gate→login→onboarding→home) | ✅ | 회원가입/로그인/refresh/내정보/탈퇴 ✅, 온보딩 취향설문 영속화(PATCH /users/me/preferences) ✅ |

---

## 2. 라이브 API 테스트 결과 (실제 호출)

### ✅ 통과 (13/13 — 핵심 흐름)
| 워크플로우 | 엔드포인트 | 결과 |
|---|---|---|
| 인증 | `POST /auth/signup` | 200 |
| 인증 | `POST /auth/login` (토큰+쿠키) | 200 |
| 인증 | `GET /users/me` | 200 |
| 인증 | `POST /auth/refresh` | 200 |
| 인증 | `PATCH /users/me` (프로필수정) | 200 |
| 탐색 | `GET /api/attractions` (+`/areas`,`/{id}`) | 200 |
| 탐색 | `GET /api/festivals` | 200 (빈 배열) |
| AI | `POST /api/recommendations` (GPT 일정생성) | 200 |
| AI | `GET /api/recommendations/{id}` | 200 |
| AI | `POST /api/recommendations/{id}/save-plan` | 200 |
| 계획 | `GET /api/plans`, `POST /api/plans` | 200/201 |
| 커뮤니티 | `GET /community/posts`, `POST /community/posts` | 200/201 |
| 커뮤니티 | 댓글/좋아요/상세 | 200/201 |
| 마이 | `GET /users/me/albums` | 200 |
| 이미지 | `POST /community/images` (멀티파트 업로드) | 200 |

### ❌ 실패 → ✅ 해소 (2026-06-18 라이브 테스트 시점 버그, 이후 수정)
> 아래는 2026-06-18 라이브 호출 당시의 실패 기록입니다. 이후 커밋으로 모두 해소되었습니다(아래 "해소" 열 참조).

| 엔드포인트 | 당시 코드 | 당시 원인 | 해소 |
|---|---|---|---|
| `POST /companion/posts` (동행 모집글) | 500 | `Column 'chat_room_id' cannot be null` | ✅ 저장 시 ChatRoom 선생성·연결 |
| `POST /community/hotplaces` (핫플 등록) | 500 | `category` NOT NULL인데 DTO에 필드 없음 | ✅ `HotPlaceCreateRequest`에 category 추가(이미 수정됨, ANALYSIS §7 확인) + 등록 시 status=APPROVED 자동승인으로 즉시 노출 |
| `POST /api/festivals/sync` (축제 동기화) | 403 | 미인증 접근 차단(=의도된 401/403 동작) | 정책상 인증 필요 동작(반증, ANALYSIS §5-3). 축제 데이터는 인증된 sync 배치 1회 실행 후 채워짐 |

---

## 3. 화면별 상태 (WORKFLOW.md IA 기준)

### 🔐 인증·온보딩
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 로그인 게이트 | 라우터 가드 | ✅ |
| 로그인 | LoginView | ✅ |
| 취향 설문(온보딩) | PreferenceSurveyView | ✅ PATCH /users/me/preferences 실제 저장 |

### 🏠 홈·탐색·장소
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 홈 | HomeView | ⚠️ 일부 정적/mock |
| 탐색 목록 | ExploreView | ✅ 관광지 실데이터 (죽은 코드 제거됨) |
| 탐색 지도 | (없음) | ❌ 미구현 |
| 장소 상세 | PlaceDetailView | ✅ ‘일정에 담기’ BE 연동 |

### 🗺️ 계획·AI
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| AI 일정 생성 | AiInputView | ✅ (단, 입력+생성 한 화면) |
| AI 결과 | AiResultView | ✅ save-plan 연동 |
| 계획 상세 | PlanView | ✅ planStore(BE) 배선 + storeToRefs 반응성 수정 + 인라인 편집(삭제/장소 위아래 이동/장소 삭제) |
| 동선 리포트 | PlanReportView | ✅ Haversine 직선거리 + 도보/차량 시간 추정(좌표없으면 '추정불가' 정직표기), '대체 동선 적용'(nearest-neighbor 재정렬 후 replacePlaces 저장) |
| 계획 없음(빈상태)→새여행 | (없음) | ❌ WORKFLOW.md에서도 미배선 화면(백로그) |

### 💬 커뮤니티
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 커뮤니티/목록/검색 | CommunityView | ✅ API + 카테고리 한글 라벨/표시 필드 매핑 |
| 게시글 상세 | PostDetailView | ✅ 작성자·이미지 필드 매핑 정규화 |
| 글쓰기 | PostWriteView | ✅ 작성. (이미지 업로드 API 전환은 별도 추적) |
| 댓글/좋아요 | PostDetailView | ✅ 좋아요 토글·댓글 삭제·작성자 닉네임 표시 |

### 📍 핫플
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 핫플 상세 | HotplaceDetailView | ✅ 미니맵 Kakao 실좌표 |
| 핫플 등록 | HotplaceRegisterView | ✅ 카카오 지도 연동 + status=APPROVED 자동승인(즉시 노출) + 목록 실데이터 |
| 중복 위치 확인 | (없음) | ❌ 미구현(백로그) |

### 🧑‍🤝‍🧑 동행
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 동행 목록 | (CommunityView 탭) | ✅ 실데이터 + status 한글 라벨 + currentMembers 표시 |
| 동행 상세 | CompanionDetailView | ✅ 신청/취소(DELETE) 실배선 + isApplied·myApplicationId 서버 조회(새로고침/중복신청 방지) + 채팅버튼 chatRoomId 사용 |
| 모집글 작성 | CompanionWriteView | ✅ 생성 + tags·estimatedCost payload 포함 |
| 신청자 관리/목록 | CompanionApplicantsView | ✅ FE 배선 + message 실데이터 + birthDate 파생 ageGroup. tripCount·mannerScore는 null(백로그) |
| 동행 채팅 | ChatRoomView/ChatRoomListView | ✅ 무의존 native WebSocket STOMP 클라이언트(api/stomp.js)+chat 스토어로 실시간 송수신, 히스토리 REST. RabbitMQ+MongoDB 구동 필요 |

### 🏅 챌린지
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 챌린지 빈상태/목록/채팅/나가기 | ChallengeDetailView 일부 | ❌ BE 엔드포인트 없음 |

### 👤 마이·기록·뱃지·공지
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 마이 | MyPageView | ✅ /api/plans 실데이터 개수·앨범 실 API·빈상태 정직 표시. 뱃지/챌린지 진행도는 BE 미구현→0/준비중 정직 표기 |
| 프로필 수정 | ProfileEditView | ✅ bio 포함 PATCH /users/me 저장 |
| 체크리스트 | ChecklistView | ⚠️ 로컬 상태(BE 미구현, 백로그) |
| 축제 상세 | (Explore/Festival) | ✅ BE /api/festivals 경유(TourAPI 직접호출 제거). 데이터는 sync 배치 1회 실행 후 노출 |
| 앨범 목록/상세 | AlbumDetailView | ⚠️ 앨범 실 API 연동. 계획↔앨범 매핑·AlbumDetail 상세는 백로그 |
| 뱃지 목록/진행/획득 | BadgesView | ⚠️ BE 미구현→준비중/0 정직 표기(백로그) |
| 공지/공지상세 | (없음) | ❌ 미구현(백로그) |
| 결제/예약확정 | PaymentView/ConfirmationView | ⚠️ FE 데모 — '데모·실제아님' 배지+주석 명시. BE 미구현(백로그) |

---

## 4. 핵심 결함 상세 & 수정 방향 (2026-06-18 기록 · 대부분 해소)

> 아래 A~G는 2026-06-18 분석 시점의 결함 상세입니다. **A·C·D·E·F·G는 2차 수정(2026-06-19)으로 해소**되었습니다
> (A 표시 필드 매핑, B `/uploads` 프록시 추가, C ChatRoom 선연결, D category DTO + 자동승인, E 인증 sync 정책 정리,
> F PlanView BE 배선, G 각 store BE 배선·축제 BE 경유). 종합 판정은 `ANALYSIS.md §10` 참조. 원문은 추적용으로 보존합니다.

### A. 게시글 표시 깨짐 (FE 필드 ≠ BE 응답) — High [해소]
| FE가 읽음 | BE 실제 | 수정 |
|---|---|---|
| `post.imageUrl` | `imageUrls[]`(상세)/`thumbnailUrl`(목록) | 매핑 |
| `post.author.nickname` | `authorNickname` | store 정규화 |
| `post.author.avatarUrl` | `authorProfileImageUrl` | store 정규화 |
| `post.location` | (없음) | 제거 |
| `post.category` = `REVIEW` | enum 영문 | 한글 라벨맵 |
| `comment.author.nickname` | `authorNickname` | store 정규화 |
| 목록 `post.content` | `PostSummaryResponse`에 content 없음 | DTO에 excerpt 추가 또는 FE 제거 |
- 본문 줄바꿈: PostDetailView 본문에 `white-space: pre-wrap` 없음 → 줄바꿈 접힘

### B. 이미지 — High
- BE는 `WebConfig.addResourceHandlers("/uploads/**")`로 서빙 ✅
- **trip-fe `vite.config.js` proxy에 `/uploads` 없음** → dev(5173)에서 업로드 이미지 404 → `/uploads` 프록시 추가 필요
- 글쓰기 이미지가 base64 data URL로 저장됨 → `/community/images` 업로드 API 사용 권장

### C. 동행 모집글 생성 500 — High (BE)
- `companion_posts.chat_room_id` NOT NULL인데 저장 시 null → 서비스에서 ChatRoom 먼저 생성·연결하거나 컬럼 nullable화

### D. 핫플 등록 500 — High (BE)
- `hot_places.category` NOT NULL인데 `HotPlaceCreateRequest`에 category 없음 → DTO에 category 추가 + 기본값/검증

### E. 축제 sync 403 — Med (BE)
- `/api/festivals/sync`가 Security에 막힘 → SecurityConfig permit 또는 관리자 권한 정책 정리

### F. AI 생성 전 ‘계획 입력 페이지’ 분리 — 요청사항
- 현재 `/ai`(AiInputView)에 입력+생성 버튼이 한 화면. 옛 흐름의 BE 연동 계획 생성/편집(`/plan`)이 빠짐
- 제안: `/ai/plan`(또는 계획 입력 라우트) 신설 → BottomNav AI 진입점을 그쪽으로 → 조건 입력 후 `/ai` 생성, 그리고 `PlanView`를 `planStore`(BE)로 배선

### G. FE 화면 다수 mock — Med
- `PlanView`(계획 CRUD), `companion store`, `festival store`(TourAPI 직접), MyPage/Badges/Checklist 정적 → 각각 store→BE 배선 필요

---

## 5. 권장 수정 우선순위 (2026-06-18 작성 · 1~4 완료)
1. ~~게시글 표시 필드 매핑 + 이미지 proxy~~ — ✅ 완료
2. ~~동행/핫플 생성 500~~ — ✅ 완료 (ChatRoom 선연결 / category DTO + 자동승인)
3. ~~PlanView BE 배선 + 인라인 편집 + 동선 리포트~~ — ✅ 완료
4. ~~축제 BE 경유 전환, 동행/마이페이지 store 배선~~ — ✅ 완료
5. 챌린지/공지/탐색지도 등 미구현 화면 — 여전히 백로그(아래 §6 참조)

---

## 6. 남은 백로그 (2026-06-19 정직 표기)

2차 수정 이후에도 미해결로 남은 항목. 과장 없이 백로그로 추적합니다.

### 기능/데이터 공백
- **동행 신청자 메타**: tripCount·mannerScore는 통계 모델 부재로 응답 null(ageGroup·message는 실데이터로 채워짐).
- **일정 표시 일부 공백**: `PlanSummaryResponseDto`에 destination/spots, `DayResponseDto`에 summary 필드가 없어 일부 표시가 비어 있음(FE 옵셔널 체이닝으로 무해).
- **축제 RecommendService 매칭**: 축제 areaCode 코드체계(legacy ↔ lDongRegnCd) cross-domain 매칭은 기존 한계로 백로그.
- **핫플 요약**: `HotPlaceSummaryResponse`에 description 필드 부재.
- **앨범**: AlbumDetail 상세, 계획↔앨범 매핑 미구현.

### BE 자체 미구현(화면은 정직 표기/데모)
- 챌린지·뱃지·퀘스트, 공지(notice), 결제(payment)/예약(booking) 컨트롤러 부재 → 해당 화면은 0/준비중/데모 배지로 정직 표기.
- 체크리스트 BE 미구현(로컬 상태).
- 중복 위치 확인·탐색 지도 화면 미구현.

### 런타임 전제
- **축제**: 데이터는 인증된 `/api/festivals/sync` 배치를 1회 실행해야 채워짐(네트워크 + TourAPI 키 필요).
- **채팅**: RabbitMQ STOMP 릴레이(61613) + MongoDB(히스토리) 구동 필요. dev에서 WebSocket은 클라이언트가 BE(:9090)로 직접 연결.
