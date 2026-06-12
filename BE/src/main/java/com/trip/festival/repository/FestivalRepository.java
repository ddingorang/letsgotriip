// Created: 2026-06-08 15:32:34
package com.trip.festival.repository;

import com.trip.festival.entity.Festival;
import com.trip.festival.entity.FestivalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FestivalRepository extends JpaRepository<Festival, String> {

    List<Festival> findByAreaCode(String areaCode);

    List<Festival> findByStatus(FestivalStatus status);

    List<Festival> findByAreaCodeAndStatus(String areaCode, FestivalStatus status);

    List<Festival> findByStatusNot(FestivalStatus status);
}
