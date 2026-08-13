# 셋업 · 실행 가이드

설치 · 실행 · 포트 · 데모 계정을 다룹니다. 프로젝트 개요는 [README.md](README.md), 화면별 사용법은 [USAGE.md](USAGE.md), API 명세는 [docs/API.md](docs/API.md) 참고.

## 프로젝트 구조

```
springaitrip/
├── docker-compose.yml   # 전체 스택 정본 (인프라 + 백엔드 + 프론트엔드)
├── .env                 # 시크릿 (gitignore) — .env.example 복사해서 작성
├── BE/                  # Spring Boot 3.5.14 + Spring AI 1.1.2 (Gradle, :9090)
├── frontend/            # Vue 3.5 + Vite → nginx (:5173)
├── db/init/             # 첫 기동 시 자동 로드되는 시드 덤프
├── scripts/             # 시드·배치 재생성 스크립트
└── docs/
```

---

## 1. 요구 사항

| 항목 | 버전 | 비고 |
|---|---|---|
| **Docker Desktop** | 최신 | **전체 스택 실행 시 이것만 있으면 됩니다** |
| JDK | 21 | 로컬에서 백엔드를 직접 실행할 때만 (`build.gradle` toolchain) |
| Node.js | 20+ | 로컬에서 프론트엔드를 직접 실행할 때만 |
| Gradle | wrapper 포함 | 별도 설치 불필요 (`BE/gradlew`) |

<details>
<summary>JAVA_HOME 설정 (로컬 실행 시, 최초 1회)</summary>

```powershell
# 설치된 JDK 21 경로로 교체하세요
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "<JDK 21 설치 경로>", "User")
```
</details>

---

## 2. 빠른 시작 — Docker 전체 스택 (권장)

### 2.1 `.env` 작성

```bash
cp .env.example .env    # Windows PowerShell: Copy-Item .env.example .env
```

> ⚠️ **반드시 저장소 루트의 `.env`** 를 사용하세요. `frontend/.env`와 `BE/.env`는 로컬 직접 실행용이며, Docker 빌드에서는 `.dockerignore`로 제외되어 **읽히지 않습니다**.

키를 비워도 앱은 뜨며 해당 기능만 비활성화됩니다.

| 키 | 용도 | 발급처 |
|---|---|---|
| `JWT_SECRET` | 토큰 서명 | `openssl rand -base64 32` |
| `OPENAI_API_KEY` | AI 어시스턴트 · 추천 · STT | SSAFY GMS 포털 (gms.ssafy.io OpenAI 호환 프록시) |
| `TOUR_API_KEY` | 관광지 · 축제 | https://www.data.go.kr (한국관광공사 TourAPI) |
| `VITE_KAKAO_MAP_KEY` | 지도 (**JavaScript 키**) | https://developers.kakao.com |
| `KAKAO_REST_API_KEY` | 자동차 경로 탐색 (**REST 키**) | https://developers.kakao.com |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google 로그인 (선택) | https://console.cloud.google.com |

### 2.2 기동

```bash
docker compose up -d --build
```

인프라 5종(MySQL · MongoDB · Redis · redis-vector · RabbitMQ) + 백엔드 + 프론트엔드가 한 번에 뜹니다.
빈 MySQL 볼륨으로 처음 기동하면 `db/init/01-seed.sql`(약 1.1MB)이 자동 로드되어 데모 데이터가 함께 들어갑니다 — 이 과정 때문에 **최초 기동은 1분 정도** 걸립니다.

| 대상 | 주소 |
|---|---|
| **웹앱** | http://localhost:5173 |
| 백엔드 API | http://localhost:9090 |
| Swagger UI | http://localhost:9090/swagger-ui/index.html |
| RabbitMQ 관리 UI | http://localhost:15672 (guest / guest) |

### 2.3 자주 쓰는 명령

```bash
docker compose ps                 # 상태 확인
docker compose logs -f backend    # 백엔드 로그
docker compose down               # 중지 (데이터 유지)
docker compose down -v            # 중지 + 볼륨 삭제 (시드 초기화)
docker compose up -d              # 재기동

# 프론트만 재빌드·교체 (VITE_* 값을 바꿨을 때 필수)
docker compose build frontend && docker compose up -d --no-deps frontend
```

---

## 3. 포트 구성

| 서비스 | 컨테이너 내부 | 호스트 | 비고 |
|---|---|---|---|
| frontend (nginx) | 80 | **5173** | 웹앱 진입점 |
| backend (Spring) | 9090 | **9090** | |
| MySQL | 3306 | **13306** | Windows winnat 예약(3307) 회피 |
| MongoDB | 27017 | – | 내부 네트워크 전용 |
| Redis | 6379 | – | 내부 네트워크 전용 |
| redis-vector | 6379 | **6380** | RAG 벡터스토어 (디버그용 노출) |
| RabbitMQ (AMQP) | 5672 | – | 내부 전용 |
| RabbitMQ STOMP / 관리 UI | 61613 / 15672 | **61613 / 15672** | UI: guest / guest |

**호스트에서 MySQL 접속** (DBeaver·MySQL Workbench 등)

| 항목 | 값 |
|---|---|
| Host | `localhost:13306` |
| Database | `trip_chat` |
| Username / Password | `root` / `password` |

---

## 4. 데모 계정

| 역할 | 이메일 | 비밀번호 |
|---|---|---|
| 일반 사용자 | `demo@triip.com` | `demo1234` |
| 시드 사용자 | `jeju@seed.triip` 등 `@seed.triip` 계정 | `seed1234` |
| 관리자 | `admin@triip.com` | `admin1234` |

로그인 폼은 기본적으로 데모 계정이 미리 채워집니다(`.env`의 `VITE_DEMO_EMAIL` / `VITE_DEMO_PASSWORD`로 변경하거나 빈 값으로 비활성화). 직접 **회원가입** 또는 **Google 로그인**도 가능합니다.

---

## 5. 로컬 부분 실행 (Docker 없이 개발)

인프라만 컨테이너로 띄우고 애플리케이션은 로컬에서 실행하는 방식입니다. HMR·디버거를 쓸 때 유용합니다.

```bash
# 1) 인프라만 기동
docker compose up -d mysql mongodb redis redis-vector rabbitmq

# 2) 백엔드
cd BE && ./gradlew bootRun          # Windows: .\gradlew.bat bootRun

# 3) 프론트엔드 (HMR)
cd frontend && npm install && npm run dev
```

> ⚠️ `application.yaml`의 `DB_PORT` 기본값은 `3307`이지만 현재 compose는 MySQL을 **13306**으로 노출합니다.
> 로컬 백엔드 실행 시 `BE/.env` 또는 OS 환경변수로 `DB_PORT=13306`을 지정하세요.

**로컬 실행 시 환경변수** — 백엔드는 `spring-dotenv`로 `BE/.env`(또는 OS 환경변수)를 읽고, 프론트엔드는 `frontend/.env`의 `VITE_*`를 읽습니다. MongoDB·Redis·RabbitMQ 호스트는 `localhost` 기본값이라 인프라만 떠 있으면 별도 설정이 필요 없습니다.

```dotenv
# BE/.env
DB_PORT=13306
JWT_SECRET=<임의의 긴 시크릿>
OPENAI_API_KEY=<SSAFY GMS 키>
TOUR_API_KEY=<TourAPI 키>
GOOGLE_CLIENT_ID=<...>
GOOGLE_CLIENT_SECRET=<...>
# 선택(기본값 있음):
#   DB_USERNAME(root) / DB_PASSWORD(password)
#   RABBITMQ_USERNAME(guest) / RABBITMQ_PASSWORD(guest)
#   CHAT_ENABLED(true; false면 RabbitMQ·MongoDB 없이 부팅)
#   COOKIE_SECURE(false; 운영 HTTPS는 true)
#   FE_ALLOWED_ORIGINS(http://localhost:5173,http://127.0.0.1:5173)
#   FE_OAUTH_CALLBACK_URL / FE_LOGIN_URL
```

```dotenv
# frontend/.env
VITE_KAKAO_MAP_KEY=<Kakao JavaScript 키>
```

**빌드 검증**

```bash
cd BE && ./gradlew compileJava      # 백엔드 컴파일
cd frontend && npm run build        # 프론트 프로덕션 빌드
```

---

## 6. 트러블슈팅

**카카오 지도가 표시되지 않음**
`VITE_KAKAO_MAP_KEY`는 **빌드 시점에 번들로 주입**됩니다. 키가 비어 있으면 minifier가 SDK 로더 코드를 통째로 제거하므로, 값을 채운 뒤 반드시 재빌드해야 합니다.
```bash
docker compose build frontend && docker compose up -d --no-deps frontend
```
Docker 실행에서는 **루트 `.env`만** 유효합니다(`frontend/.env`는 `.dockerignore` 제외 대상). 브라우저 캐시가 남아 있으면 하드 리프레시(Ctrl+Shift+R)하세요.

**백엔드 컨테이너가 바로 종료됨**
`docker compose logs backend`로 원인을 확인하세요. `Communications link failure`가 보이면 MySQL이 아직 TCP를 열지 않은 상태입니다 — compose의 healthcheck가 TCP 리스닝을 검사하도록 되어 있어 정상적으로는 발생하지 않지만, 시드 로드가 오래 걸리면 `docker compose up -d backend`로 재기동하면 됩니다.

**축제 목록이 비어 있음**
축제는 배치 적재 데이터입니다. `FestivalSyncScheduler`가 매일 06:00 / 18:00(KST)에 동기화하며, 즉시 채우려면 sync를 수동 트리거하세요(`TOUR_API_KEY` 필요).
```bash
./scripts/run-festival-sync.sh
```

**시드 데이터를 다시 만들고 싶음**
`docker compose down -v`로 볼륨을 삭제하고 재기동하면 `db/init/01-seed.sql`이 다시 로드됩니다. 덤프 자체를 갱신하려면 [scripts/SEEDING.txt](scripts/SEEDING.txt) 참고.

**채팅 없이 가볍게 띄우고 싶음**
`CHAT_ENABLED=false`로 두면 RabbitMQ·MongoDB 없이 부팅됩니다(`application.yaml`의 `chat.enabled`).

---

## 부록. 로컬 MySQL 직접 사용

Docker 없이 호스트에 설치된 MySQL을 쓰는 경우입니다. DB `trip_chat`을 미리 만들어 두고, 백엔드의 `DB_PORT`를 해당 포트에 맞추세요.

<details>
<summary>Windows 수동 실행 / 서비스 등록</summary>

```powershell
# PC 시작 시마다 실행
Start-Process -FilePath "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" `
  -ArgumentList '--datadir="C:\ProgramData\MySQL\data" --port=3307' -WindowStyle Hidden

# 영구 서비스 등록 (관리자 권한 PowerShell, 1회만)
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" --install MySQL84 --datadir="C:\ProgramData\MySQL\data"
Start-Service MySQL84
```
</details>

> `BE/trip-docker/docker-compose.yml`은 인프라 4종만 정의한 **초기 버전**이며 MySQL을 3307로 노출합니다.
> 현재 정본은 루트 `docker-compose.yml`(전체 스택, MySQL 13306)입니다.
