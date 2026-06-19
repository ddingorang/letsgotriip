package com.trip.follow.service;

import com.trip.follow.dto.FollowStatusResponse;
import com.trip.follow.dto.FollowUserResponse;
import com.trip.follow.entity.Follow;
import com.trip.follow.repository.FollowRepository;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.user.entity.User;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    /**
     * 팔로우 토글. 이미 팔로우 중이면 해제, 아니면 팔로우.
     * @return 토글 후 팔로우 상태 (true=팔로우, false=해제)
     */
    @Transactional
    public boolean toggle(Long followerId, Long targetUserId) {
        if (targetUserId == null || followerId.equals(targetUserId)) {
            throw new GeneralException(ResponseCode._BAD_REQUEST);
        }
        if (!userRepository.existsById(targetUserId)) {
            throw new GeneralException(ResponseCode.USER_NOT_FOUND);
        }

        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, targetUserId)) {
            followRepository.deleteByFollowerIdAndFolloweeId(followerId, targetUserId);
            return false;
        }

        try {
            followRepository.save(Follow.builder()
                    .followerId(followerId)
                    .followeeId(targetUserId)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 unique 제약 위반 시 — 이미 팔로우 상태로 간주
            return true;
        }
        return true;
    }

    /** 요청자가 대상을 팔로우 중인지 */
    public boolean isFollowing(Long followerId, Long targetUserId) {
        if (followerId == null || targetUserId == null) {
            return false;
        }
        return followRepository.existsByFollowerIdAndFolloweeId(followerId, targetUserId);
    }

    /**
     * 팔로우 상태 + 카운트. requesterId가 null(비로그인)이면 following=false.
     * 대상 미존재 시 USER_NOT_FOUND.
     */
    public FollowStatusResponse getStatus(Long requesterId, Long targetUserId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new GeneralException(ResponseCode.USER_NOT_FOUND);
        }
        boolean following = isFollowing(requesterId, targetUserId);
        long followerCount = followRepository.countByFolloweeId(targetUserId);
        long followingCount = followRepository.countByFollowerId(targetUserId);
        return new FollowStatusResponse(targetUserId, following, followerCount, followingCount);
    }

    /** 팔로워 수 — 대상을 팔로우하는 사람 수 */
    public long countFollowers(Long userId) {
        return followRepository.countByFolloweeId(userId);
    }

    /** 팔로잉 수 — 대상이 팔로우하는 사람 수 */
    public long countFollowing(Long userId) {
        return followRepository.countByFollowerId(userId);
    }

    /** 내가 팔로우한 사용자 목록 (최신순) */
    public List<FollowUserResponse> getFollowing(Long followerId) {
        List<Follow> follows = followRepository.findByFollowerIdOrderByCreatedAtDesc(followerId);
        if (follows.isEmpty()) {
            return List.of();
        }
        List<Long> followeeIds = follows.stream().map(Follow::getFolloweeId).toList();
        Map<Long, User> userMap = userRepository.findAllById(followeeIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        // 팔로우 순서(최신순)를 유지하면서, 삭제된 사용자는 제외
        return followeeIds.stream()
                .map(userMap::get)
                .filter(u -> u != null)
                .map(FollowUserResponse::from)
                .toList();
    }
}
