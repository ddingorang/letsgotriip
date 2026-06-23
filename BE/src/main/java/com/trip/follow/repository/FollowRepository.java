package com.trip.follow.repository;

import com.trip.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    /** 시드 reset — 마커 사용자가 관여한(팔로워/팔로위) 팔로우 관계 일괄 삭제 */
    @Modifying
    @Query("DELETE FROM Follow f WHERE f.followerId IN :userIds OR f.followeeId IN :userIds")
    int deleteByFollowerIdInOrFolloweeIdIn(@Param("userIds") List<Long> userIds);

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
