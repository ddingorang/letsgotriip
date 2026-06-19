package com.trip.user.service;

import com.trip.user.dto.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.trip.global.error.ResponseCode;
import com.trip.global.error.exception.handler.RedisHandler;
import com.trip.global.error.exception.handler.UserHandler;
import com.trip.global.util.JwtUtil;
import com.trip.global.util.RedisKeyNamingUtil;
import com.trip.user.dto.*;
import com.trip.user.entity.PasswordResetToken;
import com.trip.user.entity.User;
import com.trip.user.repository.PasswordResetTokenRepository;
import com.trip.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, RedisSessionDto> redisTemplate;

    /** 비밀번호 재설정 토큰 유효시간(분) */
    private static final long PASSWORD_RESET_TTL_MINUTES = 30L;
    private static final String DEMO_NOTE = "데모: 실제로는 이메일로 전송됩니다";

    @Transactional
    public SignupResponseDto signup(SignupRequestDto signupRequestDto) {

        final String email = signupRequestDto.email();
        final String password = signupRequestDto.password();

        // 동일한 이메일을 가진 유저가 존재하면 에러 처리
        final Boolean isExistUser = userRepository.existsByEmail(email);
        if (isExistUser) throw new UserHandler(ResponseCode.USER_ALREADY_EXISTS);

        final String encodedPassword = passwordEncoder.encode(password);

        final User user = User.of(signupRequestDto, encodedPassword);

        // 동시 가입 race: exists 체크를 통과해도 email UNIQUE 제약 위반이 날 수 있다
        try {
            final User savedUser = userRepository.save(user);
            return new SignupResponseDto(savedUser.getId(), savedUser.getEmail());
        } catch (DataIntegrityViolationException e) {
            // 동시 가입 race로 email UNIQUE 제약을 위반한 경우에만 409로 처리한다.
            // 그 외(NOT NULL 등) 제약 위반은 "이미 존재"로 가리지 않고 그대로 노출시킨다.
            if (userRepository.existsByEmail(email)) {
                throw new UserHandler(ResponseCode.USER_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    public LoginResponseDto login(LoginRequestDto loginRequestDto) {

        final String email = loginRequestDto.email();
        final String password = loginRequestDto.password();

        final User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ResponseCode.USER_NOT_FOUND));

        // 비밀번호가 일치하지 않으면 예외 던짐
        if (!passwordEncoder.matches(password, user.getPassword())) throw new UserHandler(ResponseCode.USER_PASSWORD_MISMATCH);

        // 비활성화(삭제 요청) 회원은 로그인 불가
        if (!user.isStatus()) throw new UserHandler(ResponseCode.USER_NOT_FOUND);

        // 로그인에 성공하면 AccessToken을 생성, RefreshToken을 해시로 변환.
        // RefreshToken을 생성해서 Redis에 저장하고, 클라이언트에는 쿠키로 보내준다.
        // 클라이언트는 RefreshToken을 이용해서 짧은 만료기간을 가진 AccessToken을 재발급 받을 수 있다.
        
        // 1. SessionId(FamilyId), RefreshToken 생성, RefreshToken을 해시로 변환
        final String jti = jwtUtil.generateJTI();
        final String familyId = jwtUtil.generateFamilyId(); // familyId 생성
        final String refreshToken = jwtUtil.generateRefreshToken(); // Refresh Token 생성
        final String rtHash = jwtUtil.generateSHA256Token(refreshToken); // refresh Token을 해시로 변환

        // 2. Redis에 저장할 User 정보 객체 생성
        final CustomUserInfoDto userInfo = CustomUserInfoDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .userRole(user.getUserRole())
                .build();

        // 3. Redis에 저장할 User 정보, RefreshToken 해시값, 만료기간 객체 생성
        final RedisSessionDto newRedisSessionDto = RedisSessionDto.builder()
                .customUserInfoDto(userInfo)
                .rtHash(rtHash)
                .currentAccessJti(jti)
                .prevRtHash(null) // 최초 로그인 시 이전 토큰 없음
                .rotatedAtEpoch(null)
                .build();

        // 4. Redis에 저장
        // Redis에 User 정보와 sessionId, refreshToken, 만료기간을 기록한다.
        redisTemplate.opsForValue().set(RedisKeyNamingUtil.REFRESH_TOKEN_REDIS_KEY_NAME(familyId), newRedisSessionDto, jwtUtil.getREFRESH_TTL());

        // RefreshToken과 familyId, 유저 정보를 기반으로 AccessToken을 만들고 반환.
        return LoginResponseDto.builder()
                .accessToken(jwtUtil.createAccessToken(userInfo, jti, familyId))
                .userId(user.getId())
                .refreshToken(refreshToken)
                .familyId(familyId)
                .build();
    }

    /**
     * 로그아웃: Redis에서 해당 세션(familyId)을 삭제한다.
     *
     * [정책] access token은 만료(1h)까지 유효하다.
     * denylist 미도입은 의도적 결정 — 구현 비용 대비 위험도가 낮다고 판단.
     * 로그아웃 후 탈취된 access token은 최대 1h 내 만료되므로 수용 가능한 위험.
     *
     * @param sessionId 쿠키에서 온 sessionId (familyId). null이면 멱등하게 무시.
     */
    public void logout(String sessionId) {
        if (sessionId != null) {
            redisTemplate.delete(RedisKeyNamingUtil.REFRESH_TOKEN_REDIS_KEY_NAME(sessionId));
        }
    }

    public LoginResponseDto refresh(String refreshToken, String familyId) {
        // [한계] 동시 refresh 요청은 FE single-flight + 5분 overlap window로 완화.
        // 완전한 원자성(read-compare-write)은 Lua CAS가 필요하나 현재 미적용(추후).

        // 클라이언트가 쿠키로 보낸 refreshToken을 hash로 변환.
        final String incomingRtHash = jwtUtil.generateSHA256Token(refreshToken);

        // 1. 세션 조회 (Redis에 동일한 키가 있는지 확인)
        final RedisSessionDto sessionDto = Optional.ofNullable(
                redisTemplate.opsForValue().get(RedisKeyNamingUtil.REFRESH_TOKEN_REDIS_KEY_NAME(familyId))
        ).orElseThrow(() -> new RedisHandler(ResponseCode.SESSION_NOT_FOUND));

        // 2. 현재 RT 검증 (현재 유효한 해시인지 비교)
        // Refresh 되면 rtHash 값이 바뀐다. 즉, 이전 RefreshToken을 사용할 경우, 인증에 실패한다.
        final boolean isCurrentRt = sessionDto.rtHash().equals(incomingRtHash);

        // 3. 이전 RT 검증 (overlap 허용)
        // 재사용 탐지: 새로운 토큰이 생성 되었는데, 이전 토큰이 사용됨
        // 네트워크 문제나 동시 refresh를 할 경우, 정상적인 접근에도 인증에 실패할 수 있다.
        // 조금의 오차를 허용해줘서 사용자 경험을 개선한다.
        boolean isPrevRt = false;
        final long now = Instant.now().getEpochSecond();
        if (!isCurrentRt && sessionDto.prevRtHash() != null && sessionDto.rotatedAtEpoch() != null) {

            long secondsSinceRotation = now - sessionDto.rotatedAtEpoch();

            isPrevRt = sessionDto.prevRtHash().equals(incomingRtHash)
                    && secondsSinceRotation <= jwtUtil.getOVERLAP_WINDOW().toSeconds();
        }

        // 4. 둘 다 아니면 -> 재사용 공격 또는 만료
        if (!isCurrentRt && !isPrevRt) {

            // 재사용 탐지 -> 세션 전체 삭제
            redisTemplate.delete(RedisKeyNamingUtil.REFRESH_TOKEN_REDIS_KEY_NAME(familyId));
            throw new RedisHandler(ResponseCode.SESSION_REUSE_DETECTED);
        }

        // 5. 회전(새로운 RT 발급)
        final String jti = jwtUtil.generateJTI();
        final String newRefreshToken = jwtUtil.generateRefreshToken(); // 새로운 refreshToken 발급
        final String newRtHash = jwtUtil.generateSHA256Token(newRefreshToken); // 새로운 rtHash 발급

        // AccessToken 생성용 User 정보 초기화
        final CustomUserInfoDto customUserInfoDto = CustomUserInfoDto.builder()
                .userId(sessionDto.customUserInfoDto().userId())
                .email(sessionDto.customUserInfoDto().email())
                .nickname(sessionDto.customUserInfoDto().nickname())
                .userRole(sessionDto.customUserInfoDto().userRole())
                .build();

        // Redis에 저장할 세션, RefreshToken 정보
        final RedisSessionDto newRedisSessionDto = RedisSessionDto.builder()
                .customUserInfoDto(customUserInfoDto)
                .rtHash(newRtHash)
                .currentAccessJti(jti)
                // 이전 RT 기록: overlap 요청이었으면 prevRtHash 유지, 정상 rotate면 현재 걸 prev로
                .prevRtHash(isCurrentRt ? sessionDto.rtHash() : sessionDto.prevRtHash())
                .rotatedAtEpoch(isCurrentRt ? now : sessionDto.rotatedAtEpoch()) // prevRt면 갱신 안 함
                .build();

        // Redis에 새로운 세션 + refresh Token 저장
        redisTemplate.opsForValue().set(RedisKeyNamingUtil.REFRESH_TOKEN_REDIS_KEY_NAME(familyId), newRedisSessionDto, jwtUtil.getREFRESH_TTL());

        final String newAccessToken = jwtUtil.createAccessToken(customUserInfoDto, jti, familyId); // 새로운 AccessToken 발급
        return LoginResponseDto.builder()
                .refreshToken(newRefreshToken)
                .accessToken(newAccessToken)
                .userId(sessionDto.customUserInfoDto().userId())
                .familyId(familyId)
                .build();
    }

    /**
     * 비밀번호 재설정 요청 (데모).
     * 계정 열거 방지를 위해 이메일 존재 여부와 무관하게 동일한 형태로 응답한다.
     * - 존재하면: UUID 토큰을 생성/저장하고 토큰을 응답에 담아 반환(데모이므로 직접 노출).
     * - 없으면: token/expiresAt 을 null 로 채워 반환.
     */
    @Transactional
    public PasswordResetTokenResponse requestPasswordReset(String email) {

        final Optional<User> userOpt = userRepository.findByEmail(email);

        // 계정 열거 방지: 존재하지 않아도 동일 형태로 응답
        if (userOpt.isEmpty()) {
            return new PasswordResetTokenResponse(null, null, DEMO_NOTE);
        }

        final User user = userOpt.get();
        final String token = UUID.randomUUID().toString();
        final LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PASSWORD_RESET_TTL_MINUTES);

        final PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .userId(user.getId())
                .expiresAt(expiresAt)
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        return new PasswordResetTokenResponse(token, expiresAt, DEMO_NOTE);
    }

    /**
     * 비밀번호 재설정 확정.
     * 토큰을 검증(존재/미사용/미만료)하고 통과하면 비밀번호를 변경한 뒤 토큰을 사용 처리한다.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {

        final PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new UserHandler(ResponseCode._BAD_REQUEST));

        // 이미 사용했거나 만료된 토큰은 무효
        if (resetToken.isUsed() || resetToken.isExpired(LocalDateTime.now())) {
            throw new UserHandler(ResponseCode._BAD_REQUEST);
        }

        final User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new UserHandler(ResponseCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(newPassword));
        resetToken.markUsed();
    }
}
