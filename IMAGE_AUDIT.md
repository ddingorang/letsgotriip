# Triip 이미지 아키텍처 감사 (IMAGE_AUDIT)

> 작성 기준: 화면별 이미지 감사 결과 + 핵심 file:line 직접 검증.
> 표기 규칙 — 사실은 `file:line` 근거, 검증 못 한 항목만 '(추정)'.
> 사용자 지적: "사진이 하드코딩된 것 같다" → **결론: 사실. 하드코딩/더미 이미지가 코드 6개 파일에 존재하며, 새 빈 DB에서는 거의 모든 썸네일·신규 유저 아바타가 외부 더미(picsum / dicebear)로 채워진다.**

---

## 0. 소스타입 정의 요약

| 소스타입 | 의미 | docker 안전성 |
|---|---|---|
| `tourapi-external` | TourAPI firstimage 등 외부 http URL (실데이터) | 인터넷 되면 OK, 외부 호스트/외부망 차단 시 깨짐 |
| `be-upload` | BE `/uploads/` 업로드 파일 | nginx `/uploads/` 프록시 정상 시 OK |
| `be-default-asset` | BE가 반환하는 기본 경로(`/images/default-profile.png`) | FE 정적파일 존재에 의존(교차-오리진 결합) |
| `static-public-asset` | `frontend/public` 정적 파일(`/loginpic.jpg` 등) | dist 빌드 포함되면 OK |
| `hardcoded-dummy-url` | 코드에 박힌 외부 더미 URL(picsum/dicebear/jsdelivr) | **외부망 차단 시 전부 깨짐** |
| `hardcoded-local-path` | 코드에 박힌 로컬 경로(존재 안 할 수 있음) | 본 감사 범위에서 **해당 없음** |
| `css-placeholder` | 실이미지 없이 CSS 그라데이션/색박스/인라인 SVG | 안 깨짐 (실데이터도 아님) |
| `none` | 이미지 미표시 | 깨질 것 없음 |

> 참고: 순수 `hardcoded-local-path`(예: `/img/xxx.png` 같은 박힌 로컬 경로)는 본 감사에서 **발견되지 않았다.** 하드코딩 이미지는 전부 외부 URL(`hardcoded-dummy-url`) 형태다.

---

## 1. 이미지 소스 전체 지도

| 화면 | 이미지 용도 | 소스타입 | 실데이터/더미 | docker 깨짐 위험 |
|---|---|---|---|---|
| 홈 (PlaceCard) | '지금 뜨는 여행지' 카드 썸네일 | tourapi-external | real | 낮음(인터넷). 단 빈DB→MOCK firstimage='' → picsum 폴백 |
| 홈 (PlaceCard) | 카드 폴백 썸네일 | **hardcoded-dummy-url** | **dummy** | **높음** (picsum 외부) |
| 탐색 (ExploreView) | 관광지 리스트 썸네일 | tourapi-external | real | 낮음. 빈DB→picsum 폴백 |
| 탐색 (ExploreView) | 관광지 폴백 썸네일 | **hardcoded-dummy-url** | **dummy** | **높음** (picsum) |
| 탐색 (ExploreView) | 축제 리스트 썸네일 | tourapi-external | real | 빈DB+배치 미실행 시 섹션 자체 미표시(none) |
| 탐색 (ExploreView) | 축제 폴백 썸네일 | **hardcoded-dummy-url** | **dummy** | **높음** (picsum) |
| 탐색/지도 (TripMap) | 지도 마커 핀 | css-placeholder | real | 안 깨짐 (DOM/CSS 핀) |
| 탐색/지도 (TripMap) | 지도 베이스 타일 | tourapi-external(Kakao SDK) | real | Kakao 키/도메인 미등록 시 지도 에러 |
| 홈 헤더 | 내 프로필 아바타 | be-upload / be-default-asset | fallback | 낮음(`default-profile` 차단→CSS) |
| 커뮤니티 (PostCard) | 게시글 대표 썸네일 | **hardcoded-dummy-url** | **dummy** | **높음** (picsum, 빈DB서 사실상 항상) |
| 커뮤니티 (PostCard/Detail) | 작성자 아바타 | be-default-asset | fallback | 낮음(FE 정적 존재) / dicebear면 외부 의존 |
| 커뮤니티 (PostDetail) | 상세 히어로 | be-upload | real | 빈DB→CSS placeholder (picsum 폴백 없음=목록과 불일치) |
| 커뮤니티 (PostWrite) | 업로드 미리보기 | be-upload(base64) | real | 안 깨짐. 단 base64를 DB에 통째 저장 |
| 커뮤니티/핫플/동행 (CommunityView) | 핫플·방·동행 썸네일(`seedImg`) | **hardcoded-dummy-url** | **dummy** | **높음** (picsum, 방 썸네일은 무조건) |
| 핫플 (HotplaceDetail) | 상세 히어로 background | css-placeholder | fallback | 안 깨짐. 단 사진 절대 미표시(시드 0장) |
| 핫플 (HotplaceDetail) | '사진 1/24' 카운터 | hardcoded(텍스트) | dummy | 데이터 무관 더미 텍스트 |
| 핫플 (HotplaceRegister) | 사진 업로드 버튼 '0/5' | none(미구현) | dummy | 업로드 핸들러 없음→사진 영영 안 쌓임 |
| 마이 (MyPageView) | 내 프로필 아바타 | be-upload / be-default-asset | real | dicebear면 외부 의존, onerror 핸들러 없음 |
| 마이/동행/채팅/뱃지/앨범 | 그 외 모든 아바타·썸네일·뱃지 | css-placeholder | dummy | 안 깨짐 (전부 CSS/인라인 SVG, 실데이터 0) |
| 회원가입 (SignupView) | 신규 유저 아바타 URL 생성 | **hardcoded-dummy-url** | **dummy** | **높음** (dicebear 외부, DB 영구저장) |
| 장소상세 (PlaceDetailView) | 관광지 히어로 | tourapi-external | real | 빈DB/MOCK→회색 placeholder(사진 안 뜸) |
| 로그인 (LoginView) | 히어로 배경 사진 | static-public-asset | real | 낮음 (`/loginpic.jpg` dist 포함) |
| 결제/확정/체크리스트 | 결제수단·티켓·체크리스트 | css-placeholder / none | dummy | 이미지 없음(데이터가 하드코딩 더미) |
| AI결과/챌린지/알림 | 장소·뱃지·알림 아이콘 | none / css-placeholder | - | 인라인 SVG/이모지, 깨질 것 없음 |
| 인프라 (base.css) | Pretendard 웹폰트 | hardcoded-dummy-url(jsdelivr) | real | 외부망 차단 시 폰트 폴백(이미지 아님) |

---

## 2. 하드코딩/더미 이미지 목록 — 사용자가 가장 우려한 부분

> **`hardcoded-dummy-url` 전부.** 모두 외부 도메인(picsum.photos / dicebear.com / jsdelivr CDN)에 박힌 URL이며, nginx 프록시로 구제 불가(브라우저가 직접 외부로 나감). `hardcoded-local-path`는 발견되지 않음.

### 2-A. 외부 더미 사진 — picsum.photos (5개 위치, 검증됨)

| # | file:line | 코드 | 용도 | 비고 |
|---|---|---|---|---|
| 1 | `frontend/src/components/common/PlaceCard.vue:53` | `https://picsum.photos/seed/triip-${seed}/240/200` | 홈/관광지 카드 폴백 | firstimage 비면 노출 |
| 2 | `frontend/src/components/community/PostCard.vue:79` | `https://picsum.photos/seed/triip-${seed}/640/440` | 게시글 카드 썸네일 | 시드 게시글 imageUrls=`List.of()`라 사실상 항상 |
| 3 | `frontend/src/views/ExploreView.vue:348` | `https://picsum.photos/seed/triip-${place.contentId ?? place.name}/240/240` | 관광지 썸네일 폴백 | |
| 4 | `frontend/src/views/ExploreView.vue:358` | `https://picsum.photos/seed/fest-${fest.id ?? fest.title}/240/240` | 축제 썸네일 폴백 | |
| 5 | `frontend/src/views/CommunityView.vue:298` | `https://picsum.photos/seed/triip-${seed}/${w}/${h}` (`seedImg`) | 핫플·방·동행 공용 폴백 생성기 | 방 썸네일(`:184`)은 **조건 없이 무조건** 호출 |

`CommunityView.vue:300-303`의 `onThumbError`도 로드 실패 시 **다시 picsum으로 교체** → 폐쇄망에서는 폴백조차 외부라 빈 박스로 끝남.

### 2-B. 외부 더미 아바타 — dicebear.com (최우선 이슈, 검증됨)

| # | file:line | 코드 | 용도 |
|---|---|---|---|
| 6 | `frontend/src/views/SignupView.vue:155` | `https://api.dicebear.com/9.x/thumbs/svg?seed=${encodeURIComponent(nick)}` | 회원가입 시 `profileImageUrl` 생성 → `:176` signup payload로 BE에 **DB 영구 저장** |

→ 새 빈 DB로 **폼 가입한 모든 유저의 아바타**가 외부 dicebear 도메인에 영구 종속. MyPageView/PostCard/PostDetail 아바타가 전부 이 값을 받는다.

### 2-C. 외부 CDN 폰트 (이미지는 아니나 동일한 외부 하드코딩 의존)

| # | file:line | 용도 |
|---|---|---|
| 7 | `frontend/src/assets/base.css:2` | jsdelivr CDN Pretendard `@import` — 외부망 차단 시 폰트 미적용(폴백 렌더) |

### 2-D. 데이터성 하드코딩 더미 (이미지 자원은 아니지만 '하드코딩' 지적 대상)

- `frontend/src/views/HotplaceDetailView.vue:21` — '사진 1/24' 고정 텍스트(실제 사진 수 무관).
- `frontend/src/views/MyPageView.vue` stats(5/38/2), allPlans, albums, badges 전부 컴포넌트 내 하드코딩 ref 배열.
- `frontend/src/views/BadgesView.vue` levelStats/quests/badges 하드코딩 더미.
- `PaymentView` / `ConfirmationView` / `ChecklistView` 화면 전체가 데모 목업 텍스트(코드 주석에 데모 명시).

---

## 3. docker에서 깨지는 구조적 원인

### 3-1. 새 빈 DB → 실데이터가 0이라 폴백(외부 더미)이 '기본값'이 됨
- 핫플: `HotplaceSeeder` / `DataSeeder.java:119-123`가 `imageUrls=List.of()`(사진 0장)로 시드하고 `HotPlace` 엔티티에 이미지 컬럼이 없음 → `hp.imageUrl` 항상 null → `seedImg()` picsum 고착.
- 게시글: 시드 게시글 `imageUrls=List.of()`(`DataSeeder.java:132,139`) → `post.imageUrl=null` → PostCard picsum 고착.
- 관광지/축제: TourAPI 동기화/배치 실행 전이면 firstimage 결측 → MOCK(`firstimage:''`) 폴백 → picsum.
- **즉, "사진이 안 쌓이는 구조" + "비면 외부 더미로 채우는 폴백"이 결합**되어, 새 스택에서는 더미가 화면 전체를 덮는다.

### 3-2. 외부 도메인 의존 (nginx 프록시로 구제 불가)
- picsum.photos / api.dicebear.com / cdn.jsdelivr.net는 **클라이언트가 직접 외부로 호출**. nginx는 `/api`·`/uploads`만 프록시(`nginx.conf:58`)하므로 무관. 망분리/사내망/오프라인 docker에서 전부 깨짐.

### 3-3. nginx SPA fallback이 누락 이미지에 index.html(200 html)을 반환
- `frontend/nginx.conf:79` `try_files $uri $uri/ /index.html;` (검증됨).
- dist에 없는 정적 이미지 경로(예: 오타난 `/images/xxx.png`)는 **404가 아니라 index.html(Content-Type text/html, 200)**을 반환 → `<img>`가 HTML을 이미지로 받아 조용히 깨지고 `onerror` 신뢰성이 떨어짐.
- `/uploads/`는 별도 `location`(`nginx.conf:58`)으로 BE 프록시되어 이 함정은 회피하나, `/images/` 등 FE 정적 이미지는 fallback 함정에 노출.

### 3-4. 기본 아바타 에셋의 교차-오리진 결합 (be-default-asset)
- `BE/.../user/entity/User.java:86` `DEFAULT_PROFILE_IMAGE_URL='/images/default-profile.png'`를 시드/기본 유저에 부여(`DataSeeder.java:81,90`).
- BE `WebConfig`는 `/uploads/**`만 핸들링하고 `/images/**` 핸들러 없음 → **BE 오리진 직접 요청 시 404**.
- 현재는 `frontend/public/images/default-profile.png`가 존재(커밋 `76f2e9c`에서 추가)해 nginx SPA fallback으로 우연히 서빙되어 **docker에서 동작**. 단 이 파일을 지우거나 BE를 독립 서빙하면 깨짐.
- 불일치: `HomeView.vue:90`은 같은 값을 "실제 파일이 없어 깨진다"는 **옛 전제의 죽은 방어로직**으로 null 처리(`HomeView.vue:85` 주석 검증됨)해 일부러 숨김. 반면 MyPageView/PostCard는 그대로 렌더 → 화면 간 처리 불일치.

---

## 4. '기존(npm dev)에선 됐는데 docker에서 안 되는' 차이 분석

| 차이 | npm dev (기존) | docker (현재) | 결과 |
|---|---|---|---|
| **DB 상태** | 개발자 로컬 DB에 이미 업로드/동기화 데이터가 누적되어 있었을 가능성(추정) | 새 빈 DB, 시드 사진 0장 | docker에서 firstimage/imageUrls가 전부 비어 폴백(picsum)으로 전환 |
| **외부망** | 개발 PC는 보통 인터넷 자유 → picsum/dicebear/jsdelivr 로드됨 | 망분리/오프라인/CSP 차단 환경 가능 | 외부 더미가 통째로 깨짐 (가장 직접적 차이) |
| **정적 서빙** | Vite dev 서버가 `public/`·`src/assets`를 유연히 서빙, 누락 시 404 | nginx `try_files`가 누락 이미지에 index.html(200) 반환 | 깨진 이미지가 onerror 없이 조용히 발생 |
| **TourAPI/배치** | 이미 동기화 돌려둔 상태 | 배치 미실행 시 축제/관광 데이터·이미지 없음 | 섹션 미표시 또는 picsum 폴백 |
| **기본 아바타** | FE dev 서버가 `/images/default-profile.png` 직접 서빙 | nginx fallback에 의존(파일 존재 시 OK) | 파일 누락 시 무한 깨짐 위험 |

핵심: **"기존엔 됐다"는 건 (a) 채워진 DB + (b) 외부망 자유 덕분이었고, docker는 둘 다 사라지면서 폴백 구조의 외부 더미 의존이 표면화**된 것.

---

## 5. 수정안 (우선순위별)

### P0 — 신규 유저 아바타 외부 dicebear URL 제거 (구조적 결함, DB 영구저장)
- 근거: `frontend/src/views/SignupView.vue:155,176`.
- 조치: dicebear 외부 URL 생성을 **제거**하고, 가입 시 `profileImageUrl`을 비워서 보내거나 BE 기본 에셋(`/images/default-profile.png`)으로 통일. 아바타는 BE 업로드(`/uploads/`) 경로로만 채우도록 일원화.
- 효과: 외부망 차단 docker에서도 전 유저 아바타 정상. DB에 외부 도메인이 박히는 영구 오염 제거.

### P0 — 썸네일 picsum 폴백 → 로컬 정적 에셋으로 교체
- 근거: `PlaceCard.vue:53`, `PostCard.vue:79`, `ExploreView.vue:348,358`, `CommunityView.vue:298`(+`onThumbError :300-303`).
- 조치: `https://picsum.photos/...`를 `frontend/public/images/`에 둔 **로컬 기본 썸네일**(예: `/images/placeholder-thumb.png`) 또는 CSS placeholder로 교체. `onThumbError`도 외부가 아닌 로컬/CSS로 폴백.
- 효과: 폐쇄망에서도 안 깨짐. "하드코딩 외부 더미" 지적 직접 해소.

### P1 — 핫플 사진 업로드 기능 구현 + 시드에 실이미지 추가 (근본 원인)
- 근거: `frontend/src/views/HotplaceRegisterView.vue:101-110`(업로드 핸들러/파일 input 없음, submit `imageUrls:[]` 하드코딩 `:279`).
- 조치: 핫플 등록에 실제 파일 input + `/uploads/` 멀티파트 업로드 연결. 동시에 `HotplaceSeeder`/`DataSeeder`의 `imageUrls=List.of()`를 실제 시드 이미지(번들된 정적 또는 업로드 더미 실파일)로 교체.
- 효과: 사진이 BE에 쌓이기 시작 → 썸네일이 더미가 아닌 실데이터로 전환. picsum 폴백이 '기본값'이 되는 구조 해소.

### P1 — 커뮤니티 이미지 업로드를 base64 → `/uploads/` 실업로드로 전환
- 근거: `frontend/src/views/PostWriteView.vue:119-128,149`(FileReader.readAsDataURL → base64 data URI를 imageUrls로 POST), BE `CommunityService.saveImages`가 base64를 `post_image.image_url`에 통째 저장.
- 조치: 멀티파트 업로드 + `FileStorageService(/uploads/)` 경유. 응답엔 URL만 싣기.
- 효과: 거대한 base64가 목록/상세 응답·DB 컬럼에 실리는 성능·길이 문제 제거.

### P1 — nginx 정적 이미지 404 처리 (조용한 깨짐 방지)
- 근거: `frontend/nginx.conf:79` `try_files $uri $uri/ /index.html;`.
- 조치: 이미지 확장자(`\.(png|jpe?g|gif|svg|webp)$`) location을 분리해 누락 시 **index.html이 아닌 404**를 반환하도록 설정. 그래야 `<img onerror>` 폴백이 정상 동작.
- 효과: 누락 이미지가 HTML로 둔갑해 무한 깨지는 함정 제거.

### P2 — 기본 아바타 처리 일원화 + 죽은 방어로직 제거
- 근거: `HomeView.vue:85,90`(default-profile을 null 처리하는 죽은 로직), `User.java:86`, BE `WebConfig`에 `/images/**` 핸들러 부재.
- 조치: (a) `HomeView`의 `default-profile` 차단 로직 제거(파일 존재 확인됨). (b) BE에도 `/images/**` 정적 핸들러를 추가하거나, 기본 아바타 경로를 nginx가 확실히 서빙하도록 명시. (c) 댓글 아바타(`PostDetailView.vue:115-117`)가 무시 중인 `authorProfileImageUrl`을 실제로 바인딩.
- 효과: 화면 간 아바타 처리 통일, 실 프로필 사진 반영.

### P2 — 외부 CDN 폰트 로컬 번들화
- 근거: `frontend/src/assets/base.css:2`(jsdelivr Pretendard `@import`).
- 조치: Pretendard를 npm 패키지/로컬 폰트로 번들. 외부망 차단 docker에서 폰트 정상.

### P3 — 고아 에셋 정리
- 근거: `frontend/src/assets/{hero.png,vite.svg,vue.svg}`, `frontend/public/icons.svg` 참조 0회.
- 조치: 미사용 에셋 제거(public의 icons.svg는 dist에 죽은 용량으로 복사됨).

---

## 부록: 깨지지 않는(=실데이터도 아닌) 영역

마이/동행/채팅/뱃지/앨범 화면의 거의 모든 시각 요소(아바타, 히어로, 뱃지, 챌린지, 앨범 사진)는 `<img>`가 아니라 **CSS 그라데이션 박스 + 인라인 SVG**라 docker에서 깨지지 않는다. 단 이는 "안전"이 아니라 **실데이터 연동이 0**이라는 뜻이며, `companion.js`의 `thumbnail`/`profileImageUrl` 필드는 normalize되지만 템플릿에서 미사용(데드 필드)이다.
