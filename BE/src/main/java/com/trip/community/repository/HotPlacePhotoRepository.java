// Created: 2026-06-15 23:24:59
package com.trip.community.repository;

import com.trip.community.entity.HotPlacePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotPlacePhotoRepository extends JpaRepository<HotPlacePhoto, Long> {

    List<HotPlacePhoto> findAllByHotPlaceIdOrderByDisplayOrderAsc(Long hotPlaceId);

    void deleteAllByHotPlaceId(Long hotPlaceId);
}
