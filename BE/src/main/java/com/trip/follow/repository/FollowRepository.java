package com.trip.follow.repository;

import com.trip.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    /** 팔로우 여부 */
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    /** 팔로우 해제 (삭제된 건수 반환) */
    long deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    /** 팔로워 수 — 해당 사용자를 팔로우하는 사람 수 */
    long countByFolloweeId(Long followeeId);

    /** 팔로잉 수 — 해당 사용자가 팔로우하는 사람 수 */
    long countByFollowerId(Long followerId);

    /** 해당 사용자가 팔로우한 관계 목록 (최신순) */
    List<Follow> findByFollowerIdOrderByCreatedAtDesc(Long followerId);
}
