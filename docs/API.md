# triip — API 명세

전체 엔드포인트 목록입니다. 요청/응답 스키마까지 포함한 실시간 명세는 서버 실행 후 **Swagger UI**에서 확인하세요.

| | |
|---|---|
| **Swagger UI** | http://localhost:9090/swagger-ui/index.html |
| **OpenAPI JSON** | http://localhost:9090/v3/api-docs |
| **총 경로 수** | 112 (OpenAPI 3.1 기준) |

> 프로젝트 개요·아키텍처는 [README.md](../README.md) 참고.

---

## 공통 규약

**경로 프리픽스** — 대부분 `/api/**`를 사용하되, **`/auth`, `/users`, `/community`, `/companion`, `/analysis`는 프리픽스 없이** 노출됩니다(초기 구현 경로 유지). nginx가 이 경로들을 모두 백엔드로 프록시합니다.

**응답 형식** — envelope 없이 `ResponseEntity<Dto>`를 직접 반환합니다. 에러만 아래 형태로 통일됩니다.

```json
{ "code": "PLAN_NOT_FOUND", "message": "계획을 찾을 수 없습니다." }
```

**인증** — `Authorization: Bearer <accessToken>` (1시간). refresh 토큰(7일)은 httpOnly 쿠키로 전달되며, `POST /auth/refresh`가 회전·재사용 탐지를 수행합니다.

**인증 표기**

| 표기 | 의미 |
|---|---|
| 공개 | 비로그인 접근 가능 |
| 인증 | 로그인 필요 |
| ADMIN | `ROLE_ADMIN` 필요 |
| 소유자 | 로그인 + 리소스 소유자만 (불일치 시 403) |

---

## 1. 인증 (`/auth`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/auth/signup` | 회원가입 | 공개 |
| POST | `/auth/login` | 로그인 → access 발급 + refresh 쿠키 | 공개 |
| POST | `/auth/refresh` | access 재발급 (회전 + 재사용 탐지 + 5분 overlap) | 공개(쿠키) |
| POST | `/auth/logout` | Redis 세션 삭제 + 쿠키 만료 | 공개(쿠키) |
| POST | `/auth/password/reset-request` | 비밀번호 재설정 요청 | 공개 |
| POST | `/auth/password/reset` | 비밀번호 재설정 실행 | 공개 |

> access token은 로그아웃 후에도 만료(1h)까지 유효합니다 — denylist 미도입을 명시적 정책으로 채택했습니다.

**OAuth2 (Google)** — `/oauth2/authorization/google` 진입 → 성공 시 refresh 쿠키만 심고 `{FE_URL}/oauth/callback`으로 리다이렉트 → FE가 `/auth/refresh`로 access 획득 (토큰이 URL에 노출되지 않음).

---

## 2. 사용자 (`/users`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/users/me` | 내 정보 조회 | 인증 |
| PATCH | `/users/me` | 프로필 수정 (닉네임·bio 등) | 인증 |
| DELETE | `/users/me` | 회원 탈퇴 (소프트 삭제 + 세션 정리) | 인증 |
| POST | `/users/me/profile-image` | 프로필 이미지 업로드 | 인증 |
| PATCH | `/users/me/preferences` | 온보딩 취향 설문 저장 | 인증 |

### 앨범

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/users/me/albums` | 내 앨범 목록 | 인증 |
| POST | `/users/me/albums` | 앨범 생성 | 인증 |
| POST | `/users/me/albums/images` | 앨범 사진 업로드 | 인증 |
| GET | `/users/me/albums/{albumId}` | 앨범 상세 | 소유자 |
| PATCH | `/users/me/albums/{albumId}` | 앨범 수정 | 소유자 |
| DELETE | `/users/me/albums/{albumId}` | 앨범 삭제 | 소유자 |
| POST | `/users/me/albums/{albumId}/share` | 공유 토큰 발급 | 소유자 |
| GET | `/api/albums/shared/{token}` | 공유 앨범 열람 | **공개** |

---

## 3. 관광지 · 축제 · 검색

### 관광지 (`/api/attractions`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/attractions` | 검색 — `areaCode, sigunguCode, contentTypeId, keyword, page, size` | 공개 |
| GET | `/api/attractions/{contentId}` | 상세 (Redis 캐시 6h) | 공개 |
| GET | `/api/attractions/{contentId}/images` | 이미지 목록 | 공개 |
| GET | `/api/attractions/areas` | 지역코드 목록 (캐시 24h) | 공개 |
| GET | `/api/attractions/curated` | 큐레이션 목록 | 공개 |
| GET | `/api/attractions/{contentId}/like` | 좋아요 상태·수 | 공개 |
| POST | `/api/attractions/{contentId}/like` | 좋아요 토글 | 인증 |

> 검색 캐시 TTL 15분. TourAPI 장애 시 만료 직전 응답(stale)으로 폴백합니다.

### 리뷰

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/attractions/{contentId}/reviews` | 리뷰 목록 | 공개 |
| POST | `/api/attractions/{contentId}/reviews` | 리뷰 작성 | 인증 |
| PATCH | `/api/attractions/{contentId}/reviews/{reviewId}` | 리뷰 수정 | 소유자 |
| DELETE | `/api/attractions/{contentId}/reviews/{reviewId}` | 리뷰 삭제 | 소유자 |
| GET | `/api/reviews/me` | 내가 쓴 리뷰 목록 | 인증 |

### 축제 · 검색

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/festivals` | 축제 목록 (배치 적재 데이터) | 공개 |
| POST | `/api/festivals/sync` | 축제 수동 동기화 | 인증 |
| GET | `/api/search` | 통합 검색 | 공개 |

> 축제 데이터는 `FestivalSyncScheduler`가 매일 **06:00 / 18:00 (KST)** 자동 동기화하고 종료된 축제를 정리합니다.

---

## 4. 여행 계획 (`/api/plans`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/plans` | 내 계획 목록 | 인증 |
| POST | `/api/plans` | 계획 생성 (기간 → `trip_days` 자동 생성) | 인증 |
| GET | `/api/plans/{planId}` | 계획 상세 (days + places 트리) | 소유자 |
| PATCH | `/api/plans/{planId}` | 제목·기간 수정 | 소유자 |
| DELETE | `/api/plans/{planId}` | 계획 삭제 | 소유자 |
| POST | `/api/plans/{planId}/days/{dayNo}/places` | 장소 추가 (contentId → 스냅샷 upsert) | 소유자 |
| PUT | `/api/plans/{planId}/days/{dayNo}/places` | 일자 내 장소 순서·구성 전체 교체 | 소유자 |
| DELETE | `/api/plans/{planId}/days/{dayNo}/places/{placeId}` | 장소 제거 | 소유자 |
| GET | `/api/plans/{planId}/route-report` | 동선 통계 (거리·도보/차량 소요 추정) | 소유자 |
| GET | `/api/plans/{planId}/route-path` | 실제 도로 경로 좌표 (Kakao Mobility) | 소유자 |
| GET | `/api/plans/{planId}/budget` | 예산 집계 | 소유자 |
| GET | `/api/plans/compare` | 계획 1:1 비교 | 인증 |
| GET | `/api/plans/compare-many` | 계획 다중 비교 | 인증 |
| POST | `/api/plans/{planId}/share` | 공유 토큰 발급 | 소유자 |
| GET | `/api/plans/shared/{token}` | 공유 계획 열람 | **공개** |

**동시성** — `trip_plans.version`(`@Version`) 낙관적 잠금. 하위 엔티티(일자·장소) 변경도 plan을 터치해 버전을 올립니다.

| 상황 | 응답 |
|---|---|
| 같은 일자에 동일 장소 추가 | `409 DUPLICATE_PLACE` |
| 장소가 있는 일자가 사라지는 기간 축소 | `409 PLAN_PERIOD_CONFLICT` |
| `expectedVersion` 불일치 | `409 PLAN_VERSION_CONFLICT` |

---

## 5. AI 추천 · 어시스턴트

### 추천 (`/api/recommendations`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/recommendations` | 조건 → AI 일정 초안 생성 (동기) | 인증 |
| GET | `/api/recommendations` | 내 추천 이력 | 인증 |
| GET | `/api/recommendations/{id}` | 추천 상세 | 소유자 |
| POST | `/api/recommendations/{id}/save-plan` | 초안 → 계획 저장 (멱등) | 소유자 |

**생성 동작**
- Redis 분산 락 `lock:reco:{userId}` (NX EX 60, Lua compare-and-delete) — 점유 중이면 `409 RECO_IN_PROGRESS`
- 직전 5분 내 동일 `request_hash`의 SUCCESS가 있으면 **LLM 호출 없이 이전 결과 재반환**
- 검증: 후보 외 contentId 제거 → 일자 범위 검사 → 중복 제거 → 후보 풀 내 1회 보충

| 결과 | 의미 |
|---|---|
| `SUCCESS` | 전 일자 정상 생성 |
| `PARTIAL` | 일부 일자만 생성 (FE가 빈 슬롯 표시) |
| `FAILED` | 생성 실패 (`RECO_EMPTY_RESULT` 등) |

`save-plan`은 `saved_plan_id`로 멱등 보장 — 이미 저장했다면 기존 계획을 200으로 재반환합니다.

### 어시스턴트 · 문서 (RAG)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/assistant/chat` | 챗봇 대화 | 인증 |
| POST | `/api/assistant/chat/stream` | 챗봇 대화 (SSE 스트리밍) | 인증 |
| GET | `/api/documents` | 업로드 문서 목록 | 인증 |
| POST | `/api/documents` | 문서 업로드 (txt/pdf) → 벡터 색인 | 인증 |
| DELETE | `/api/documents/{id}` | 문서 삭제 | 소유자 |

업로드 문서는 redis-stack(RediSearch) 벡터스토어 `triip-docs`에 색인되어 챗봇이 참조합니다. 최대 파일 크기 20MB.

### 데이터 분석 (`/analysis`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/analysis/upload/kakao` | 카카오톡 대화 파일 업로드·분석 | 인증 |
| POST | `/analysis/upload/voice` | 음성 파일 업로드 → STT | 인증 |

> STT는 현재 목업(`MockSTTManager`)으로 동작합니다. 실제 인식은 Whisper 연동으로 전환 가능합니다.

---

## 6. 커뮤니티 (`/community`)

### 게시글

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/community/posts` | 목록 (카테고리 서버 필터·페이지네이션) | 공개 |
| POST | `/community/posts` | 작성 | 인증 |
| GET | `/community/posts/{postId}` | 상세 | 공개 |
| PATCH | `/community/posts/{postId}` | 수정 | 소유자 |
| DELETE | `/community/posts/{postId}` | 삭제 | 소유자 |
| POST | `/community/posts/{postId}/likes` | 좋아요 토글 | 인증 |
| GET | `/community/posts/liked` | 내가 좋아요한 글 | 인증 |
| POST | `/community/images` | 게시글 이미지 업로드 | 인증 |

### 댓글

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/community/posts/{postId}/comments` | 댓글 목록 | 공개 |
| POST | `/community/posts/{postId}/comments` | 댓글 작성 | 인증 |
| PATCH | `/community/posts/{postId}/comments/{commentId}` | 댓글 수정 | 소유자 |
| DELETE | `/community/posts/{postId}/comments/{commentId}` | 댓글 삭제 | 소유자 |
| POST | `/community/posts/{postId}/comments/{commentId}/likes` | 댓글 좋아요 토글 | 인증 |

### 핫플레이스

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/community/hotplaces` | 목록 (승인된 것만) | 공개 |
| POST | `/community/hotplaces` | 등록 → `PENDING` 상태 | 인증 |
| GET | `/community/hotplaces/popular` | 인기 핫플 | 공개 |
| GET | `/community/hotplaces/{hotPlaceId}` | 상세 | 공개 |
| PATCH | `/community/hotplaces/{hotPlaceId}` | 수정 | 소유자 |
| DELETE | `/community/hotplaces/{hotPlaceId}` | 삭제 | 소유자 |
| GET | `/community/hotplaces/{hotPlaceId}/like` | 좋아요 상태 | 공개 |
| POST | `/community/hotplaces/{hotPlaceId}/like` | 좋아요 토글 | 인증 |
| GET | `/community/hotplaces/pending` | 승인 대기 목록 | **ADMIN** |
| POST | `/community/hotplaces/{hotPlaceId}/approve` | 승인 | **ADMIN** |
| POST | `/community/hotplaces/{hotPlaceId}/reject` | 거절 | **ADMIN** |

---

## 7. 동행 (`/companion`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/companion/posts` | 모집글 목록 | 공개 |
| POST | `/companion/posts` | 모집글 작성 (채팅방 자동 생성) | 인증 |
| GET | `/companion/posts/my` | 내가 참여한 동행 | 인증 |
| GET | `/companion/posts/{postId}` | 상세 | 공개 |
| PATCH | `/companion/posts/{postId}` | 수정 | 소유자 |
| DELETE | `/companion/posts/{postId}` | 삭제 | 소유자 |
| PATCH | `/companion/posts/{postId}/close` | 모집 마감 | 소유자 |
| GET | `/companion/posts/{postId}/applications` | 신청자 목록 | 소유자 |
| POST | `/companion/posts/{postId}/applications` | 동행 신청 | 인증 |
| GET | `/companion/posts/{postId}/applications/me` | 내 신청 상태 | 인증 |
| DELETE | `/companion/posts/{postId}/applications/{applicationId}` | 신청 취소 | 인증 |
| PATCH | `/companion/posts/{postId}/applications/{applicationId}/approve` | 수락 (→ 채팅방 입장 권한) | 소유자 |
| PATCH | `/companion/posts/{postId}/applications/{applicationId}/reject` | 거절 | 소유자 |

승인 시 정원(`currentMembers`)이 증가하며 초과 승인·정원 미달 축소가 차단됩니다.

---

## 8. 채팅 (`/api/chat`, `/ws`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/chat/rooms/{chatRoomId}/messages` | 메시지 히스토리 (MongoDB) | 인증 |
| GET | `/api/chat/rooms/{chatRoomId}/participants` | 참여자 목록 | 인증 |
| POST | `/api/chat/rooms/{chatRoomId}/participants` | 참여자 초대 | 인증 |
| DELETE | `/api/chat/rooms/{chatRoomId}/participants/{userId}` | 강퇴 | 방장 |
| PATCH | `/api/chat/rooms/{chatRoomId}` | 방 제목·소개 수정 | 방장 |
| POST | `/api/chat/rooms/{chatRoomId}/image` | 방 이미지 변경 | 방장 |
| PATCH | `/api/chat/rooms/{chatRoomId}/host` | 방장 위임 | 방장 |
| PATCH | `/api/chat/rooms/{chatRoomId}/membership/mute` | 알림 음소거 | 인증 |
| DELETE | `/api/chat/rooms/{chatRoomId}/membership` | 방 나가기 | 인증 |

**WebSocket / STOMP**

| 항목 | 값 |
|---|---|
| 핸드셰이크 | `/ws` (핸드셰이크는 공개, 인증은 STOMP `CONNECT` 프레임에서) |
| 브로커 | RabbitMQ STOMP 릴레이 (`:61613`) |
| 메시지 영속 | MongoDB `chat_messages` |
| 토글 | `chat.enabled=false`면 Mongo·RabbitMQ 없이 기동 |

---

## 9. 알림 · 게임화 · 소셜

### 알림 (`/api/notifications`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/notifications` | 알림 목록 | 인증 |
| GET | `/api/notifications/stream` | SSE 실시간 구독 | 인증 |
| GET | `/api/notifications/unread-count` | 미읽음 수 | 인증 |
| PATCH | `/api/notifications/{notificationId}/read` | 개별 읽음 | 인증 |
| PATCH | `/api/notifications/read-all` | 전체 읽음 | 인증 |

### 게임화 (`/api/gamification`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/gamification/summary` | 레벨·경험치·뱃지 요약 | 인증 |
| GET | `/api/gamification/quests` | 퀘스트 진행도 | 인증 |

### 찜 · 팔로우 · 여행기

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/favorites` | 찜 목록 | 인증 |
| POST | `/api/favorites` | 찜 추가 | 인증 |
| DELETE | `/api/favorites/{targetType}/{targetId}` | 찜 해제 | 인증 |
| POST | `/api/follows` | 팔로우 토글 | 인증 |
| GET | `/api/follows/me/following` | 내 팔로잉 목록 | 인증 |
| GET | `/api/follows/users/{userId}/follow-status` | 팔로우 상태 | 인증 |
| GET | `/api/stories` | 여행기 목록 | 인증 |
| POST | `/api/stories` | 여행기 작성 | 인증 |
| GET | `/api/stories/{storyId}` | 여행기 상세 | 인증 |
| PATCH | `/api/stories/{storyId}` | 여행기 수정 | 소유자 |
| DELETE | `/api/stories/{storyId}` | 여행기 삭제 | 소유자 |

---

## 10. 체크리스트 · 그룹 · 공지 · 상황 정보

### 체크리스트 (`/api/checklists`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/checklists` | 항목 목록 | 인증 |
| POST | `/api/checklists` | 항목 추가 | 인증 |
| GET | `/api/checklists/templates` | 템플릿 목록 | 인증 |
| POST | `/api/checklists/apply` | 템플릿 일괄 적용 | 인증 |
| PATCH | `/api/checklists/{id}` | 항목 수정 | 소유자 |
| PATCH | `/api/checklists/{id}/toggle` | 완료 토글 | 소유자 |
| DELETE | `/api/checklists/{id}` | 항목 삭제 | 소유자 |

### 그룹 (`/api/groups`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/groups/discounts` | 그룹 할인 정보 (데모 데이터) | 공개 |
| GET | `/api/groups` | 그룹 목록 | 인증 |
| POST | `/api/groups` | 그룹 생성 | 인증 |
| GET | `/api/groups/{id}` | 그룹 상세 | 인증 |
| GET | `/api/groups/{id}/members` | 멤버 목록 | 인증 |
| POST | `/api/groups/{id}/join` | 가입 | 인증 |
| DELETE | `/api/groups/{id}/leave` | 탈퇴 | 인증 |

### 공지 (`/api/notices`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/notices` | 공지 목록 | 공개 |
| GET | `/api/notices/{noticeId}` | 공지 상세 | 공개 |
| POST | `/api/notices` | 작성 | **ADMIN** |
| PATCH | `/api/notices/{noticeId}` | 수정 | **ADMIN** |
| DELETE | `/api/notices/{noticeId}` | 삭제 | **ADMIN** |

### 상황 정보 (`/api/context`)

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| GET | `/api/context/weather` | 날씨 | 공개 |
| GET | `/api/context/ev-stations` | 전기차 충전소 (데모 데이터) | 공개 |
| GET | `/api/context/news` | 여행 뉴스 (데모 데이터) | 공개 |

---

## 11. 개발용 (`/api/dev`) — 로컬 전용

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/dev/seed` | 데모/시드 데이터 생성 (멱등) |
| POST | `/api/dev/attractions/batch` | 관광지 대량 적재 배치 |
| POST | `/api/dev/attractions/relike` | 관광지 좋아요 재생성 |
| POST | `/api/dev/festivals/sync` | 축제 동기화 수동 트리거 |

> ⚠️ Spring Security에서는 `permitAll`이지만, **`app.seed.api.enabled=true`이고 `X-Seed-Secret` 헤더가 설정값과 일치할 때만** 동작합니다. 운영 배포 시 `SEED_API_ENABLED`를 제거하거나 `false`로 두세요.

---

## 정적 리소스

| Path | 설명 | 인증 |
|---|---|---|
| `/uploads/**` | 업로드된 이미지·문서 서빙 | 공개 |
| `/swagger-ui/**`, `/v3/api-docs/**` | API 문서 | 공개 |

---

## 참고

- 위 표에 없는 경로는 `anyRequest().authenticated()`가 적용되어 **기본 인증 필요**입니다.
- `SecurityConfig`에 `/preprocessing/**` 매처가 남아 있으나 실제 컨트롤러 경로는 `/analysis/**`입니다. 해당 경로는 기본 규칙(`authenticated`)으로 보호되므로 동작상 문제는 없지만, 정리 대상입니다.
- 인증·소유자 검증 실패는 각각 `401` / `403`, 리소스 없음은 `404 *_NOT_FOUND`, 중복·충돌은 `409`로 응답합니다.
