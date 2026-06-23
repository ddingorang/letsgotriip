// Created: 2026-06-15 23:24:57
package com.trip.community.repository;

import com.trip.community.entity.HotPlace;
import com.trip.community.entity.enums.HotPlaceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HotPlaceRepository extends JpaRepository<HotPlace, Long> {

    Page<HotPlace> findAllByStatusOrderByCreatedAtDesc(HotPlaceStatus status, Pageable pageable);

    // 인기순('지금 뜨는 여행지') — 좋아요 desc, 동점은 id desc tie-breaker
    Page<HotPlace> findAllByStatusOrderByLikeCountDescIdDesc(HotPlaceStatus status, Pageable pageable);

    List<HotPlace> findAllByStatus(HotPlaceStatus status);

    // 통합검색: 이름 또는 주소 부분일치(대소문자 무시) + 상태 필터, DB 상한
    @Query("""
            select h from HotPlace h
            where h.status = :status
              and (lower(h.name) like lower(concat('%', :keyword, '%'))
                or lower(h.address) like lower(concat('%', :keyword, '%')))
            order by h.createdAt desc
            """)
    List<HotPlace> searchByNameOrAddress(@Param("status") HotPlaceStatus status,
                                         @Param("keyword") String keyword,
                                         Pageable pageable);

    /** 시드 reset — 마커 사용자가 등록한 핫플 */
    List<HotPlace> findAllBySubmitter_IdIn(List<Long> submitterIds);
}
