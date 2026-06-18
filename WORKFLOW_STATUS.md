# 관통 여행(Triip) — 워크플로우 구현 상태 (됨/안됨)

> 기준: `tree/WORKFLOW.md`(Figma 45화면 IA·전이도·시나리오)
> 대상: `trip-fe`(통합 FE) + `springaitrip/BE`(Spring Boot, 9090)
> 방법: BE 라이브 API 호출 테스트 + codex 4종 워크플로우 검사 + 코드 대조
> 작성일: 2026-06-18

## 범례
- ✅ **동작**: BE 연동까지 라이브 검증됨
- ⚠️ **부분**: 화면/일부만 동작 (mock 데이터이거나 표시 버그)
- ❌ **깨짐**: 호출 시 에러(500/403 등) 또는 미구현
- 🔌 **BE만**: BE 엔드포인트는 있으나 FE가 아직 호출 안 함

---

## 1. 시나리오별 한눈 요약 (WORKFLOW.md §5)

| # | 시나리오 | 상태 | 비고 |
|---|---|---|---|
| 1 | 탐색 → 일정 (home→place-detail→plan→report) | ⚠️ | 관광지/장소담기 ✅, 계획화면(PlanView) mock, 동선리포트 없음 |
| 2 | AI 계획 (ai-planner→plan→report) | ⚠️ | AI 생성·결과·저장 ✅, **생성 전 계획입력 페이지 분리 안 됨**, 동선리포트 화면 없음 |
| 3 | 커뮤니티 글 (community→post→comment/menu→editor) | ⚠️ | API 전부 ✅(작성/상세/댓글/좋아요), **FE 표시 필드 깨짐**(작성자/이미지/카테고리) |
| 4 | 핫플 등록 (explore→hotspot→create→duplicate) | ❌ | 목록 ✅, **등록 500(category NOT NULL)**, 중복확인 화면 없음 |
| 5 | 동행 (community→list→detail→apply→manage→chat) | ❌ | 목록 ✅, **모집글 작성 500(chat_room_id NOT NULL)**, FE store mock |
| 6 | 챌린지 (my→empty→list→chat→leave) | ❌ | BE 엔드포인트 없음, FE 화면 일부만 |
| 7 | 마이/기록 (my→profile/album/badge/notices) | ⚠️ | 내정보/프로필수정/앨범 ✅, 뱃지/체크리스트/공지 정적 |
| 8 | 인증 (gate→login→onboarding→home) | ✅ | 회원가입/로그인/refresh/내정보/탈퇴 전부 검증됨 |

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

### ❌ 실패 (실제 BE 버그 — 우리 변경과 무관한 기존 결함)
| 엔드포인트 | 코드 | 원인 (BE 로그 확인) |
|---|---|---|
| `POST /companion/posts` (동행 모집글) | **500** | `Column 'chat_room_id' cannot be null` — 저장 전 채팅방 미생성/미연결 |
| `POST /community/hotplaces` (핫플 등록) | **500** | `Column 'category' cannot be null` — `HotPlaceCreateRequest`에 category 필드 없음 |
| `POST /api/festivals/sync` (축제 동기화) | **403** | Spring Security가 차단 (permit 목록/권한 누락) → 그래서 `/api/festivals`가 빈 배열 |

---

## 3. 화면별 상태 (WORKFLOW.md IA 기준)

### 🔐 인증·온보딩
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 로그인 게이트 | 라우터 가드 | ✅ |
| 로그인 | LoginView | ✅ |
| 취향 설문(온보딩) | PreferenceSurveyView | ⚠️ 화면 있음, 저장 연동 확인 필요 |

### 🏠 홈·탐색·장소
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 홈 | HomeView | ⚠️ 일부 정적/mock |
| 탐색 목록 | ExploreView | ✅ 관광지 실데이터 |
| 탐색 지도 | (없음) | ❌ 미구현 |
| 장소 상세 | PlaceDetailView | ✅ ‘일정에 담기’ BE 연동 |

### 🗺️ 계획·AI
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| AI 일정 생성 | AiInputView | ✅ (단, 입력+생성 한 화면) |
| AI 결과 | AiResultView | ✅ save-plan 연동 |
| 계획 상세 | PlanView | ⚠️ **mock 로컬 배열, planStore/BE 미배선** |
| 동선 리포트 | (없음) | ❌ 미구현 |
| 계획 없음(빈상태)→새여행 | (없음) | ❌ WORKFLOW.md에서도 미배선 화면 |

### 💬 커뮤니티
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 커뮤니티/목록/검색 | CommunityView | ⚠️ API ✅ / **카테고리 영문·본문요약 깨짐** |
| 게시글 상세 | PostDetailView | ⚠️ API ✅ / **작성자·이미지 필드 불일치로 공백** |
| 글쓰기 | PostWriteView | ⚠️ 작성 ✅ / 이미지는 base64로 전송(업로드 API 미사용) |
| 댓글/좋아요 | PostDetailView | ⚠️ API ✅ / 댓글 작성자 필드 불일치 |

### 📍 핫플
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 핫플 상세 | HotplaceDetailView | ⚠️ |
| 핫플 등록 | HotplaceRegisterView | ❌ **등록 500** |
| 중복 위치 확인 | (없음) | ❌ |

### 🧑‍🤝‍🧑 동행
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 동행 목록 | (CompanionListView 없음/CommunityView 탭) | ⚠️ store mock |
| 동행 상세 | CompanionDetailView | ⚠️ 신청이 서버 호출 없이 로컬만 |
| 모집글 작성 | CompanionWriteView | ❌ **생성 500** |
| 신청자 관리/목록 | CompanionApplicantsView | 🔌 BE 있음, FE 미배선 |
| 동행 채팅 | ChatRoomView/ChatRoomListView | ⚠️ STOMP 연동 확인 필요 |

### 🏅 챌린지
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 챌린지 빈상태/목록/채팅/나가기 | ChallengeDetailView 일부 | ❌ BE 엔드포인트 없음 |

### 👤 마이·기록·뱃지·공지
| 화면 | FE 뷰 | 상태 |
|---|---|---|
| 마이 | MyPageView | ⚠️ 일부 정적 |
| 프로필 수정 | ProfileEditView | ⚠️ 화면 있으나 PATCH 미호출(자동) |
| 체크리스트 | ChecklistView | ⚠️ 로컬 상태 |
| 축제 상세 | (Explore/Festival) | ⚠️ TourAPI 직접 |
| 앨범 목록/상세 | AlbumDetailView | ✅ 앨범 API 존재 |
| 뱃지 목록/진행/획득 | BadgesView | ❌ BE 없음, 정적 |
| 공지/공지상세 | (없음) | ❌ 미구현 |
| 결제/예약확정 | PaymentView/ConfirmationView | ⚠️ FE 고유, BE 무관 |

---

## 4. 핵심 결함 상세 & 수정 방향

### A. 게시글 표시 깨짐 (FE 필드 ≠ BE 응답) — High
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

## 5. 권장 수정 우선순위
1. **게시글 표시 필드 매핑 + 이미지 proxy** (사용자 체감 가장 큼) — FE
2. **동행/핫플 생성 500** — BE (NOT NULL 컬럼 처리)
3. **PlanView BE 배선 + AI 전 계획 입력 페이지** — FE (요청사항)
4. 축제 sync 403, 동행/마이페이지 store 배선
5. 챌린지/공지/동선리포트/탐색지도 등 미구현 화면 (백로그)
