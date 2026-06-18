// Created: 2026-06-15 23:42:19
package com.trip.companion.repository;

import com.trip.companion.entity.CompanionApplication;
import com.trip.companion.entity.CompanionPost;
import com.trip.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanionApplicationRepository extends JpaRepository<CompanionApplication, Long> {

    boolean existsByCompanionPostAndApplicant(CompanionPost post, User applicant);

    Optional<CompanionApplication> findByIdAndCompanionPost(Long id, CompanionPost post);

    List<CompanionApplication> findAllByCompanionPost(CompanionPost post);
}
