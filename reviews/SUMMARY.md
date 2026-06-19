# Codex 적대적 리뷰 종합 (5개 도메인)

> 생성: companion / chat / community / plan-ai / auth-sec 각 `reviews/codex-*.md`
> 총 ~54건. 아래는 심각도·패턴별 종합. (코드 수정은 하지 않은 read-only 리뷰)

## 🔴 Critical — 보안 (인가/인증)

| # | 영역 | 결함 | 근거 |
|---|---|---|---|
| S1 | chat/companion | **STOMP 채팅방 IDOR**: SUBSCRIBE/SEND에 멤버십 검증 없음 + body `chatRoomId` 스푸핑 → 토큰만 있으면 임의 방 송수신 | `StompSessionInterceptor.java:29`, `ChatStompController.java:34`, `ChatService.java:53` |
| S2 | chat | **차단/퇴장 멤버도 히스토리 조회** (isBanned/leftAt 미검사) | `ChatHistoryController.java:44`, `ChatRoomMembership.java:37` |
| S3 | community/auth | **업로드 검증 우회**: MIME·원본확장자만 신뢰 → `/uploads/`에 .html/.js 저장·공개 가능 | `FileStorageService.java:34`, `AlbumStorageService.java:31`, `SecurityConfig.java:65` |
| S4 | auth | **Refresh Token 회전 결함**: overlap window에서 이전 RT가 새 RT로 승격 → 탈취 RT로 세션 연장 | `AuthService.java:148-198` |
| S5 | auth | **탈퇴 후에도 타 기기 refresh 가능** (현재 sessionId만 삭제, DB status 미검증) | `UserService.java:82`, `AuthService.java:136` |
| S6 | auth | **실제 시크릿이 작업트리 `BE/.env`에 평문** (JWT/Google secret/API key) — gitignore라 커밋은 안 되나 워크트리 공유 시 노출. *로컬 검증용으로 넣은 값 — 키 회전 권장* | `BE/.env:7-11` |
| S7 | community | **비승인/반려 핫플 상세가 공개 ID로 조회** (목록은 APPROVED만, 상세는 findById) | `HotPlaceController.java:35`, `HotPlaceService.java:40` |
| S8 | auth | 로그인 API가 이메일 존재 여부를 404/401로 구분 → 이메일 enumeration | `AuthService.java:64`, `ResponseCode.java:36` |

## 🟠 High — 기능/데이터 정합

| # | 영역 | 결함 | 근거 |
|---|---|---|---|
| H1 | companion | 승인된 신청 **취소 시 application만 삭제, 채팅 멤버십 잔존** → 방 계속 노출 | `CompanionService.java:230-277` |
| H2 | companion | **승인 정원 레이스** → 정원 초과 가능 (조회→save, unique 제약 없음) | `CompanionService.java:189-213`, `ChatRoomMembership.java:24` |
| H3 | companion | `GET /companion/posts/my`가 공개 GET 매처에 걸려 **미인증 500(NPE)** | `SecurityConfig.java:62`, `CompanionController.java:25` |
| H4 | community | **핫플 승인 플로우 우회**: 일반 등록이 즉시 APPROVED (관리자 승인 모델 무력화) — *우리가 "등록 후 노출" 위해 의도적으로 바꾼 것, 정책 재확인 필요* | `HotPlaceService.java:51` |
| H5 | plan | **AI 초안 저장이 장소 누락을 조용히 성공 처리** (upsert 실패 catch 무시) | `PlanService.java:333-360` |
| H6 | plan | `replacePlaces` 중복 seq/장소 → DB unique 위반 500 | `PlaceItemDto.java:9`, `PlanService.java:239` |
| H7 | community/plan | **FE가 생성/수정 실패를 성공처럼 처리** (글·핫플 등록 실패에도 이동/가짜항목) | `PostWriteView.vue:159`, `HotplaceRegisterView.vue:283` |
| H8 | community | 게시글 이미지가 **base64로 DB 저장**(업로드 API 미사용) | `PostWriteView.vue:122`, `PostImage.java:23` |

## 🟡 공통 패턴 (전 도메인 반복)

- **입력 검증 부재**: 대부분 컨트롤러에 `@Valid` 없음, DTO에 `@NotBlank/@Size/@Min` 없음 → 잘못된 입력이 400 대신 500/오염데이터. (companion·community·plan·auth·chat 전부) *오늘 GlobalExceptionHandler로 400 매핑은 했으나, 도메인 DTO 제약은 별개.*
- **N+1 쿼리**: 목록/상세/신청자/앨범/채팅히스토리 전부 lazy 반복 조회. fetch join/batch/projection 필요.
- **FE가 실패를 성공으로**: companion 승인·거절, community 글·핫플 등록, chat send 즉시성공.
- **FE-BE 계약 불일치**: AI status `DONE`(FE) vs `SUCCESS`(BE), plan `destination/spots` 미존재, chat 메시지 type 분기, FE 8박 vs BE 7일.
- **동시성**: 좋아요/조회수 `++`, 중복 신청, 정원 — 원자 update/락/unique 부재.
- **배포**: compose 기본 자격증명(guest/password), 업로드 볼륨 미마운트(재생성 시 사진 소실), 쿠키 Secure=false.

## 권장 우선순위
1. **S1·S2 채팅 인가**(IDOR) — 멤버십 검증 추가. 가장 위험.
2. **S3 업로드 검증** — magic-byte+확장자 allowlist.
3. **S4·S5 인증** — RT 회전 원자화, 탈퇴 시 전 세션 무효화+active 검증.
4. **H1·H2·H3 동행** — 멤버십 정리/락/`/my` 인증.
5. 공통: 도메인 DTO `@Valid` 일괄, N+1 정리, FE 실패처리.
