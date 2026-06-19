# Triip 최종 완성도 재감사 — GAPS-FINAL

작성일 2026-06-19. 읽기 전용 코드 근거 기반(file:line). 경로는 저장소 루트 기준.

---

## TOP priorities (우선 처리 순)

1. **신규 페이지 6종 진입 동선 누락** — BottomNav/메뉴에 stories·groups·checklist·analysis·badges 링크 없음(라우트만 존재). 사용자가 도달 불가.
2. **게임화 실제 적립 로직 전무** — 포인트/EXP/뱃지 영속 테이블 없음, 읽기 시점 파생만. 어떤 도메인도 적립을 호출하지 않음.
3. **BadgesView 퀘스트 전부 하드코딩** — 진행률(3/5 등)·XP 보상이 가짜 정적 배열. BE 퀘스트/보상 적립 자체가 없음.
4. **체크리스트-알림 미연결(스펙 명시 항목)** — ChecklistService가 알림 이벤트를 발행하지 않음.
5. **축제-계획 연결 부재** — ExploreView 축제 행이 클릭/상세/계획추가 모두 불가(표시 전용). 축제 상세 라우트 없음.
6. **운영·관리(공지·핫플 승인) FE 부재** — 관리자 엔드포인트 다수 존재하나 호출하는 UI가 전혀 없음(unreachable).
7. **통합검색 부재** — 관광지 한정 검색만 존재, 도메인 횡단 통합검색 없음.

---

## [높음]

- **신규 페이지 진입 동선 누락** — `frontend/src/components/common/BottomNav.vue:3-30`(고정 5탭: home/explore/plan/community/mypage), `frontend/src/router/index.js:46-85`. stories/groups/checklist/analysis/badges 라우트가 등록돼 있으나 FE 전역에서 이 경로로의 `push`/`RouterLink`가 0건(grep 확인). assistant/documents만 MyPage에서 진입 가능(`MyPageView.vue:206,216`). → MyPage 메뉴 또는 홈에 진입 카드/링크 추가.

- **게임화 적립 로직 미구현(스텁)** — `BE/.../user/entity/User.java:82-83`(`// 게임포인트`, `// 게임 통계` 주석만, 필드 없음). `BE/.../gamification/service/GamificationService.java:17,21`(`@Transactional(readOnly=true)`, "별도 이벤트 추적 없이 … 읽기 시점에 파생"). 포인트/EXP/뱃지 영속 엔티티·테이블 없음. PlanService 등 어디서도 적립 호출 없음. → UserBadge/Point 엔티티 + 행동 이벤트 적립 훅 도입.

- **BadgesView 퀘스트 하드코딩** — `frontend/src/views/BadgesView.vue:150-198` `const quests = [...]` 3개 정적 객체(진행률 3/5·8/10·1/3, XP 보상 라벨 전부 가짜). 뱃지/챌린지는 gamiStore(API)에서 오지만 퀘스트만 가짜. → 퀘스트 BE 신설 또는 화면에서 제거.

- **체크리스트-알림 미연결** — `BE/.../checklist/service/ChecklistService.java`(전체에 NotificationEvent/eventPublisher 호출 0건). 비교: community(`CommunityService.java:145,186,211`)·companion(`CompanionService.java:170,234,253`)은 알림 발행. → 미완료/임박 항목 리마인더 또는 상태변경 알림 발행 추가.

- **축제-계획 연결 부재** — `frontend/src/views/ExploreView.vue:144-157` 축제 행에 `@click`/네비게이션 없음(표시 전용). 라우터에 festival 상세 경로 없음(`router/index.js` 내 festival 0건). PlaceDetail의 addToPlan(`PlaceDetailView.vue:337`)은 관광지 contentId 기준이라 축제 연계 안 됨. → 축제 클릭→상세 또는 직접 "계획에 추가" 지원.

- **운영·관리 FE 전무(관리자 UI 없음)** — 다음 BE 엔드포인트를 호출하는 화면이 없음:
  - 공지 관리 `POST/PUT/DELETE /api/notices` (`NoticeController`); `noticeApi.create/update/remove`는 `api/index.js:36-42`에 정의됐으나 어떤 view도 미사용(list만 `stores/notification.js:125`에서 사용).
  - 핫플 승인 `GET /community/hotplaces/pending`, `POST .../approve`, `POST .../reject` (`HotPlaceController`) — FE 래퍼·호출 모두 없음(`approve/pending/reject` grep 0건).
  → 최소 관리자 화면 또는 데모면 스펙에서 제외 명시.

- **통합검색 부재** — 검색은 `AttractionController.java:30` `search`(관광지 한정)만. FE에 `/search`·통합검색 진입 0건. 커뮤니티/동행/축제/스토리 횡단 검색 없음. → 통합검색 엔드포인트+화면 또는 스코프 축소 명시.

## [중간]

- **핫플 본인 수정/삭제 FE 미노출** — `PATCH/DELETE /community/hotplaces/{id}` 존재하나 `hotplaceApi`는 list/detail/create만 래핑. HotplaceRegisterView는 등록만 가능. → 상세에서 본인 글 수정/삭제 노출.

- **그룹 퀘스트/공유목표 부재** — `BE/.../group/`에 quest/goal/milestone 엔티티·필드 없음(`TravelGroup`: id/owner/name/desc/maxMembers만). 스펙의 "그룹 퀘스트" 미구현. → 공유 목표/진행 엔티티 추가 또는 스펙 축소.

- **그룹 권한(OWNER) 강제 미흡** — `GroupMember.java:16-17`에 ROLE 상수 존재, leave 시 owner 차단(`GroupService.java:111`)뿐. 그룹 수정 등 owner 전용 엔드포인트/검증 없음(`TravelGroup.update()`는 있으나 노출 안 됨). → owner 전용 수정 엔드포인트+검증.

- **알림 발행 도메인 2/6+** — community·companion만 발행. plan(공유/수정), chat(메시지), group(가입/탈퇴), checklist는 사용자 이벤트가 있음에도 알림 미발행. → 핵심 이벤트(특히 plan 공유, chat) 알림 추가.

- **알림 실시간 푸시 없음** — SSE/WebSocket 전무(`SseEmitter`/stream grep 0건), 폴링(`GET /notifications/unread-count`)만. (채팅은 STOMP 존재하나 알림은 별개) → SSE 또는 STOMP 알림 채널 검토.

- **PlaceDetail 리뷰/추천 가짜** — `PlaceDetailView.vue:331-336` `STATIC_REVIEWS` 정적 배열(리뷰 API 없음), `getRecommendation()`(약 388줄)은 `alert()` 스텁. → 리뷰 BE 연동 또는 섹션 제거, 추천 버튼 실제 연결.

- **앨범 상세 하드코딩** — `frontend/src/views/AlbumDetailView.vue:67-74` 정적 `albums` 배열, API import 없음. BE AlbumController는 실제 존재하므로 FE가 BE를 안 씀(불일치). → `albumApi`로 실데이터 연동.

## [낮음]

- **결제/확정 데모(허용 가능)** — `PaymentView.vue:142-195`, `ConfirmationView.vue:98-136` 전부 하드코딩이며 화면 상단에 "데모/목업" 주석 명시. PG·예약저장 BE 없음 → 의도된 데모로 수용 가능.

- **뉴스/EV충전소/날씨 컨텍스트** — `ContextController`(news/ev-stations/weather) 실 엔드포인트 존재, HomeView/PlaceDetail에서 호출. 외부 데이터 의존 데모성은 허용 범위.

- **STT Mock 동시 등록** — `WhisperSTTManager`(`@Primary @Component`)가 실제 동작, `MockSTTManager`(`@Component`)도 빈으로 등록(Primary가 우선). 혼선 방지 위해 Mock은 `@Profile`/`@ConditionalOnProperty`로 분리 권장. STT 자체는 실제 구현됨.

- **체크리스트 템플릿 정적** — `ChecklistTemplates.java`(domestic/overseas/camping 3종 하드코딩). MVP 수준 허용, 코드 수정 없이 확장 불가.

- **그룹 단체할인 데모** — `GroupService.java:28-39` 정적 5개(전 항목 `demo=true`). 데모로 표기돼 있어 허용 가능(동적 계산·예약 연동은 없음).

- **festival sync 수동 엔드포인트** — `POST /api/festivals/sync` FE 트리거 없음(배치/운영용). 정상.

- **관광지 다중이미지 갤러리 부재** — `BE/.../attraction`에 images/gallery/photoList 필드 없음(단일 대표 이미지만). 스펙의 "사진 갤러리 다중이미지" 미충족. → 상세 이미지 목록 API/UI 보강(낮음~중간).

---

### 참고: 정상 동작 확인된 항목
- RAG 챗봇/문서: `assistant/AssistantService.java`(Spring AI ChatClient+QuestionAnswerAdvisor+VectorStore), `rag/IngestionService.java`(청킹·임베딩·VectorStore 저장) — 실제 구현.
- STT/카톡 업로드: `AnalysisUploadView.vue`→`analysisApi`(`api/index.js`)→`PreprocessingController` /analysis/upload/{kakao,voice} 실연동, Whisper 실 호출.
- 체크리스트 CRUD/토글, 그룹 생성/가입/탈퇴, 알림 CRUD, 스토리 CRUD, 계획 공유/비교/예산/route-report, 동행, 커뮤니티 좋아요/댓글 — 실 엔드포인트+FE 연동 확인.
- 모든 view가 라우터에 등록됨(고아 view 없음). AiInputView(확인 단계)/AiPlanInputView(입력 단계)는 중복 아님.
