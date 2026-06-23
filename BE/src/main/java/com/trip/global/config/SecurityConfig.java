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
                        .requestMatchers("/ws/**", "/ws-sockjs/**").permitAll() // WebSocket 핸드셰이크 인증 제외(인증은 STOMP CONNECT 프레임에서)
                        .requestMatchers("/auth/**").permitAll()
                        // 개발용 시드 엔드포인트 — 컨트롤러가 자체적으로 app.seed.api.enabled + X-Seed-Secret 으로 게이팅한다.
                        .requestMatchers("/api/dev/**").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/preprocessing/**").authenticated()
                        // 인증된 사용자라면 수동 동기화 허용
                        .requestMatchers(HttpMethod.POST, "/api/festivals/sync").authenticated()
                        // 관광지 리뷰 작성/수정/삭제는 인증 필수 (GET 목록은 아래 attractions GET permitAll로 공개)
                        .requestMatchers(HttpMethod.POST, "/api/attractions/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/attractions/*/reviews/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/attractions/*/reviews/**").authenticated()
                        // 관광지 좋아요(하트) 토글은 인증 필수 (GET 상태/큐레이션은 아래 attractions GET permitAll 로 공개)
                        .requestMatchers(HttpMethod.POST, "/api/attractions/*/like").authenticated()
                        // 비회원 공개 조회 (탐색 도메인)
                        .requestMatchers(HttpMethod.GET, "/api/festivals/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/attractions/**").permitAll()
                        // 통합검색 — 비회원 공개 탐색
                        .requestMatchers(HttpMethod.GET, "/api/search").permitAll()
                        // 찜(즐겨찾기) — 본인 리소스, 인증 필수
                        .requestMatchers("/api/favorites/**").authenticated()
                        // 팔로우 — 토글/상태/내 팔로잉 전부 인증 필수
                        .requestMatchers("/api/follows/**").authenticated()
                        // 여행 맥락 정보(날씨/충전소/뉴스) — 전부 비회원 공개
                        .requestMatchers("/api/context/**").permitAll()
                        // 공유 토큰으로 계획 공개 조회 (소유 검증 없이 접근, 토큰만으로)
                        .requestMatchers(HttpMethod.GET, "/api/plans/shared/**").permitAll()
                        // 공지 변경(생성/수정/삭제)은 ADMIN 전용 — GET permitAll 보다 먼저 선언
                        .requestMatchers(HttpMethod.POST, "/api/notices", "/api/notices/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/notices/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/notices/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/notices/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/notices", "/api/notices/**").permitAll()
                        // 커뮤니티 비회원 공개 조회
                        .requestMatchers(HttpMethod.GET, "/community/posts", "/community/posts/*", "/community/posts/*/comments").permitAll()
                        // 핫플 승인 대기 목록은 ADMIN 전용 — 아래 hotplaces/* permitAll 와일드카드에 포함되지 않도록 먼저 선언
                        .requestMatchers(HttpMethod.GET, "/community/hotplaces/pending").hasRole("ADMIN")
                        // 핫플 승인/거절은 매처 레벨에서 ADMIN 강제 — GET hotplaces permitAll 와일드카드보다 먼저 선언
                        .requestMatchers(HttpMethod.POST, "/community/hotplaces/*/approve", "/community/hotplaces/*/reject").hasRole("ADMIN")
                        // 핫플 좋아요(하트) — 토글은 인증, 상태조회는 공개(비로그인 liked=false). GET hotplaces/* 보다 먼저.
                        .requestMatchers(HttpMethod.POST, "/community/hotplaces/*/like").authenticated()
                        .requestMatchers(HttpMethod.GET, "/community/hotplaces/*/like").permitAll()
                        .requestMatchers(HttpMethod.GET, "/community/hotplaces", "/community/hotplaces/*").permitAll()
                        // 동행 — 내 참여 방은 인증 필수 (공개 매처보다 먼저 선언해 /my가 permitAll에 포함되지 않도록)
                        .requestMatchers(HttpMethod.GET, "/companion/posts/my").authenticated()
                        // 동행 비회원 공개 조회
                        .requestMatchers(HttpMethod.GET, "/companion/posts", "/companion/posts/*").permitAll()
                        // 여행 스토리 — 본인 소유 리소스, 인증 필수
                        .requestMatchers("/api/stories/**").authenticated()
                        // 단체할인 — 데모 목록은 공개(구체 매처를 그룹 인증 매처보다 먼저)
                        .requestMatchers(HttpMethod.GET, "/api/groups/discounts").permitAll()
                        // 그룹 — 생성/가입/탈퇴/내 목록 등 인증 필수
                        .requestMatchers("/api/groups/**").authenticated()
                        // 앨범 공유 토큰 공개뷰 — 인증 없이 공유 링크로 앨범 조회 (다른 스트림이 구현)
                        .requestMatchers(HttpMethod.GET, "/api/albums/shared/**").permitAll()
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
