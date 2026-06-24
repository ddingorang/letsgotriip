# Triip

여행 계획 · 커뮤니티 · 동행 · 실시간 채팅 · AI 어시스턴트를 한 곳에서 다루는 여행 앱.

- **백엔드** — Spring Boot 3.5 / Java 21 / Spring AI (포트 `9090`)
- **프론트엔드** — Vue 3.5 / Vite → nginx (포트 `5173`)
- **인프라** — MySQL · MongoDB · Redis · Redis-Vector(RAG) · RabbitMQ(STOMP)

---

## 실행법

### 1) 사전 준비
- [Docker Desktop](https://www.docker.com/) 설치 및 실행
- 루트에 `.env` 생성 — [`.env.example`](.env.example)을 복사해 값 채우기

```bash
cp .env.example .env   # Windows PowerShell: Copy-Item .env.example .env
```

채울 키(없어도 앱은 뜨며 해당 기능만 비활성):
- `JWT_SECRET` — 토큰 서명 키 (`openssl rand -base64 32`)
- `OPENAI_API_KEY` — 챗봇 / STT
- `TOUR_API_KEY` — 관광지(한국관광공사 TourAPI)
- `VITE_KAKAO_MAP_KEY` — 지도
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` — Google 로그인(선택)

### 2) 전체 스택 실행 (권장)

```bash
docker compose up -d --build
```

인프라 5종 + 백엔드 + 프론트엔드가 한 번에 뜹니다. 최초 부팅은 백엔드 초기화(약 30~60초)가 필요합니다.
빈 MySQL 볼륨으로 처음 기동하면 `db/init/01-seed.sql`이 자동 로드되어 데모/시드 데이터가 바로 들어갑니다.
수동 `scripts/*.sh`는 평상시 실행용이 아니라 시드 데이터나 관광지 배치를 다시 만들고 덤프를 갱신할 때만 사용합니다.

| 대상 | 주소 |
|------|------|
| **웹앱** | http://localhost:5173 |
| 백엔드 API | http://localhost:9090 |
| RabbitMQ 관리 UI | http://localhost:15672 (guest / guest) |

종료 / 재시작:

```bash
docker compose down                          # 중지
docker compose up -d                         # 재기동
docker compose build frontend && docker compose up -d --no-deps frontend   # 프론트만 재빌드·교체
docker compose logs -f backend               # 백엔드 로그
```

### 3) 데모 계정

| 역할 | 이메일 | 비밀번호 |
|------|--------|----------|
| 일반 사용자 | `demo@triip.com` | `demo1234` |
| 시드 사용자 | `jeju@seed.triip` 등 `@seed.triip` 계정 | `seed1234` |
| 관리자 | `admin@triip.com` | `admin1234` |

로그인 폼은 기본적으로 데모 계정이 미리 채워집니다. 회원가입 / Google 로그인도 가능합니다.

---

## 로컬 개발 (Docker 없이 부분 실행)

```bash
# 백엔드 (인프라는 docker compose로 띄운 상태에서)
cd BE && ./gradlew bootRun          # Windows: gradlew.bat bootRun

# 프론트엔드 (개발 서버, HMR)
cd frontend && npm install && npm run dev
```

빌드 검증:

```bash
cd BE && ./gradlew compileJava       # 백엔드 컴파일
cd frontend && npm run build         # 프론트 프로덕션 빌드
```

> 인프라(MySQL·MongoDB·Redis·RabbitMQ)는 `docker compose up -d mysql mongodb redis redis-vector rabbitmq`로 띄울 수 있습니다. 자세한 환경/포트는 [SETUP.md](SETUP.md) 참고.

---

## 문서

- **[USAGE.md](USAGE.md)** — 화면별 사용법(탐색·계획·커뮤니티·동행·채팅·AI·관리자 등)
- **[SETUP.md](SETUP.md)** — 상세 설치/인프라/환경변수
- **[docs/system-design.md](docs/system-design.md)** — 시스템 설계 / 아키텍처

---

## 참고 (데모 한계)

결제·예약은 데모 화면(실 PG 연동 없음), STT는 현재 목업(Whisper 연동 전환 가능), 뉴스·충전소·그룹할인 등 일부는 데모 데이터입니다. 자세한 내용은 [USAGE.md](USAGE.md#3-참고--한계-데모) 참고.
