# syntax=docker/dockerfile:1

# ── build stage ──────────────────────────────────────────────────────────────
# Gradle 래퍼로 bootJar 생성 (테스트 제외, 데몬 미사용)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Gradle 래퍼/설정 먼저 복사 → 의존성 레이어 캐시 활용
COPY gradlew ./gradlew
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# 래퍼 실행 권한 보장 (Windows 체크아웃 대비)
RUN chmod +x ./gradlew

# 소스 복사 후 빌드
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# ── runtime stage ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# 빌드 산출물(bootJar) → app.jar (plain.jar 제외 위해 *-SNAPSHOT.jar 패턴 사용 가능하나
# bootJar 단일 산출물 기준으로 *.jar 매칭)
COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar /app/app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
