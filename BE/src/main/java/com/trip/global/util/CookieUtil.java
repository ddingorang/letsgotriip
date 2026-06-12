package com.trip.global.util;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final JwtUtil jwtUtil;

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
                .secure(false) // TODO 운영 HTTPS 전환 시 secure(true)
                .build();
    }
}
