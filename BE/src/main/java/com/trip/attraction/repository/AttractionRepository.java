package com.trip.attraction.repository;

import com.trip.attraction.entity.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Attraction JPA 리포지토리
 * RepositoryConfig 화이트리스트에 com.trip.attraction.repository 추가 필요
 */
public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    Optional<Attraction> findByContentIdAndContentType(String contentId, Integer contentType);
}
