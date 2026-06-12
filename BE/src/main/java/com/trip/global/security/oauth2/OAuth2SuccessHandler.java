package com.trip.global.security.oauth2;

import com.trip.global.util.CookieUtil;
import com.trip.global.util.JwtUtil;
import com.trip.global.util.RedisKeyNamingUtil;
import com.trip.user.dto.CustomUserInfoDto;
import com.trip.user.dto.RedisSessionDto;
import com.trip.user.entity.User;
import com.trip.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final RedisTemplate<String, RedisSessionDto> redisTemplate;
    private final UserRepository userRepository;

    @Value("${app.frontend.oauth-callback-url}")
    private String frontendCallbackUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

        // 1. SessionId(FamilyId), RefreshToken 생성, RefreshToken을 해시로 변환
        final String jti = jwtUtil.generateJTI();
        final String familyId = jwtUtil.generateFamilyId();
        final String refreshToken = jwtUtil.generateRefreshToken();
        final String rtHash = jwtUtil.generateSHA256Token(refreshToken);

        // 2. Redis에 저장할 User 정보 객체 생성
        final CustomUserInfoDto userInfo = CustomUserInfoDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .userRole(user.getUserRole())
                .build();

        // 3. Redis에 저장할 Session 정보 객체 생성
        final RedisSessionDto newRedisSessionDto = RedisSessionDto.builder()
                .customUserInfoDto(userInfo)
                .rtHash(rtHash)
                .currentAccessJti(jti)
                .prevRtHash(null)
                .rotatedAtEpoch(null)
                .build();

        // 4. Redis에 저장
        redisTemplate.opsForValue().set(RedisKeyNamingUtil.REFRESH_TOKEN_REDIS_KEY_NAME(familyId), newRedisSessionDto, jwtUtil.getREFRESH_TTL());

        // 5. 쿠키 설정
        cookieUtil.addAuthCookies(response, refreshToken, familyId);

        // 6. FE 콜백으로 리다이렉트 — access token은 URL에 싣지 않는다 (브라우저 히스토리/로그 노출 방지)
        //    FE는 콜백 도착 후 refresh 쿠키로 /auth/refresh를 호출해 access token을 획득한다.
        getRedirectStrategy().sendRedirect(request, response, frontendCallbackUrl);
    }
}
