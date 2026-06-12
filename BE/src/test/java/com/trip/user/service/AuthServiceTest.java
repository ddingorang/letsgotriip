package com.trip.user.service;

import com.trip.global.error.ResponseCode;
import com.trip.global.error.exception.handler.RedisHandler;
import com.trip.global.util.JwtUtil;
import com.trip.global.util.RedisKeyNamingUtil;
import com.trip.user.dto.CustomUserInfoDto;
import com.trip.user.dto.LoginResponseDto;
import com.trip.user.dto.RedisSessionDto;
import com.trip.user.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.trip.user.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, RedisSessionDto> redisTemplate;

    @Mock
    private ValueOperations<String, RedisSessionDto> valueOperations;

    @InjectMocks
    private AuthService authService;

    // 공통 fixture
    private static final String FAMILY_ID   = "test-family-id";
    private static final String REDIS_KEY   = RedisKeyNamingUtil.REFRESH_TOKEN_REDIS_KEY_NAME(FAMILY_ID);
    private static final String RAW_RT      = "raw-refresh-token";
    private static final String RT_HASH     = "hashed-rt";
    private static final String PREV_RT     = "prev-refresh-token";
    private static final String PREV_RT_HASH = "hashed-prev-rt";

    private CustomUserInfoDto userInfo;

    @BeforeEach
    void setUp() {
        userInfo = CustomUserInfoDto.builder()
                .userId(1L)
                .email("test@test.com")
                .nickname("tester")
                .userRole(UserRole.USER)
                .build();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    // -----------------------------------------------------------------------
    // refresh: 현재 RT 일치 → 회전 성공
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("refresh: 현재 RT 일치 → 새 토큰 회전 성공")
    void refresh_currentRt_rotateSuccess() {
        // given
        RedisSessionDto session = RedisSessionDto.builder()
                .customUserInfoDto(userInfo)
                .rtHash(RT_HASH)
                .currentAccessJti("old-jti")
                .prevRtHash(null)
                .rotatedAtEpoch(null)
                .build();

        given(valueOperations.get(REDIS_KEY)).willReturn(session);
        given(jwtUtil.generateSHA256Token(RAW_RT)).willReturn(RT_HASH);
        given(jwtUtil.generateJTI()).willReturn("new-jti");
        given(jwtUtil.generateRefreshToken()).willReturn("new-raw-rt");
        given(jwtUtil.generateSHA256Token("new-raw-rt")).willReturn("new-rt-hash");
        given(jwtUtil.getREFRESH_TTL()).willReturn(Duration.ofDays(7));
        given(jwtUtil.createAccessToken(any(), eq("new-jti"), eq(FAMILY_ID))).willReturn("new-access-token");

        // when
        LoginResponseDto result = authService.refresh(RAW_RT, FAMILY_ID);

        // then
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-raw-rt");
        verify(valueOperations).set(eq(REDIS_KEY), any(RedisSessionDto.class), eq(Duration.ofDays(7)));
    }

    // -----------------------------------------------------------------------
    // refresh: 이전 RT + overlap 5분 내 → 성공 (grace period)
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("refresh: 이전 RT이지만 overlap 5분 이내 → 성공")
    void refresh_prevRt_withinOverlapWindow_success() {
        // given — rotatedAtEpoch를 3분 전으로 설정 (5분 창 내)
        long threeMinutesAgo = Instant.now().getEpochSecond() - 180;

        RedisSessionDto session = RedisSessionDto.builder()
                .customUserInfoDto(userInfo)
                .rtHash("current-rt-hash")          // 현재 RT (일치 안 함)
                .currentAccessJti("old-jti")
                .prevRtHash(PREV_RT_HASH)            // 이전 RT 해시
                .rotatedAtEpoch(threeMinutesAgo)
                .build();

        given(valueOperations.get(REDIS_KEY)).willReturn(session);
        given(jwtUtil.generateSHA256Token(PREV_RT)).willReturn(PREV_RT_HASH);
        given(jwtUtil.getOVERLAP_WINDOW()).willReturn(Duration.ofMinutes(5));
        given(jwtUtil.generateJTI()).willReturn("new-jti");
        given(jwtUtil.generateRefreshToken()).willReturn("new-raw-rt");
        given(jwtUtil.generateSHA256Token("new-raw-rt")).willReturn("new-rt-hash");
        given(jwtUtil.getREFRESH_TTL()).willReturn(Duration.ofDays(7));
        given(jwtUtil.createAccessToken(any(), eq("new-jti"), eq(FAMILY_ID))).willReturn("new-access-token");

        // when
        LoginResponseDto result = authService.refresh(PREV_RT, FAMILY_ID);

        // then
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        verify(valueOperations).set(eq(REDIS_KEY), any(RedisSessionDto.class), eq(Duration.ofDays(7)));
    }

    // -----------------------------------------------------------------------
    // refresh: 불일치 RT → SESSION_REUSE_DETECTED + 세션 삭제
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("refresh: 불일치 RT → SESSION_REUSE_DETECTED 예외 + 세션 전체 삭제")
    void refresh_unknownRt_sessionReuseDetected() {
        // given — overlap 창 밖(10분 전)의 이전 RT도 매칭 안 됨
        long tenMinutesAgo = Instant.now().getEpochSecond() - 600;

        RedisSessionDto session = RedisSessionDto.builder()
                .customUserInfoDto(userInfo)
                .rtHash("current-rt-hash")
                .currentAccessJti("old-jti")
                .prevRtHash(PREV_RT_HASH)
                .rotatedAtEpoch(tenMinutesAgo)
                .build();

        String unknownRawRt = "completely-unknown-token";
        String unknownRtHash = "unknown-hash";

        given(valueOperations.get(REDIS_KEY)).willReturn(session);
        given(jwtUtil.generateSHA256Token(unknownRawRt)).willReturn(unknownRtHash);
        given(jwtUtil.getOVERLAP_WINDOW()).willReturn(Duration.ofMinutes(5));

        // when / then
        assertThatThrownBy(() -> authService.refresh(unknownRawRt, FAMILY_ID))
                .isInstanceOf(RedisHandler.class)
                .satisfies(ex -> {
                    RedisHandler handler = (RedisHandler) ex;
                    assertThat(handler.getErrorCode()).isEqualTo(ResponseCode.SESSION_REUSE_DETECTED);
                });

        // 세션 전체 삭제 확인
        verify(redisTemplate).delete(REDIS_KEY);
    }

    // -----------------------------------------------------------------------
    // logout: sessionId 있을 때 → Redis 세션 삭제
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("logout: sessionId 존재 → Redis 키 삭제 호출")
    void logout_withSessionId_deletesRedisKey() {
        // when
        authService.logout(FAMILY_ID);

        // then
        verify(redisTemplate).delete(REDIS_KEY);
    }

    // -----------------------------------------------------------------------
    // logout: sessionId null (쿠키 없음) → 멱등, 예외 없음
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("logout: sessionId null (쿠키 없음) → 예외 없이 멱등 처리")
    void logout_withNullSessionId_idempotentNoException() {
        // when
        authService.logout(null);

        // then — delete 호출되지 않아야 함
        verify(redisTemplate, never()).delete(anyString());
    }
}
