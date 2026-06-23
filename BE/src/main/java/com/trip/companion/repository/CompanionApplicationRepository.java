// Created: 2026-06-15 23:42:19
package com.trip.companion.repository;

import com.trip.companion.entity.CompanionApplication;
import com.trip.companion.entity.CompanionPost;
import com.trip.companion.entity.enums.ApplicationStatus;
import com.trip.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanionApplicationRepository extends JpaRepository<CompanionApplication, Long> {

    /** 시드 reset — 주어진 동행글들의 신청 일괄 삭제 */
    @Modifying
    @Query("DELETE FROM CompanionApplication a WHERE a.companionPost.id IN :postIds")
    int deleteByCompanionPostIdIn(@Param("postIds") List<Long> postIds);

    boolean existsByCompanionPostAndApplicantAndStatusNot(CompanionPost post, User applicant, ApplicationStatus status);

    boolean existsByCompanionPostAndApplicant_IdAndStatusNot(CompanionPost post, Long applicantId, ApplicationStatus status);

    Optional<CompanionApplication> findFirstByCompanionPostAndApplicant_IdAndStatusNot(CompanionPost post, Long applicantId, ApplicationStatus status);

    Optional<CompanionApplication> findByIdAndCompanionPost(Long id, CompanionPost post);

    List<CompanionApplication> findAllByCompanionPost(CompanionPost post);

    int countByCompanionPostAndStatus(CompanionPost post, ApplicationStatus status);

    Optional<CompanionApplication> findByCompanionPostAndApplicant(CompanionPost post, User applicant);
}
