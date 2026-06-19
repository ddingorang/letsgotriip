package com.trip.user.service;

import com.trip.community.service.FileStorageService;
import com.trip.global.error.ResponseCode;
import com.trip.global.error.exception.handler.UserHandler;
import com.trip.global.util.RedisKeyNamingUtil;
import com.trip.user.dto.PreferenceUpdateRequestDto;
import com.trip.user.dto.RedisSessionDto;
import com.trip.user.dto.UserProfileResponseDto;
import com.trip.user.dto.UserUpdateRequestDto;
import com.trip.user.entity.User;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, RedisSessionDto> redisTemplate;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public UserProfileResponseDto getProfile(Long userId) {
        User user = findActiveUserById(userId);
        return toProfileResponse(user);
    }

    /** 프로필 이미지 업로드: 파일 저장 후 URL 을 사용자 프로필에 반영하고 갱신된 프로필을 반환. */
    @Transactional
    public UserProfileResponseDto updateProfileImage(Long userId, MultipartFile file) {
        User user = findActiveUserById(userId);
        String imageUrl = fileStorageService.store(file);
        user.updateProfileImage(imageUrl);
        return toProfileResponse(user);
    }

    private UserProfileResponseDto toProfileResponse(User user) {
        return UserProfileResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .name(user.getName())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .preferredInterests(splitInterests(user.getPreferredInterests()))
                .preferredCompanion(user.getPreferredCompanion())
                .userRole(user.getUserRole())
                .build();
    }

    @Transactional
    public void updateProfile(Long userId, UserUpdateRequestDto updateDto) {
        User user = findActiveUserById(userId);

        // 도메인 모델의 비즈니스 메서드를 사용하여 프로필 업데이트 (유효성 검증 포함)
        user.updateProfile(updateDto.nickname(), updateDto.profileImageUrl(), updateDto.bio());
    }

    /** 온보딩 취향설문 저장. 관심사 목록은 콤마 구분 문자열로 직렬화한다. */
    @Transactional
    public void updatePreferences(Long userId, PreferenceUpdateRequestDto request) {
        User user = findActiveUserById(userId);

        String interests = (request.interests() == null)
                ? null
                : request.interests().stream()
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(","));

        user.updatePreferences(interests, request.companion());
    }

    /** 콤마 구분 관심사 문자열을 리스트로 역직렬화. null/빈 값이면 빈 리스트. */
    private List<String> splitInterests(String interests) {
        if (interests == null || interests.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(interests.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * 회원 탈퇴: Soft Delete 후 현재 기기의 Redis 세션을 삭제하고 쿠키를 만료시킨다.
     *
     * [한계] 1인 1세션 구조가 아니므로, 현재 요청의 sessionId(쿠키)에 해당하는 세션만 삭제된다.
     * 다른 기기에서 발급된 세션은 해당 refresh token이 만료될 때까지(최대 7일) 유효하다.
     * 다중 기기 전체 세션 무효화가 필요하면 userId→[sessionId] 역색인 도입이 필요하다.
     *
     * @param userId    탈퇴할 사용자 ID
     * @param sessionId 현재 기기의 sessionId(familyId). null이면 세션 삭제를 건너뛴다.
     */
    @Transactional
    public void withdraw(Long userId, String sessionId) {
        User user = findActiveUserById(userId);

        // 회원 탈퇴 처리 (Soft Delete)
        user.withdraw();

        // 현재 기기 세션 삭제
        if (sessionId != null) {
            redisTemplate.delete(RedisKeyNamingUtil.REFRESH_TOKEN_REDIS_KEY_NAME(sessionId));
        }
    }

    private User findActiveUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(ResponseCode.USER_NOT_FOUND));
        
        if (!user.isStatus()) {
            throw new UserHandler(ResponseCode.USER_NOT_FOUND); // 혹은 별도의 탈퇴된 유저 에러
        }
        return user;
    }
}
