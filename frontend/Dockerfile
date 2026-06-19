# syntax=docker/dockerfile:1

# ── build stage ──────────────────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

# 의존성 레이어 캐시
COPY package.json package-lock.json ./
# 크로스플랫폼(호스트 Windows lock → alpine/musl 빌드) 드리프트 방지를 위해
# npm ci 대신 install 사용 — 타깃 플랫폼에서 옵셔널 네이티브 deps(@emnapi/* 등)를 해석.
RUN npm install --no-audit --no-fund

# 카카오 지도 키는 빌드 ARG로 주입 (없으면 빈 값)
ARG VITE_KAKAO_MAP_KEY=
ENV VITE_KAKAO_MAP_KEY=$VITE_KAKAO_MAP_KEY

# 데모 로그인 프리필 — 비어있지 않으면 로그인 폼을 미리 채움(운영은 빈 값으로 두면 빈칸)
ARG VITE_DEMO_EMAIL=
ENV VITE_DEMO_EMAIL=$VITE_DEMO_EMAIL
ARG VITE_DEMO_PASSWORD=
ENV VITE_DEMO_PASSWORD=$VITE_DEMO_PASSWORD

# API base URL은 빈 값 → axios baseURL='' → 모든 호출이 상대경로
# (nginx 리버스 프록시가 /api·/auth 등을 backend로 라우팅)
ENV VITE_API_BASE_URL=

COPY . .
RUN npm run build

# ── runtime stage ────────────────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# 기본 conf 제거 후 커스텀 conf 적용
RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 빌드 산출물 정적 서빙
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
