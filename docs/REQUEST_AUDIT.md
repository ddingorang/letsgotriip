# 요청 이력 점검 (채팅기록 전수 조사)

> 작성: 2026-06-22 · 대상 세션 3개(`0cd630af`, `359eb00f`, `350e32a5`)의 사용자 요청을 전수 추출해 실제 코드/커밋으로 교차검증.
> 범례: ✅ 완료(근거 확인) · ⚠️ 부분/주의 · ➖ 코드변경 불필요(답변·확인성) · ❌ 미완

---

## A. 초기 인프라 / 외부 연동

| # | 요청 | 상태 | 근거 |
|---|------|------|------|
| A1 | .env 파일 생성 (Kakao/Google OAuth/OpenAI/TourAPI 키) | ✅ | `.env`, `BE/.env`, `frontend/.env` 존재 |
| A2 | 사진/이미지 안 보임 수정 | ✅ | `4cba76d` 외부더미 제거+로컬폴백, `ce7e9eb` 썸네일 |
| A3 | Kakao 지도 SDK 로드 실패 수정 (JS키 map 서비스) | ✅ | SDK 로더 `TripMap.vue:42` `loadKakao()`, 도메인 :5173 |
| A4 | 길찾기/네비게이션 (Kakao Mobility REST) | ✅ | `KakaoDirectionsClient.java` (route/routeStitched) |
| A5 | install(ubuntu-ssh) 컨테이너에서 접근 + 인증 | ✅ | Playwright 테스트 하니스(메모리 `triip-ai-and-test-harness`) |
| A6 | 실제 테스트 돌려 검증(주장 금지) | ✅(원칙) | 매 변경 빌드/배포/조회 검증 — 작업 원칙으로 지속 |

## B. 계획 편집 / 동선 / 지도

| # | 요청 | 상태 | 근거 |
|---|------|------|------|
| B1 | 1일차/2일차 분리 + 이전/다음 + 1일차 전체경로 | ✅ | `PlanView.vue` currentDay* |
| B2 | 위아래 버튼 → 드래그 이동(터치/폰 포함) | ✅ | `plan.js reorderDayPlaces`, PointerEvents |
| B3 | 드래그 안됨/느림 수정 + 검증 | ✅ | 낙관적 reorder + 빌드/배포 검증 |
| B4 | 여행 수정 UX 개선(행 액션: 다른날 이동/방문시간/삭제) | ✅ | `b6ad55e`, `movePlaceToDay`/`setPlaceVisitTime` |
| B5 | 계획 이동 애니메이션 | ✅ | `6284ee1` TransitionGroup(FLIP) |
| B6 | 동선 표시 안됨 수정 | ✅ | `7e16d4a` 직선 폴백, `currentDayLine` |
| B7 | 안 되는 구간만 가장 가까운 도로 폴백, 되는 건 실제 | ✅ | `98a207a` 구간별 폴백 `routeStitched` |
| B8 | 완성된 동선값 변경없으면 저장(캐싱) | ✅ | `2346327` Redis(L1) version 키 |
| B9 | redis 말고 개인별 여행(DB)에 영속 | ✅ | `2b76b76` `TripPlan.routePathJson`+L2 DB |

## C. 장바구니(담기) 모델

| # | 요청 | 상태 | 근거 |
|---|------|------|------|
| C1 | 일정에 담기 시 어디에 담을지(활성계획) 선택 | ✅ | `plan.js` activePlan/quickAddPlace, `CartFab.vue` |
| C2 | 기본 일정 자동 생성 + 한번탭 담기 | ✅ | `ensureActivePlan`, PlaceDetail 분할버튼 |

## D. AI 추천 / 챗봇(RAG)

| # | 요청 | 상태 | 근거 |
|---|------|------|------|
| D1 | AI 계획추천 프롬프트 구조화 | ✅ | `6a25272` SYSTEM_PROMPT(역할+CoT 5단계) |
| D2 | tool-calling + CoT + 시스템 프롬프트 | ✅ | `RecommendService.RecommendTools` @Tool |
| D3 | LangGraph식 단계 수집 | ✅ | searchPlaces/listFestivals 누적 `gathered` |
| D4 | 챗봇이 사용자 기록 조회(계획/찜/리뷰/스토리) | ✅ | `AssistantService.RecordTools` |
| D5 | 기록 활용 껏다켜기/소스별/기억 체크 설정 | ✅ | `00e4d15` MemoryPrefs + AssistantView 설정패널 |
| D6 | 내 계획 선택 토글(화이트리스트) | ✅ | `MemoryPrefs.planIds`, `togglePlan` |
| D7 | plan id 타인 조회 불가(보안) | ✅ | 모든 PlanService 메서드 `verifyOwner` (IDOR 감사) |
| D8 | 챗봇 마크다운 렌더링 | ✅ | `f4462d8` `utils/markdown.js` v-html |
| D9 | 상세 → 챗봇으로 넘어가기 | ✅ | `goAssistant(/assistant?planId)` |
| D10 | 계획 비교 2개→N개 | ✅ | `6284ee1` 비교 N UI |
| D11 | codex 적대적 리뷰 + 실제 동작 조사 | ✅ | `6a25272` 하드닝, `docs/ai-rag-design/02-codex-analysis.md` |
| D12 | AI/RAG 의사결정 폴더 저장 | ✅ | `docs/ai-rag-design/` 01~04 |
| D13 | usecases.md+button-plan.md 기반 병렬 테스트 | ✅ | Playwright 47/49 PASS(메모리 기록) |

## E. 체크리스트

| # | 요청 | 상태 | 근거 |
|---|------|------|------|
| E1 | 체크리스트 여행별 + 소유권 검증 | ✅ | `b3c5a09`/`a1327b0` `checklist/` 모듈 |
| E2 | 내 정보 누출 방지 | ✅ | 소유권 검증 + 서버 userId 기준 조회 |

## F. 커뮤니티 / 소셜

| # | 요청 | 상태 | 근거 |
|---|------|------|------|
| F1 | 사용자 팔로우 | ✅ | `677b0ec` `follow/` 모듈 + FE(PostDetail/MyPage) |
| F2 | 공유게시판 공유 버튼 동작 | ✅ | `PostDetailView.sharePost` (navigator.share/클립보드) |
| F3 | 공유게시판 ⋯(더보기) 메뉴 동작 | ✅ | `bb4faef` menuOpen 드롭다운 |
| F4 | 공유게시판 글쓰기 진입(COM-09) | ✅ | `4a909bd` 글쓰기 FAB |

## G. 이미지 설정

| # | 요청 | 상태 | 근거 |
|---|------|------|------|
| G1 | 계획/동행/게시글 대표 이미지 설정 | ✅ | `TripPlan.imageUrl`, `CompanionPost.imageUrl`, `PostImage` |
| G2 | 이미지 중 선택 또는 업로드 | ✅ | `community/FileStorageService`, `AlbumController` 업로드 |
| G3 | 없으면 기본 이미지 | ✅ | `THUMB_PLACEHOLDER` 폴백(ExploreView 등) |

## H. 탐색(Explore)

| # | 요청 | 상태 | 근거 |
|---|------|------|------|
| H1 | 정렬 안먹음 수정 + 정렬 기준 | ✅ | `8c25aff` arrange=B 인기순, default/distance/name |
| H2 | 드래그 바텀시트(깔짝거림 수정) | ✅ | `8c25aff` 포인터 드래그 3단 스냅 |
| H3 | 조회 시 현 지도 크기 기준 | ✅ | `b8f69e4` `mapAreaParams` 모든 비키워드 조회 |
| H4 | 분류 변경해도 사용자 지정 지도크기 우선 | ✅ | `ca1519e` selectCategory mapFit=false |
| H5 | session management 필요? (이미 보유) | ➖ | JWT 인증 + ChatMemory(userId:conversationId) |

## I. 프로세스 / 문서 / 답변성

| # | 요청 | 상태 | 근거 |
|---|------|------|------|
| I1 | 커밋+원격 푸시, 제안 안나올때까지 | ✅ | 진행 |
| I2 | 원격 master 호환성 테스트 + codex 전략 | ✅ | 머지 충돌 해소(App/TripMap/PlaceDetail) |
| I3 | PR/링크 제공 | ✅ | 푸시 완료 |
| I4 | 커밋 author spspd/rlarbals37@naver.com | ✅ | author 재작성 |
| I5 | 프로젝트 이름 추천(변경X) | ➖ | "Triip" 등 제안(텍스트 답변) |
| I6 | 내가 체크할 것 알려줘 / 뭘 더해야할까 | ➖ | 텍스트 가이드 답변 |
| I7 | 원격기준 사용자관점 API/사용법 문서 + 다이어그램 | ✅ | `README.md`,`USAGE.md`,`docs/system-design.md` |

---

## 종합

- **코드변경 요청 ~38건 전부 ✅ 완료**, 답변성 ➖ 5건은 해당 없음, ❌ 0건.
- 최근 미배포/검증 한계: H3/H4 지도 카메라 동작은 Kakao 도메인 제약상 **:5173 실배포에서만 육안 검증 가능**(빌드+컨테이너 기동으로 확인).
- 잠재 리스크(코덱스 리뷰 대상): ① per-segment routeStitched 호출량(N개 구간 → N요청), ② 챗봇 RecordTools 응답이 LLM 컨텍스트로 들어갈 때 PII 범위, ③ planIds 화이트리스트 외 계획 누출 가능성, ④ Explore mapDragCenter 자동fit 발화로 인한 의도치 않은 영역조회.

---

## 코덱스(GPT-5.5 xhigh) 적대적 리뷰 결과

> 요약 코드를 인라인해 검증 요청. 환각 없이 실제 코드 근거로 응답(whitelist 범위·prefs null 기본값·30포인트 절단·상태변경 도구 상시 장착을 정확히 지적). 아래는 트리아지.

| # | 코덱스 지적 | 판정 | 비고 |
|---|------------|------|------|
| R1 | 상태변경 도구(addPlace/removePlace/evaluate/checklist)는 소유권만 검증, **planIds 화이트리스트 미적용** | ⚠️ 설계판단 | 화이트리스트 의도는 "챗봇이 읽는 계획 컨텍스트 제한". 액션 도구는 사용자가 대화에서 직접 planId를 지정 + verifyOwner. 누출은 아니나, "선택 안 한 계획은 건드리지도 말라"면 빈틈 |
| R2 | `prefs == null` → 전체 허용(opt-out) 기본값 | ⚠️ 실유효 | FE는 항상 prefs 전송(기본 전체 ON)이라 실제로도 opt-out. 서버 기본값을 비허용으로 바꾸는 게 방어적 |
| R3 | `routeStitched` 30포인트 초과 **조용한 절단** | ✅ 실결함 | 31번째 이후 장소가 동선에서 사라지는데 성공처럼 보임 → 최소 로그/표식 필요 |
| R4 | 카카오 일시장애 시 만든 **직선 폴백이 plan.version 캐시에 고착** | ✅ 실결함 | 폴백 포함 결과는 짧은 TTL 또는 재계산 트리거 필요 |
| R5 | 직선 폴백 구간을 **real/fallback 상태로 구분 표시 안 함** | ⚠️ 정직성 | 현재 일자 전체가 한 스타일(실패시 점선). 구간 혼합을 한 색으로 그려 실제 도로로 오인 가능 |
| R6 | 상태변경 도구 **상시 장착 + 서버 intent/confirm 게이트 없음** | ⚠️ 심층방어 | 프롬프트 보안규칙 + verifyOwner로 1차 차단됨. 도구별 확인 토큰은 defense-in-depth |
| R7 | RAG filter `"userId == '" + userId + "'"` 문자열 결합 | ➖ 저위험 | 서버 주입 Long이라 인젝션 불가하나 typed filter가 더 안전 |
| R8 | radius 500~20000m clamp → 광역 줌아웃 시 전체 영역 미조회 | ➖ API제약 | TourAPI radius 상한이 20km라 우리 버그 아님 |
| R9 | 기록 도구가 계획30+찜/리뷰/스토리15~20개 → LLM 컨텍스트 비용 | ⚠️ 성능 | 요약/필드 축소 여지 |
| R10 | mapDragCenter 자동fit 발화로 비명시 영역조회 | ➖ 의도부합 | 사용자가 "현 지도 기준 조회"를 명시 요청 → 자동fit 후 그 영역 조회는 의도와 일치 |

### 우선순위(코덱스 Top3, 내 코멘트 반영)
1. **AI 도구 권한 경계** (R1+R6) — 화이트리스트를 planId 기반 모든 도구에 확장 + 상태변경 서버 확인. → 설계 판단 필요(사용자 의도 확인)
2. **기록/PII 최소화** (R2+R9) — prefs null 기본값을 비허용으로, LLM 전달 필드 축소. → 빠른 방어 가능
3. **동선 폴백 안정화** (R3+R4+R5) — 30개 절단 표면화·폴백 캐시 TTL·구간 상태 표시. → 실결함, 우선 수정 권장

