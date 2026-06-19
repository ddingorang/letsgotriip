package com.trip.global.security;

import com.trip.global.util.JwtUtil;
import com.trip.user.dto.CustomUserInfoDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                CustomUserInfoDto userInfo = jwtUtil.getUserInfo(token);
                UserPrincipal userPrincipal = new UserPrincipal(userInfo.userId());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        // ROLE_ 접두어 부여 → 향후 hasRole(...) 정합. JWT 클레임(userRole)은 enum name 그대로 유지된다.
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userInfo.userRole().name()))
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
