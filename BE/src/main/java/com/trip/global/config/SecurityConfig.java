package com.trip.global.config;

import com.trip.global.security.JwtAuthenticationFilter;
import com.trip.global.security.oauth2.CustomOAuth2UserService;
import com.trip.global.security.oauth2.OAuth2SuccessHandler;
import com.trip.global.util.JwtUtil;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtUtil jwtUtil;

    @Value("${app.frontend.login-url}")
    private String frontendLoginUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // TODO: 운영 단계에서는 Security 패턴 엄격하게 변경
        return http
                .cors(Customizer.withDefaults())                                // WebConfig의 CorsConfigurationSource 사용
                .csrf(AbstractHttpConfigurer::disable)                          // WebSocket은 CSRF 토큰 사용 불가
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // JWT 사용으로 세션 미사용
                )
                .authorizeHttpRequests(auth -> auth
                        // sendError(403 등)로 발생하는 ERROR 디스패치가 401로 덮이지 않도록 허용
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/ws/**").permitAll()                  // WebSocket 엔드포인트 인증 제외
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/preprocessing/**").authenticated()
                        // 운영성 엔드포인트는 ADMIN 한정 (공개 GET보다 먼저 매칭)
                        .requestMatchers(HttpMethod.POST, "/api/festivals/sync").hasAuthority("ADMIN")
                        // 비회원 공개 조회 (탐색 도메인)
                        .requestMatchers(HttpMethod.GET, "/api/festivals/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/attractions/**").permitAll()
                        // 커뮤니티 비회원 공개 조회
                        .requestMatchers(HttpMethod.GET, "/community/posts", "/community/posts/*", "/community/posts/*/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/community/hotplaces", "/community/hotplaces/*").permitAll()
                        // 동행 비회원 공개 조회
                        .requestMatchers(HttpMethod.GET, "/companion/posts", "/companion/posts/*").permitAll()
                        // 업로드 파일 공개 접근
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        // API 문서
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                // SPA용: 미인증 API 요청은 OAuth 로그인 리다이렉트(302) 대신 401 반환
                // (구글 로그인은 FE가 /oauth2/authorization/google로 직접 진입하므로 영향 없음)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler((req, res, ex) ->
                                res.sendRedirect(frontendLoginUrl + "?error=oauth_failed"))
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
