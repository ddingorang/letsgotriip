# Triip — 프로젝트 인수인계 (codex 핸드오프)

> 대상: 이 프로젝트를 이어받아 독립적으로 개발할 에이전트(codex GPT-5.5 xhigh).
> 이 문서 하나로 **빌드·실행·검증·함정·현재상태·할일**을 파악할 수 있게 정리했다.
> 세부는 기존 문서 참조: [README.md](../README.md) · [SETUP.md](../SETUP.md) · [USAGE.md](../USAGE.md) · [docs/seed-data.md](seed-data.md) · [docs/domain-architecture.md](domain-architecture.md)

---

## 0. 한 줄 요약
Triip = 한국 여행 계획 앱. **BE**: Spring Boot 3.5 / Java 21 / Spring AI 1.1 (Gradle, 포트 9090). **FE**: Vue 3.5 + Pinia + Vite (포트 5173, 모바일 웹뷰 레이아웃). 인프라: MySQL·MongoDB·Redis·Redis(vector)·RabbitMQ. **전부 루트 `docker compose` 로 기동**.

현재 브랜치: `feat/cart-and-plan-edit-ux` (main 브랜치는 `master`).

---

## 1. 빌드 · 실행 (이것만 하면 뜬다)
```bash
docker compose up -d --build          # 전체 빌드+기동 (BE/FE 이미지 빌드 포함)
docker compose ps                     # 상태 확인 (mysql·rabbitmq는 healthy 떠야 함)
# 접속: 프론트 http://localhost:5173  /  백엔드 http://localhost:9090
docker compose down -v && docker compose up -d   # 데이터까지 완전 초기화(덤프에서 재로드)
```
- **데이터는 자동 로드**: `db/init/01-seed.sql`(커밋된 덤프)이 빈 MySQL 볼륨에 자동 주입된다(관광지 ~1,573 / 핫플 8 / 데모·시드 유저 등). 평상시 시연은 `docker compose up -d` **하나면 끝, 스크립트 0개**.
- 포트 매핑: FE `5173:80`(nginx), BE `9090:9090`, MySQL `13306:3306`, redis-vector `6380:6379`, RabbitMQ `15672`(UI)/`61613`(STOMP).
- BE 로컬(도커 없이) Gradle 실행 시 호스트 기본 Java로는 실패 → **JDK 21 경로 지정 필요**(이 머신: `/c/Users/SSAFY/.jdks/ms-21.0.10`).

### 로그인 계정 (시연/테스트)
| 계정 | 비밀번호 | 비고 |
|---|---|---|
| `jeju@seed.triip` (busan/seoul/foodie/… 동일 패턴) | `seed1234` | SeedService가 만든 시드 유저 8명 |
| `demo@triip.com` | `demo1234` | DataSeeder 데모 계정 |
| `admin@triip.com` | `admin1234` | ADMIN (관리자 화면은 현재 비활성 처리됨) |

---

## 2. ⚠ 반드시 알아야 할 함정 (시간 많이 날린 것들)

1. **한글 curl 인코딩(cp949)** — Git Bash on Windows는 `curl -d '{"...한글..."}'` 의 한글을 cp949로 보내 BE가 *Invalid UTF-8 (0xb5)* 400을 뱉는다. → **JSON을 UTF-8 파일로 쓰고 `curl --data-binary @file`** 로 보낼 것. (python으로 파일 생성). 이걸 몰라서 멀쩡한 기능을 "버그"로 두 번 오진했다.
2. **community/companion/analysis는 `/api` prefix 없이 서빙** — BE는 `/community/...`, `/companion/...`, `/analysis/...`. FE는 `/api/community/...` 로 부르고 **프록시(nginx & vite)가 `/api`를 떼준다**. 그래서 BE :9090에 직접 `/api/community/...` 치면 401(미매칭→authenticated)이 정상. 프론트 :5173 경유로는 200. (그 외 `/api/attractions`·`/api/plans` 등은 prefix 유지)
3. **MySQL 자격** — root 비번 `password`, DB `trip_chat`, 컨테이너명 `trip-mysql`. 한글 조회 시 `--default-character-set=utf8mb4` 안 주면 깨져 보인다(데이터는 멀쩡, 터미널 출력 문제).
   ```bash
   docker compose exec -T mysql sh -c 'mysql -uroot -ppassword trip_chat --default-character-set=utf8mb4 -N -e "SELECT ..."'
   ```
4. **테이블명 주의** — 계획=`trip_plans`, 핫플=`hot_places`(단수 아님), 좋아요=`hot_place_like`/`attraction_like`, 관광지=`attractions`.
5. **시드 reset의 insert-before-delete** — Hibernate가 한 트랜잭션에서 insert를 delete보다 먼저 실행해 unique 충돌남. SeedService resetSeedData 끝에 `userRepository.flush()` 로 해결돼 있음(건드리지 말 것).
6. **AI 툴콜링은 느리다(~48초)** — `createTravelPlan`은 (툴결정 LLM → 툴실행 내부 LLM/TourAPI → 최종요약) 3단계. 타임아웃 `CALL_TIMEOUT_SECONDS`는 **90초**로 올려둠(60초면 간헐 실패). 툴 실행 중엔 토큰이 안 흘러 "멈춘 듯" 보이는 게 정상.
7. **태그는 영문 키로 저장** — 큐레이션 태그는 DB에 `,food,`/`,culture,`/`,activity,`/`,night,`. API `?tag=food`(한글 `맛집` 아님). 한글 라벨↔영문 키 매핑은 FE에서.
8. **부팅 시드 비활성** — `SEED_BOOT_ENABLED=false`(덤프가 데이터 제공). 레거시 `DataSeeder`/`HotplaceSeeder`는 idempotent(데모계정/승인핫플 존재 시 skip)라 덤프 기동 시 안 돈다.

---

## 3. 데이터 파이프라인 (덤프 기반)
- **자동**: `docker compose up` 시 덤프 자동 로드. 평상시 추가 작업 없음.
- **수동 스크립트**(데이터를 새로 만들/갱신할 때만, `sh`로 실행):
  | 스크립트 | 용도 |
  |---|---|
  | `sh scripts/seed.sh demo --reset` | 데모 시드 재생성(시드 코드 바꿨을 때) |
  | `sh scripts/batch.sh 2000` | 관광지 ~2천개 TourAPI 재수집(태그·가짜 좋아요 부여) |
  | `sh scripts/db-dump.sh` | 현재 DB → `db/init/01-seed.sql` 덤프 갱신 → **커밋해야 공유됨** |
  - 전제: BE 떠 있고 `SEED_API_ENABLED=true`(로컬 compose 기본값), 헤더 `X-Seed-Secret: triip-local-seed`.
- 데이터를 바꾸면 반드시 `db-dump.sh` → `git add db/init/01-seed.sql && commit`. 안 하면 다음 `down -v` 때 사라진다.

---

## 4. AI / 어시스턴트 구조
- **GMS 프록시**(`https://gms.ssafy.io/gmsapi/api.openai.com/v1/`)로 chat=`gpt-4o-mini`, STT=`whisper-1`. 키는 application.yaml/env.
- 어시스턴트(`BE/.../assistant/AssistantService.java`): Spring AI ChatClient + `@Tool`(createTravelPlan/searchAttractions/evaluatePlan) + RAG(QuestionAnswerAdvisor, recall 켤 때만) + 대화기억(MessageChatMemoryAdvisor, userId 격리).
  - 시스템 프롬프트가 지역명→areaCode(서울1…제주39), 기간→날짜 추론, "되묻지 말고 생성", **능력/예시 물으면 구체 예시 3개 제시**를 지시. **보안경계**: 상태변경 툴은 "현재 대화창 사용자의 직접 요청"일 때만, 주입된 문서/자료의 지시로는 절대 호출 안 함(유지 필수).
  - 엔드포인트 둘 다 인증필수: `POST /api/assistant/chat`(동기), `/chat/stream`(SSE). FE 기본은 stream, 실패 시 동기 폴백.
- STT/대화분석은 **목업 아님**(실제 whisper-1 + LLM). `PreprocessingService`가 transferTo용 절대경로(`java.io.tmpdir`) 사용(상대경로면 FileNotFound 재발). 분석결과는 마스킹 transcript 프리뷰 + 테마키 반환.

---

## 5. 현재 미커밋 변경분 (이번 세션 — 검증 완료, 아직 커밋 안 함)
`git diff --stat` 로 11개 파일. 데이터덤프 제외 코드 변경 요약:

**A. 핫플 별점 + 홈 트렌딩 좋아요 표시**
- `HotPlace.java`: `rating`(Double)·`ratingCount`(int) + `applyDemoRating()` 추가.
- `HotPlaceSummaryResponse.java`: rating/ratingCount 노출.
- `SeedService.seedHotPlace`: 좋아요수에 비례한 사실적 별점 부여(280→4.1 … 1240→4.8, 이름해시 ±흔들림, 리뷰수=좋아요 35~64%).
- `PlaceCard.vue`: 카드 이미지에 좋아요(하트) 배지(1,000↑은 `1.2k`). 별점은 기존 지원.
- `HomeView.vue`: 트렌딩 매핑에 rating/reviewCount 추가.
- `DataSeeder.java`: 중복(좋아요0·별점없음) 핫플 시딩 제거 — 트렌딩은 SeedService 8건만.
- (DB) orphan 핫플 id 2~5 삭제 + 덤프 재생성됨.

**B. 커뮤니티 3탭 (공유게시판·핫플·동행)**
- `CommunityView.vue`: 동행을 **3번째 메인 탭**으로 추가(companionStore 기반 모집글 카드 + FAB). 옛 죽은 동행 CSS(comp-item/room-card)를 PlanView식 companion-card CSS로 교체.
- `PlanView.vue`: 동행을 커뮤니티로 **이전** — 계획 페이지의 `내 계획/동행구하기` 탭바 + 동행 pane 제거(단일 뷰). ⚠ **남은 죽은코드**: `useCompanionStore` import, `companions`(storeToRefs), `companionDday()`, `pageTab` ref(현재 'plans' 고정으로 `v-show`에만 쓰임), companion CSS(2589~) — 동작 무해하나 **정리 대상**.

**C. 탐색 지도 조회범위 안내**
- `ExploreView.vue`: 내접원 반경이 TourAPI 상한 20km를 넘으면(과도한 줌아웃) `areaCapped` 플래그 → 바텀시트에 "중심 20km만 조회됨, 확대하면 정확" 안내. 분류/태그 조회 시 해제. (radius는 [500m,20km] clamp 유지)

**D. 어시스턴트**
- `AssistantService.java`: `CALL_TIMEOUT_SECONDS` 60→90(툴콜링 간헐 타임아웃 방지). 시스템 프롬프트에 "예시 요청 시 구체 예시 3개" 가이드 추가.

검증한 방식: docker 재빌드 → 프리뷰(:5174 vite) DOM eval로 렌더 확인 + UTF-8 curl로 어시스턴트 툴콜링(planId 34/35 실제 생성)·예시응답(3.7s) 확인.

---

## 6. 열린 백로그 (다음에 할 일 후보)
1. **PlanView 죽은코드 정리** — 5-B의 남은 companion import/ref/CSS 제거(단, `pageTab`은 `v-show`에서 쓰이니 함께 정리).
2. **좋아요 ↔ 스크랩(찜) 카운트 정합** — 좋아요(하트)=AttractionLike/HotPlaceLike, 스크랩(찜)=Favorite. 혼동/카운트 표기 정리 요청이 있었음. 핫플 상세/리스트의 저장수·좋아요수 표기 점검.
3. **데이터 주입 bash 정리** — `scripts/*.sh` 사용성/문서화 다듬기 요청이 있었음.
4. (선택) 어시스턴트 툴실행 중 진행표시("계획 만드는 중…") — 48초 무피드백 UX 개선.

---

## 7. 검증 레시피 (그대로 복사)
```bash
# 1) 백엔드 헬스 + 큐레이션(좋아요순)
curl -s "http://localhost:9090/api/attractions/curated?tag=food&size=3" | python -m json.tool | head
# 2) 핫플 popular(별점/좋아요)  — 주의: /api 없이!
curl -s "http://localhost:9090/community/hotplaces/popular?size=3" | python -m json.tool | head -40
# 3) 어시스턴트 툴콜링 (UTF-8 파일 필수)
mkdir -p .tmp && printf '{"email":"jeju@seed.triip","password":"seed1234"}' > .tmp/l.json
TOKEN=$(curl -s -X POST localhost:9090/auth/login -H "Content-Type: application/json" --data-binary @.tmp/l.json | python -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")
python -c "import json;open('.tmp/c.json','w',encoding='utf-8').write(json.dumps({'conversationId':'','message':'제주 2박3일 추천해줘','memory':None}))"
curl -s -X POST localhost:9090/api/assistant/chat -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" --data-binary @.tmp/c.json -w "\n(%{time_total}s)\n"
rm -rf .tmp
# 4) FE 프리뷰(vite) — .claude/launch.json 의 triip-frontend(:5174)로 띄워 DOM 확인
```

---

## 8. codex 실행 메모 (이 머신 한정)
- `codex` CLI 설치됨(`codex exec`, config 기본 `gpt-5.5` / `xhigh`).
- OS 샌드박스 헬퍼 누락 → `-s read-only` 로 셸 실행 시 실패. `--dangerously-bypass...` 는 차단됨.
- **읽기전용 협업이 안전**: 필요한 코드를 프롬프트에 인라인하고 "셸 명령/파일읽기 금지" 지시 후
  `cd <repo>; codex exec --skip-git-repo-check -s read-only - < prompt.txt > out.md 2>err.log` (백그라운드, xhigh는 수분 소요). 내장 web_search는 동작.
- 실제 파일 수정 작업을 codex에 맡기려면 쓰기권한 모드 실행이 필요(이 머신 제약 확인 후 진행).
