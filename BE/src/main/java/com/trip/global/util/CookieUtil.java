package com.trip.global.util;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final JwtUtil jwtUtil;

    // 환경별 Secure 플래그 제어 — 운영(HTTPS) 기본 true, 로컬 개발은 application.yaml에서 false로 오버라이드
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    public void addAuthCookies(HttpServletResponse response,
                                      String refreshToken,
                                      String sessionId) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie("refreshToken", refreshToken, jwtUtil.getREFRESH_TTL()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie("sessionId", sessionId, jwtUtil.getREFRESH_TTL()).toString());
    }

    /**
     * 인증 쿠키(refreshToken, sessionId)를 만료시킨다.
     * Max-Age=0 으로 설정하면 브라우저가 즉시 쿠키를 삭제한다.
     */
    public void expireAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie("refreshToken", "", Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie("sessionId", "", Duration.ZERO).toString());
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .secure(cookieSecure) // 운영 HTTPS는 true, 로컬 HTTP는 app.cookie.secure=false로 허용
                .build();
    }
}
