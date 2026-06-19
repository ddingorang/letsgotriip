// Created: 2026-06-15 23:42:18
package com.trip.companion.repository;

import com.trip.companion.entity.CompanionPost;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanionPostRepository extends JpaRepository<CompanionPost, Long> {

    List<CompanionPost> findAllByDeletedFalseOrderByIdDesc(Pageable pageable);

    List<CompanionPost> findAllByDeletedFalseAndIdLessThanOrderByIdDesc(Long cursorId, Pageable pageable);

    List<CompanionPost> findAllByDeletedFalseAndChatRoomIdIn(List<Long> chatRoomIds);

    // 통합검색: 제목 또는 지역 부분일치(대소문자 무시) + 미삭제, DB 상한
    @Query("""
            select p from CompanionPost p
            where p.deleted = false
              and (lower(p.title) like lower(concat('%', :keyword, '%'))
                or lower(p.region) like lower(concat('%', :keyword, '%')))
            order by p.id desc
            """)
    List<CompanionPost> searchByTitleOrRegion(@Param("keyword") String keyword, Pageable pageable);

    /** 정원 승인 시 동시성 직렬화를 위한 비관적 쓰기 락 조회 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from CompanionPost p where p.id = :id")
    Optional<CompanionPost> findByIdForUpdate(@Param("id") Long id);
}
