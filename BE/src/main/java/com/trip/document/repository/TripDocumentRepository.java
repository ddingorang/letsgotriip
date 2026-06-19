package com.trip.document.repository;

import com.trip.document.entity.TripDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripDocumentRepository extends JpaRepository<TripDocument, Long> {

    List<TripDocument> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<TripDocument> findByIdAndUserId(Long id, Long userId);
}
