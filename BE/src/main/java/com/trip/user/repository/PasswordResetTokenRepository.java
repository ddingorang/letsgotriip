package com.trip.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trip.user.entity.PasswordResetToken;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);
}
