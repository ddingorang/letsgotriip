# 프로젝트 셋업 가이드

## 프로젝트 구조

```
pjt/
├── backend/    # Spring Boot 3.5.9 + Spring AI 1.1.2
├── frontend/   # Vue 3.5 + Vite
└── docs/
```

---

## 1. 필수 환경

| 항목 | 버전 | 설치 경로 |
|------|------|-----------|
| Java | 17 (OpenJDK) | `C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot` |
| MySQL | 8.4.9 | `C:\Program Files\MySQL\MySQL Server 8.4` |
| Node.js | 18+ | - |

### JAVA_HOME 설정 (최초 1회)
```powershell
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot", "User")
```

---

## 2. MySQL 시작

> MySQL은 서비스가 아닌 수동 실행으로 설정되어 있습니다.

### PC 시작 시마다 실행
```powershell
Start-Process -FilePath "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" `
  -ArgumentList '--datadir="C:\ProgramData\MySQL\data" --port=3306' -WindowStyle Hidden
```

### 영구 서비스 등록 (관리자 터미널에서 1회만)
```powershell
# 관리자 권한 PowerShell에서 실행
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" --install MySQL84 --datadir="C:\ProgramData\MySQL\data"
Start-Service MySQL84
```

### DB 접속 정보
| 항목 | 값 |
|------|----|
| Host | localhost:3306 |
| Database | ssafy_ai |
| Username | ssafy |
| Password | ssafy |
| Root Password | root |

---

## 3. API 키 설정

`backend/src/main/resources/application-local.properties` 파일에 입력합니다.  
이 파일은 `.gitignore`에 포함되어 있어 Git에 커밋되지 않습니다.

```properties
ssafy.gms.api-key=<SSAFY GMS 키>
api.openweather.key=<OpenWeather 키>
api.tavily.key=<Tavily 키>
```

### API 키 발급처

| 키 | 발급처 | 무료 한도 |
|----|--------|-----------|
| `ssafy.gms.api-key` | SSAFY 포털 / 강사님 | - |
| `api.openweather.key` | https://home.openweathermap.org/api_keys | 월 1,000,000 calls |
| `api.tavily.key` | https://app.tavily.com | 월 1,000 searches |

---

## 4. Backend 실행

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

접속: http://localhost:8080

> 최초 실행 시 Maven 의존성 다운로드로 수 분 소요될 수 있습니다.

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
1. MySQL 시작 (위 2번 명령어)
2. backend: .\mvnw.cmd spring-boot:run
3. frontend: npm run dev
```
