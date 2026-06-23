package com.trip.community.repository;

import com.trip.community.entity.HotPlaceLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HotPlaceLikeRepository extends JpaRepository<HotPlaceLike, Long> {

    Optional<HotPlaceLike> findByUserIdAndHotPlaceId(Long userId, Long hotPlaceId);

    boolean existsByUserIdAndHotPlaceId(Long userId, Long hotPlaceId);

    @Modifying
    @Query("delete from HotPlaceLike l where l.hotPlaceId in :hotPlaceIds")
    void deleteByHotPlaceIdIn(@Param("hotPlaceIds") List<Long> hotPlaceIds);
}
