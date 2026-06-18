// Created: 2026-06-15 23:24:57
package com.trip.community.repository;

import com.trip.community.entity.HotPlace;
import com.trip.community.entity.enums.HotPlaceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotPlaceRepository extends JpaRepository<HotPlace, Long> {

    Page<HotPlace> findAllByStatusOrderByCreatedAtDesc(HotPlaceStatus status, Pageable pageable);

    List<HotPlace> findAllByStatus(HotPlaceStatus status);
}
