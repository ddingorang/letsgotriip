// Created: 2026-06-15 23:42:18
package com.trip.companion.repository;

import com.trip.companion.entity.CompanionPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanionPostRepository extends JpaRepository<CompanionPost, Long> {

    List<CompanionPost> findAllByDeletedFalseOrderByIdDesc(Pageable pageable);

    List<CompanionPost> findAllByDeletedFalseAndIdLessThanOrderByIdDesc(Long cursorId, Pageable pageable);
}
