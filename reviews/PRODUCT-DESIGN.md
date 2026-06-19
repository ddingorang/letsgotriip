# Triip 제품/백엔드 설계 문서

> 대상: 채팅방 설정·참여자 관리 / 제품 기능 아이디에이션 / 외부데이터 영속화 전략
> 근거는 모두 실제 코드(`file:line`)에 기반. 시그니처 스케치는 설계용이며 전체 구현이 아님.

---

## A. 채팅방 설정 + 참여자 관리

### A-0. 현재 구현 현황 (코드 기준)

도메인 구조: 채팅방은 **동행 게시글(CompanionPost)에 1:1로 종속**되어 생성된다. 독립적인 "방 만들기" 진입점은 없다.

- 방 생성: `CompanionService.createPost(...)` `BE/.../companion/service/CompanionService.java:44-86`
  - `ChatRoom` 생성 후 작성자를 `isHost=true` 멤버십으로 등록 (`:76-83`).
  - `ChatRoom.title`은 동행 게시글 제목을 18자로 잘라 사용 (`:49-51`, `ChatRoom.java:23` `length=18`).
- 멤버 추가: `CompanionService.approveApplication(...)` `:190-242` — 동행 신청 승인 시 `isHost=false` 멤버십 추가 + `participationCount++`(`:229`). 즉 **현재 유일한 입장 경로는 "동행 신청 → 방장 승인"**.

엔티티 필드 현황:
- `ChatRoom` `BE/.../chat/entity/ChatRoom.java`: `id, title(18), password(4), description(200), maxParticipants, participationCount, imageUrl, chatRoomMembershipList`.
- `ChatRoomMembership` `BE/.../chat/entity/ChatRoomMembership.java`: `userId, chatRoom, isHost(:34), joinedAt, isBanned(:41), leftAt(:44), bannedAt(:47), joinMessageId(:50)` + `leave(when)`(:54) / `isActiveMember()`(:59).

**EXISTS (이미 있음):**
- 방장(host) 개념: `ChatRoomMembership.isHost` (`:34`). 방 생성자가 host, 승인 입장자는 비-host.
- 방 나가기: `ChatService.leaveRoom()` `ChatService.java:100-116` + `DELETE /api/chat/rooms/{chatRoomId}/membership` `ChatHistoryController.java:74-81`. **소프트 탈퇴(`leftAt` 기록)**, 방장은 나갈 수 없음(400, `:107-110`). FE: `ChatRoomView.vue:237-252`, `chat.js`(disconnect), `chatApi.leaveRoom` `api/index.js:147`.
- 참여자 목록: `ChatService.getParticipants()` `ChatService.java:122-147` + `GET /.../participants` `ChatHistoryController.java:62-68` → `ChatRoomParticipantsResponse`(userId/nickname/isHost). FE: 참여자 시트 `ChatRoomView.vue:124-138`, `:220-233`.
- 강퇴 인프라 **일부**: `isBanned`/`bannedAt` 컬럼과 `isActiveMember()`의 ban 필터(`:60`)는 이미 존재 → **데이터 모델은 강퇴 대비 완료, 단 강퇴를 수행하는 API/서비스/FE 없음**.
- ⋮ 드롭다운 메뉴 + 참여자 시트 UI 골격 `ChatRoomView.vue:22-36, 123-138`.

**MISSING (없음 → 설계 대상):**
1. 방 이름 변경 API/UI (방장).
2. 알림 음소거(mute) — 엔티티/필드/엔드포인트/FE 전부 없음.
3. 참여자 강퇴(kick) — 컬럼만 있고 실행 경로 없음.
4. 참여자 초대/추가(invite/add) — 승인 외 직접 추가 경로 없음.
5. 멤버십 변경(강퇴/입장/퇴장/이름변경)의 **실시간 반영** — STOMP는 채팅 메시지(`/topic/chat.room.{id}`)만 발행하고 시스템 이벤트 채널이 없음(`ChatService.sendMessage:61-62`).
6. 방장 위임/이양(host transfer) — 방장이 나갈 수 없으므로(`:107`) 위임 없이는 방이 영구 고착.

---

### A-1. 방 설정 (Settings) — 구체 설계

#### (1) 방 이름 변경 — 방장만
- BE 엔드포인트: `PATCH /api/chat/rooms/{chatRoomId}` body `{ "title": "..." }` (≤18자, `ChatRoom.title length=18` 준수).
- 권한: 호출자의 멤버십 `isHost==true` 검증 후 `ChatRoom.setTitle(...)`(이미 `@Setter` 존재, `ChatRoom.java:12`).
- 주의: 방 제목은 동행 게시글에서 파생되었으므로(`CompanionService:49`) **방 제목 ≠ 게시글 제목**으로 분리 운영됨을 명시(게시글은 그대로). 양방향 동기화는 비범위.
- STOMP: 시스템 이벤트로 브로드캐스트(아래 A-3) → 모든 참여자 헤더(`ChatRoomView.vue:11`) 갱신.
- FE: ⋮ → "방 설정" 시트에 인라인 편집 input + 저장.

#### (2) 알림 음소거(mute) — 본인 기준
- 성격: 방 단위가 아니라 **참여자 본인의 개인 설정** → `ChatRoomMembership`에 컬럼 추가.
- BE 엔티티 변경: `ChatRoomMembership`에 `@Column Boolean muted`(기본 false) 추가 + `mute(boolean)` 메서드.
- 엔드포인트: `PATCH /api/chat/rooms/{chatRoomId}/membership/mute` body `{ "muted": true }` (본인 멤버십만).
- 권한: 누구나 자기 자신에 대해. 방장 불필요.
- 효과 범위: 푸시/뱃지 카운트 억제. 현재 미읽음 카운트는 미구현(`unreadCount:0` 하드코딩 `ChatService.java:89`)이므로, mute는 향후 푸시(B-3)와 결합될 때 실효. 지금은 플래그 저장 + FE 토글 표시까지.
- STOMP 불필요(개인 상태). FE: 설정 시트 토글 스위치.

#### (3) 방 나가기 — **이미 존재** (재확인)
- `DELETE /api/chat/rooms/{chatRoomId}/membership` + `ChatService.leaveRoom` (소프트 탈퇴). 변경 없음.
- 개선 권고(선택): 나갈 때 `MessageType.SYSTEM` 입퇴장 메시지 + STOMP 시스템 이벤트 발행(A-3)으로 실시간 인원수 반영. 현재는 `companionStore.fetchMyRooms()` 재조회로만 갱신(`ChatRoomView.vue:245`).

#### (4) 방장(host) 개념 — **이미 존재**, 단 **위임 추가 필요**
- 현재 `isHost` 1명 고정, 방장은 못 나감(`ChatService.java:107-110`) → 방장이 떠나려면 **호스트 이양**이 사실상 필수.
- 추가 엔드포인트: `PATCH /api/chat/rooms/{chatRoomId}/host` body `{ "newHostUserId": ... }` (현 방장만). 기존 방장 `isHost=false`, 대상 `isHost=true`를 한 트랜잭션에서 스왑. 이후 기존 방장은 일반 멤버로 나가기 가능.

---

### A-2. 참여자 관리 (설정 시트 내부) — 구체 설계

#### (1) 강퇴(kick) — 방장만
- 데이터: 이미 `isBanned/bannedAt` 보유 → 신규 컬럼 불필요.
- 엔드포인트: `DELETE /api/chat/rooms/{chatRoomId}/participants/{userId}` (= 강퇴). 대안 명시적 경로: `POST /api/chat/rooms/{chatRoomId}/participants/{userId}/ban`.
- 서비스: `ChatService.kickParticipant(chatRoomId, targetUserId, requesterId)`
  - 검증: requester가 활성 host(`getParticipants`의 권한 패턴 `:126-130` 재사용).
  - 대상 멤버십 `isBanned=true, bannedAt=now`, (선택) `leftAt=now`. 자기 자신/다른 방장 강퇴 금지.
  - `ChatRoom.participationCount--`.
  - **효과 즉시성**: 히스토리/참여자 조회는 `isActiveMember()`가 ban을 걸러내므로(`ChatRoomMembership.java:60`, `ChatHistoryController.java:50`) 강퇴 즉시 차단됨. 단 이미 열린 STOMP 구독은 끊기지 않음 → A-3 시스템 이벤트로 강퇴 대상 클라이언트가 스스로 `disconnect()`.
- FE: 참여자 시트 행(`ChatRoomView.vue:131-135`)에 방장 한정 "내보내기" 버튼 노출(`v-if="isHostViewer && !p.isHost"`), confirm 후 `chatApi.kick(roomId, userId)`.

#### (2) 초대/추가(invite/add) — 방장만
- 현 제약: 입장은 동행 승인뿐(`approveApplication`). 두 가지 설계안:
  - **안 A(권장, 적은 변경):** 방장이 닉네임/유저로 직접 추가. `POST /api/chat/rooms/{chatRoomId}/participants` body `{ "userId": ... }` 또는 `{ "nickname": ... }`. 서비스에서 정원(`maxParticipants`) 체크 후 `isHost=false` 멤버십 생성 + `participationCount++`. 동시성은 `approveApplication`의 비관락/유니크충돌 패턴(`CompanionService.java:191-228`)을 동일 적용.
  - 안 B(초대 링크): 토큰 기반 입장 — 별도 `chat_invite` 테이블 필요, 범위 큼. 후순위.
- 대상 사용자에게 알림: 기존 `NotificationEvent` 발행 패턴 재사용(`CompanionService.java:170` 참조).
- STOMP: 시스템 이벤트로 참여자 목록 갱신(A-3).
- FE: 설정 시트 "참여자 추가" → 닉네임 검색 input.

#### (3) 참여자 역할 표시
- 이미 `Participant.isHost`로 "방장" 배지 표시(`ChatRoomView.vue:134`). 추가로 본인 표시("나"), 강퇴 버튼 노출 제어를 위해 응답에 `isMe` 또는 viewer의 host 여부를 함께 내려준다.
- `ChatRoomParticipantsResponse`에 최소 변경: 최상위에 `boolean viewerIsHost` 추가(FE가 강퇴/추가/이름변경 버튼 노출 판단). `Participant`에는 `muted`(자기 자신만 의미) 정도는 선택.

---

### A-3. 실시간 반영 (STOMP 시스템 이벤트)

현재 STOMP는 채팅 메시지만 발행(`ChatService.sendMessage:61-62`, 구독 `chat.js:88`). 멤버십 변경(rename/kick/invite/leave/host-transfer)은 **별도 시스템 이벤트 페이로드**로 동일 토픽에 발행하거나 전용 서브토픽을 신설.

- 권장: 동일 토픽 `"/topic/chat.room.{id}"`에 `type: "SYSTEM"` 이벤트 발행(FE 구독 1개 유지, `chat.js:88` 그대로).
  - 페이로드 예: `{ event: "ROOM_RENAMED"|"PARTICIPANT_KICKED"|"PARTICIPANT_JOINED"|"PARTICIPANT_LEFT"|"HOST_CHANGED", chatRoomId, targetUserId?, title? }`.
  - FE `chat.js` 구독 콜백(`:88-95`)에서 `dto.event`가 있으면 메시지 목록에 추가하지 않고 별도 핸들러로 분기(헤더 제목 갱신 / 참여자 재조회 / 본인이 kick 대상이면 `disconnect()` + 라우터 back + 토스트).
  - `MessageType` enum(`TEXT, IMAGE, RECEIPT`)에 `SYSTEM` 추가하거나, 시스템 이벤트는 메시지로 저장하지 않고 브로드캐스트만(MongoDB 미저장) 하는 편이 단순. **권고: 시스템 이벤트는 브로드캐스트 전용(비영속)** + 입퇴장 안내가 대화에 남길 필요가 있으면 그때만 `SYSTEM` 메시지 영속.
- 강퇴 대상의 실연결 종료: 서버가 강제로 구독을 끊기는 복잡하므로, 클라이언트 협조 방식(이벤트 수신 → self disconnect)을 1차 채택.

---

### A-4. 추가/수정 파일 목록 (정확히)

**BE — 신규:**
- `chat/dto/ChatRoomUpdateRequest.java` (record: `String title`)
- `chat/dto/ParticipantAddRequest.java` (record: `Long userId` 또는 `String nickname`)
- `chat/dto/HostTransferRequest.java` (record: `Long newHostUserId`)
- `chat/dto/MuteRequest.java` (record: `Boolean muted`)
- `chat/dto/ChatSystemEvent.java` (record: `String event, Long chatRoomId, Long targetUserId, String title`) — STOMP 시스템 이벤트 페이로드
- `chat/controller/ChatRoomSettingsController.java` (또는 `ChatHistoryController`에 합침) — rename/kick/invite/host/mute 엔드포인트

**BE — 수정:**
- `chat/entity/ChatRoomMembership.java` — `muted` 필드 + `mute()`, `ban()`(`isBanned/bannedAt` 설정) 메서드 추가
- `chat/service/ChatService.java` — `renameRoom / kickParticipant / addParticipant / transferHost / setMuted` 추가, 각 변경 시 `messagingTemplate.convertAndSend("/topic/chat.room."+id, systemEvent)` 발행
- `chat/repository/ChatRoomMembershipRepository.java` — `findByChatRoomIdAndUserId(...)` 단건 조회 추가(현재 `findByChatRoomId` 전체 후 stream 필터, `ChatService.java:102-104`)
- `chat/dto/ChatRoomParticipantsResponse.java` — `boolean viewerIsHost`(최상위) 추가, `Participant`에 `boolean isMe`(선택)
- `chat/repository/ChatRoomRepository.java` — 필요 시 락 조회(`findByIdForUpdate`) 추가(정원 동시성)

**FE — 수정:**
- `frontend/src/api/index.js` — `chatApi`에 `renameRoom / kick / addParticipant / transferHost / setMuted` 추가
- `frontend/src/views/ChatRoomView.vue` — ⋮ 드롭다운(`:22-36`)을 "방 설정" 시트로 확장: 이름변경 input, mute 토글, (방장) 참여자별 강퇴 버튼(`:131-135`에 조건부 버튼), 참여자 추가 input, 호스트 위임
- `frontend/src/stores/chat.js` — 구독 콜백(`:88-95`)에 시스템 이벤트 분기 핸들러 추가(헤더/참여자/강퇴 처리)

---

## B. 더 있어야 하는 기능 (우선순위)

도메인 인벤토리(코드 기준): attraction, festival, plan, recommend, community(글/댓글/좋아요/핫플), companion, chat, notification, gamification, story, group, checklist, context(날씨/충전소/뉴스), assistant(RAG), document, notice, user. FE API 표면은 `frontend/src/api/index.js` 참조.

**확인된 공백:** 즐겨찾기/북마크 도메인 **없음**, 관광지 리뷰/평점 도메인 **없음**(grep으로 `favorite|bookmark|review|rating` 미존재), 알림은 적재+폴링만 있고 **실시간 푸시 없음**(`NotificationService`는 REST, STOMP 미연동 `NotificationController`).

| # | 기능 | 우선 | 노력 | 근거 / 붙는 코드 |
|---|------|:---:|:---:|------|
| B-1 | **즐겨찾기/북마크(관광지·게시글·동행)** | **High** | **M** | 핵심 여행앱 기능인데 부재. 관광지 영속화(C)와 직접 연결 — 즐겨찾기하려면 contentId를 로컬에 잡아둬야 함. `Attraction` 엔티티/`upsertSnapshot`(이미 존재)에 FK로 붙이는 `favorite` 테이블. 게시글은 `community`에 재사용. 마이페이지 진입점 존재. |
| B-2 | **관광지 리뷰·평점** | **High** | **L** | 관광지 상세(`AttractionController.getDetail :59`)에 평점/리뷰가 없음. 이미지 업로드 인프라(`communityApi.uploadImage`), 좋아요/댓글 패턴(`community`) 재사용 가능. `Attraction`을 평점 집계 대상 FK로(=C 영속화 선결). |
| B-3 | **실시간 알림 푸시(STOMP/SSE)** | **High** | **M** | 알림은 이미 적재(`NotificationEventListener` AFTER_COMMIT)되나 FE는 폴링만 가능(`notificationApi.unreadCount`). 이미 STOMP 인프라(`WebSocketBrokerConfig`) 보유 → `"/topic/user.{id}"` 개인 토픽으로 푸시하면 저비용. A-2 mute와 결합. |
| B-4 | **통합 검색(관광지+게시글+동행+축제)** | **High** | **M** | 현재 검색은 관광지 단일(`searchKeyword2` 경유 `AttractionService.search`). 사용자 관점 "한 곳에서" 검색 부재. 각 도메인 list 엔드포인트 존재 → 파사드/통합 결과 DTO. C 영속화 시 관광지도 로컬 LIKE 검색 가능해져 품질↑. |
| B-5 | **최근 본 항목(관광지/게시글)** | Med | S | 상세 조회 시 로컬 기록. 클라이언트 로컬스토리지로도 가능하나, C로 영속화하면 서버측 가능. 낮은 비용, 체감 좋음. |
| B-6 | **여행 후기 공유(스토리 공개 피드)** | Med | M | `story` 도메인이 **개인 전용**(소유자만 조회, `storyApi`). 공개 피드/좋아요로 확장하면 커뮤니티와 시너지. plan 공유 토큰(`planApi.share`) 패턴 재사용. |
| B-7 | **알림 설정(채널별 on/off)** | Med | S | 알림 type(`companion` 등)별 수신 토글. `user` 설정 + `NotificationService.create`에서 필터. A-2 mute와 동일 계열. |
| B-8 | **딥링크/공유(관광지·게시글·동행)** | Med | S | plan만 공유 토큰 보유. 관광지/게시글/동행 공유 URL + OG 메타. C 영속화로 관광지 캐노니컬 URL 안정화. |
| B-9 | **사용자 팔로우** | Low | M | 커뮤니티/스토리 활성도가 선결. 후기 공유(B-6) 이후 의미. |
| B-10 | **다국어(i18n)** | Low | L | 외국인 관광객 타깃이면 가치 크나 전면 작업. 데이터(관광지)는 한국어 TourAPI 종속 → 콘텐츠 번역 비용 큼. |

**High 4건 요약:** B-1 즐겨찾기/북마크(M), B-2 관광지 리뷰·평점(L), B-3 실시간 알림 푸시(M), B-4 통합 검색(M). 셋 중 B-1·B-2·B-4는 모두 **C(관광지 영속화)를 선결로 두면 품질·구현난이도가 동시에 개선**된다 → C 우선 추진이 레버리지가 큼.

---

## C. 외부데이터(TourAPI 관광지) 영속화 전략 — 설계 권고 (구현 X)

### C-0. 현황 비교

- **Festival**: 배치 동기화로 **DB가 source of truth**. `FestivalSyncJobConfig`(reader→processor→writer + cleanupStep). `Festival.@PreUpdate`로 `syncedAt` 갱신, Job 시작시각보다 오래된 행(미수신)은 `ENDED` 처리(`FestivalSyncJobConfig.java:91-97`), 만료 행도 `ENDED`. → **정합성/검색/필터를 DB로 수행**.
- **Attraction**: 읽기 경로(`search`/`getDetail`/`getAreas`)는 **매번 TourAPI 라이브 호출 + Redis 캐시만**(`AttractionService.java:56-157`). DB(`attractions` 테이블)는 **쓰기 경로에서만** 채워짐 — plan에 장소를 담을 때 `upsertSnapshot`(`AttractionService.java:163-238`, 호출처 `PlanService.java:260,305,420`)으로만 영속. 즉 **"플랜에 담긴 관광지"만 DB에 존재**, 일반 검색/상세로는 영구 저장되지 않음.

핵심 모순: `Attraction` 엔티티·upsert·유니크제약(`uk_attr_content`)·배치사이즈·인덱스(`idx_attractions_area`)가 **이미 완비**(`Attraction.java`)되어 있는데, 읽기 경로가 이를 채우지 않아 자산이 저활용 상태.

### C-1. 영속화(write-through 물질화)의 이점/단점

**이점**
- 속도: 라이브 TourAPI 왕복 제거(특히 detail). Redis 캐시 미스 시에도 DB 폴백 가능.
- 쿼터 절감: TourAPI 일일 호출 한도 보호(현재 모든 상세/검색이 외부 호출).
- 오프라인/장애 내성: 현재는 `stale 캐시`에 의존(`:91-99`)하나 TTL 만료 후엔 전면 실패(`EXTERNAL_API_ERROR`). DB가 있으면 항구적 폴백.
- 기능 잠금 해제: **즐겨찾기(B-1)·리뷰/평점(B-2)·통합검색(B-4)·최근본(B-5)·딥링크(B-8)** 가 모두 `Attraction` PK를 FK로 참조 가능. plan은 이미 그렇게 동작(`PlanService.upsertSnapshot`).
- 이미지 안정: TourAPI `firstimage` URL 변동/만료 대비, 로컬 메타 보존(이미지 자체 미러링은 별도).

**단점**
- 신선도: 운영시간/전화/폐업 등 변동 데이터의 staleness. `fetchedAt`(`Attraction.java:63`) 기반 TTL 관리 필요.
- 스토리지: 관광지 수만~수십만 건. `overview`가 `TEXT`(`:60`)라 행 크기 큼 → 검색용 경량 컬럼과 상세 본문 분리 고려.
- 쓰기 부하/일관성: 읽기 경로에 쓰기를 끼우면 응답 지연·동시 insert 충돌(이미 `DataIntegrityViolationException` 처리 패턴 보유 `:232-236`).

### C-2. TTL·갱신 전략

- 신선도 등급화: **상세(detailCommon2)**는 변동 가능 → `fetchedAt` + TTL(예 7~30일) 초과 시 백그라운드 갱신. **좌표/주소/제목**은 거의 불변 → 길게.
- 갱신 트리거 옵션(택1 또는 혼합):
  - lazy refresh: 조회 시 `fetchedAt`이 오래되면 응답은 즉시(DB) 반환하고 비동기로 재페치(stale-while-revalidate).
  - 배치 리프레시: festival처럼 야간 배치로 인기/즐겨찾기된 관광지만 우선 갱신(전수 동기화는 비현실적 — 지역기반 전수 크롤은 쿼터 부담).
- 소멸 처리: festival의 `status` 패턴을 차용해 `active/inactive` 또는 `deleted` 소프트 플래그(전수 동기화를 안 하므로 "사라짐 감지"는 약함 — 갱신 실패 누적으로 추정).

### C-3. Redis(@Cacheable)와의 역할 분담

- 권고 계층: **L1 = Redis(핫 캐시, 짧은 TTL)** → **L2 = DB(영구 source-of-record)** → **L3 = TourAPI(콜드/갱신)**.
  - 현재 Redis는 `StringRedisTemplate` 수동 직렬화(`AttractionService.java:70-99`). DB 도입 후엔 Redis는 "최근 응답 가속" 역할에 집중하고, **검색/즐겨찾기/리뷰 집계 같은 질의는 DB**가 담당.
  - `@Cacheable` 전환은 선택(현 수동 캐시도 동작). DB 도입과 분리해 점진 적용 가능.
- 충돌 주의: search 결과는 페이지/파라미터 조합이 많아 **검색 결과 자체를 DB로 물질화하기보다, 검색으로 등장한 개별 item을 upsert**(목록의 row를 DB에 흘려보냄)하고 목록 캐시는 Redis로 유지하는 편이 깔끔.

### C-4. 마이그레이션 난이도

- **낮음~중간.** 테이블/엔티티/유니크제약/upsert가 이미 존재(`Attraction`, `upsertSnapshot`)하므로 **스키마 신설 불필요**. 핵심 작업은 "읽기 경로에 쓰기 훅 추가" 1지점:
  - `getDetail()`(`:106-132`)에서 TourAPI 성공 시 `upsertFromItem`을 비동기로 호출.
  - `search()`(`:56-100`)에서 받은 `items`를 비동기로 개별 upsert.
- 리스크: 읽기 경로 latency 증가(→ 비동기/`@Async` 또는 after-response 처리로 회피), 동시 insert(→ 기존 `DataIntegrityViolationException` 폴백 재사용), `search`의 list item 필드가 `detailCommon2`보다 빈약(좌표/overview 누락 가능 → null 덮어쓰기 방지 로직 이미 있음 `:200-202`).

### C-5. 권고안 (1개)

> **권고:** 읽기 경로에서 **detail 우선 + search opportunistic의 비동기 write-through로 `attractions` 테이블을 점진적으로 물질화**한다 — `getDetail` 성공 시 `upsertFromItem`을 비차단으로 호출해 상세 본문을 영속화하고, `search` 결과 item은 백그라운드로 경량 upsert(좌표/제목/이미지)한다. Redis는 L1 핫 캐시로 유지하되 DB를 source-of-record로 삼아 `fetchedAt` 기반 stale-while-revalidate(상세 7~30일 TTL, 좌표/주소는 장기)로 갱신하고, 전수 배치 동기화 대신 **즐겨찾기/조회 인기 관광지만 야간 배치 리프레시**한다. 스키마·upsert·유니크제약이 이미 존재하므로 변경은 읽기 경로 훅 추가에 국한되어 난이도가 낮고, 이 영속화가 B-1(즐겨찾기)·B-2(리뷰/평점)·B-4(통합검색)의 선결 인프라가 된다.
