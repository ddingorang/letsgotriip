# 좋아요·태그 큐레이션 — 리뷰/테스트/적대적 리뷰 결과

> 작성: 2026-06-23 · 대상: 관광지 좋아요(하트)·태그 큐레이션(맛집/문화/액티비티/야경)·홈 칩→탐색 태그모드·시드 참여데이터.
> 절차: 직접 검증(브라우저/API) + codex(GPT-5.5 xhigh) 적대적 리뷰 → 저비용·고가치 결함 즉시 수정, 나머지 트리아지.

## 1. 검증/테스트 결과 (정상)
- **홈 칩 → 탐색 태그모드**: `/explore?tag=food` → "맛집 인기순 50" 목록, 좋아요순 정렬(제주살롱 1482→당케올레국수 1210→…), `#맛집 ✕` 해제 칩. `tag=night` → "야경 인기순" 정상. (브라우저 검증)
- **큐레이트 API**: food/culture/activity 각 500, night 50. 좋아요순 정렬 정상.
- **좋아요 토글/상태**: liked·likeCount 일관(1051↔1051).
- **좋아요 분포**: 지수분포로 median~130, max~1482 — 자연스러운 롱테일.
- **커뮤니티 시드 참여**: 게시글별 좋아요·댓글 표시([8]좋아요3·댓글2 등).
- ⚠️ **빈 계획·핫플 400은 테스트 하니스 문제**(Windows 셸이 한글 본문을 cp949로 인코딩 → `Invalid UTF-8 0xb5`). ASCII 제목은 201 성공. **프런트(UTF-8)는 정상** — 실제 기능 결함 아님.

## 2. codex 적대적 리뷰 — 트리아지 & 조치

### ✅ 이번에 수정함 (저비용·고가치)
| 항목 | 조치 | 위치 |
|---|---|---|
| 태그모드 URL/상태 분리 | `setTag`가 `router.replace({query:{tag}})`로 URL 동기화, `clearTag`/`selectCategory`가 `?tag` 제거 → 새로고침·뒤로가기·공유 일관 | ExploreView |
| 늦게 온 큐레이트 응답이 최신 태그 덮어씀 | 요청 토큰(`curatedReqId`)으로 stale 응답 무시 | ExploreView.fetchCurated |
| 이전 거리/이름순이 태그모드 인기순을 덮음 | `setTag` 진입 시 `sortMode='default'` 고정 | ExploreView |
| 큐레이트 페이지 동점 시 중복/누락 | `order by likeCount desc, id desc` tie-breaker(4쿼리) | AttractionRepository |
| 유효하지 않은 태그가 전체로 폴백(오인 노출) | 잘못된 태그 → 빈 결과 반환 | AttractionCuratedService |

### ⚠️ 인지된 한계 — 데모 규모에선 수용, 운영 전 권장
| 항목 | codex 등급 | 판단 |
|---|---|---|
| `likeCount` 동시성 lost-update (엔티티 read-modify-write) | P0 | 데모 동시성 낮아 미발현. 운영 시 **원자적 `UPDATE like_count = like_count ± 1`**로 전환 권장 |
| toggle 비멱등(재시도 시 좋아요↔취소 뒤집힘) | P1 | 운영 시 `PUT/DELETE /like` 명시적 상태 API 권장 |
| fake(배치)·real 좋아요 혼합 → `reroll`이 실제 좋아요 덮음 | P0 | `reroll`은 **시연 직전 1회용 dev 도구**. 운영 도입 시 `demoLikeCount`/`realLikeCount` 분리 권장 |
| 태그 LIKE(`%,food,%`) 인덱스 미사용 | P2 | 1.5천건 규모 무영향. 대규모면 태그 조인테이블 |
| dev 배치 게이트(env+secret) | - | 운영은 `SEED_API_ENABLED` 미설정(기본 false). dev-profile+admin은 더 강한 옵션 |
| 상세 좋아요 더블클릭 race | P1 | `likeLoading`으로 요청 중 버튼 비활성 — 1차 가드 존재 |

### ➖ 비해당/저위험
- 큐레이트 SQL injection: `VALID_TAGS` 화이트리스트 + 바인딩 파라미터 → 불가.
- 좋아요 인증: `userId`는 SecurityContext principal에서만 취득(요청 파라미터 아님).

## 3. 남은 권장(우선순위)
1. 좋아요 저장 원자화(`UPDATE ± 1`) + 멱등 상태 API(PUT/DELETE) — 운영 전.
2. demo/real 좋아요 컬럼 분리(reroll은 demo만).
3. 프런트: 상세 좋아요 in-flight 중복요청 시퀀스 가드(현재 버튼 잠금으로 1차 방어).
