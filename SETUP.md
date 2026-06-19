# 프로젝트 셋업 가이드

## 프로젝트 구조

```
springaitrip/
├── BE/         # Spring Boot 3.5.14 + Spring AI 1.1.2 (Gradle, 포트 9090)
├── frontend/   # Vue 3.5 + Vite (포트 5173)
└── docs/
```

---

## 1. 필수 환경

| 항목 | 버전 | 비고 |
|------|------|-----------|
| Java | 21 (build.gradle toolchain) | JDK 21 필요 |
| 빌드 | Gradle (wrapper 포함) | `BE/gradlew.bat` 사용, 별도 설치 불필요 |
| MySQL | 8.x | 포트 3307 / DB `trip_chat` |
| MongoDB | 7.x | 채팅 메시지 히스토리 (포트 27017) |
| Redis | 7.x | 세션 회전 / 추천 캐시 / 조회수 (포트 6379) |
| RabbitMQ | 3.12+ (STOMP 플러그인) | 채팅 STOMP 릴레이 (AMQP 5672 / STOMP 61613 / 관리UI 15672) |
| Node.js | 18+ | - |

> **채팅(WebSocket/STOMP) 기능**은 RabbitMQ(STOMP 릴레이)·MongoDB(메시지 히스토리)·Redis 를 모두 요구합니다.
> 가장 쉬운 방법은 아래 **1.5절 docker compose** 로 인프라 4종(MySQL·MongoDB·Redis·RabbitMQ)을 한 번에 띄우는 것입니다.
> RabbitMQ 없이 채팅을 끄고 부팅하려면 `CHAT_ENABLED=false` 환경변수를 사용하세요(application.yaml `chat.enabled`).
>
> dev 환경에서 WebSocket 은 FE 클라이언트가 BE(:9090) 로 **직접** 연결합니다(`/ws` raw / `/ws-sockjs`).

### JAVA_HOME 설정 (최초 1회)
```powershell
# 설치된 JDK 21 경로로 교체하세요
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "<JDK 21 설치 경로>", "User")
```

---

## 1.5 인프라 4종 (docker compose — 권장)

채팅까지 포함한 전체 기능을 가장 쉽게 띄우는 방법입니다. `BE/trip-docker/docker-compose.yml` 은
MySQL(3307:3306)·MongoDB(27017)·Redis(6379)·RabbitMQ(AMQP 5672 / STOMP 61613 / 관리UI 15672, STOMP 플러그인 자동 활성화)
를 정의합니다(인프라만 — 앱 컨테이너는 미포함).

```powershell
cd BE\trip-docker
docker compose up -d        # 4종 백그라운드 기동
docker compose ps           # 상태 확인
# 종료: docker compose down  (데이터 유지) / docker compose down -v (볼륨 삭제)
```

기본 자격증명: MySQL `root` / `password`(DB `trip_chat`), RabbitMQ `guest` / `guest`.
docker compose 로 MySQL 을 띄우면 아래 2절(수동 MySQL 실행)은 건너뛰어도 됩니다.

> docker 없이 채팅만 끄고 띄우려면: MySQL(2절)만 준비하고 BE 를 `CHAT_ENABLED=false` 로 실행하세요.

---

## 2. MySQL 시작 (docker 미사용 시)

> docker compose(1.5절)를 쓰지 않고 로컬 MySQL 을 직접 쓰는 경우의 절차입니다.
> MySQL은 서비스가 아닌 수동 실행으로 설정되어 있습니다.

### PC 시작 시마다 실행
```powershell
Start-Process -FilePath "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" `
  -ArgumentList '--datadir="C:\ProgramData\MySQL\data" --port=3307' -WindowStyle Hidden
```

### 영구 서비스 등록 (관리자 터미널에서 1회만)
```powershell
# 관리자 권한 PowerShell에서 실행
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" --install MySQL84 --datadir="C:\ProgramData\MySQL\data"
Start-Service MySQL84
```

### DB 접속 정보
> 실제 값은 `BE/src/main/resources/application.yaml` 의 `spring.datasource` 및
> 환경변수(`DB_USERNAME`/`DB_PASSWORD`)를 기준으로 합니다. 아래는 기본값입니다.

| 항목 | 값 |
|------|----|
| Host | localhost:3307 |
| Database | trip_chat |
| Username | `${DB_USERNAME:root}` (기본 root) |
| Password | `${DB_PASSWORD:password}` (기본 password) |

---

## 3. API 키 / 환경변수 설정

BE 는 `spring-dotenv` 로 `BE/.env` 파일(또는 OS 환경변수)을 읽습니다.
`application.yaml` 이 참조하는 주요 변수는 아래와 같습니다(`.env` 는 `.gitignore` 대상).

```dotenv
# BE/.env
OPENAI_API_KEY=<SSAFY GMS(OpenAI 호환) 키>
TOUR_API_KEY=<한국관광공사 TourAPI 키 — 축제 sync 배치에 필요>
JWT_SECRET=<임의의 긴 시크릿 문자열>
GOOGLE_CLIENT_ID=<Google OAuth 클라이언트 ID>
GOOGLE_CLIENT_SECRET=<Google OAuth 시크릿>
# 선택(기본값 있음):
#   DB_USERNAME(기본 root) / DB_PASSWORD(기본 password)
#   RABBITMQ_USERNAME(기본 guest) / RABBITMQ_PASSWORD(기본 guest)
#   CHAT_ENABLED(기본 true; false면 RabbitMQ 없이 부팅)
#   COOKIE_SECURE(기본 false; 운영 HTTPS는 true 권장)
#   FE_ALLOWED_ORIGINS(기본 http://localhost:5173,http://127.0.0.1:5173 — CORS 허용 origin)
#   FE_OAUTH_CALLBACK_URL / FE_LOGIN_URL (OAuth 콜백·로그인 리다이렉트)
```

> MongoDB·Redis·RabbitMQ 호스트는 `application.yaml` 에서 localhost 기본값으로 설정되어 있어,
> 1.5절 docker compose(또는 동일 포트의 로컬 인스턴스)만 떠 있으면 별도 env 없이 연결됩니다.

### API 키 발급처

| 변수 | 발급처 |
|----|--------|
| `OPENAI_API_KEY` | SSAFY GMS 포털 / 강사님 (gms.ssafy.io OpenAI 호환 프록시) |
| `TOUR_API_KEY` | https://www.data.go.kr (한국관광공사 TourAPI) |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | https://console.cloud.google.com (OAuth 2.0) |

---

## 4. Backend 실행

```powershell
cd BE
.\gradlew.bat bootRun
```

접속: http://localhost:9090 (Swagger UI: http://localhost:9090/swagger-ui.html)

> 최초 실행 시 Gradle 의존성 다운로드로 수 분 소요될 수 있습니다.
> 채팅을 켜려면 RabbitMQ(STOMP)·MongoDB·Redis 가 떠 있어야 합니다(1.5절 docker compose 권장).
> 인프라 없이 띄우려면 `CHAT_ENABLED=false` 로 채팅 없이 부팅할 수 있습니다.

---

## 5. Frontend 실행

```powershell
cd frontend
npm install   # 최초 1회
npm run dev
```

접속: http://127.0.0.1:5173

---

## 6. 전체 실행 순서 (매일)

```
1. 인프라: cd BE\trip-docker; docker compose up -d   (MySQL·MongoDB·Redis·RabbitMQ)
   - docker 미사용 시: MySQL만 직접 기동(2절) + BE를 CHAT_ENABLED=false 로 실행(채팅 비활성)
2. BE: cd BE; .\gradlew.bat bootRun                   (포트 9090)
3. frontend: cd frontend; npm run dev                 (포트 5173)
```

### 축제 데이터 채우기 (최초 1회)
축제(`/api/festivals`)는 sync 배치를 1회 실행해야 데이터가 채워집니다(네트워크 + `TOUR_API_KEY` 필요).
배치를 실행하기 전에는 축제 목록이 빈 배열로 정상 반환됩니다. (인증된 요청으로 sync 트리거)
