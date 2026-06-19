package com.trip.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.trip.user.entity.PasswordResetToken;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /**
     * 토큰을 single-use 로 원자적으로 소비한다.
     * 미사용(used=false)이고 미만료(expiresAt> now)인 경우에만 used=true 로 갱신한다.
     * 동시 요청 레이스에서도 정확히 1건만 affected=1 을 받는다(나머지는 0).
     *
     * @return 갱신된 행 수 (성공적으로 소비했으면 1, 그 외 0)
     */
    @Modifying
    @Query("update PasswordResetToken t set t.used = true "
            + "where t.token = :token and t.used = false and t.expiresAt > :now")
    int consumeToken(@Param("token") String token, @Param("now") LocalDateTime now);
}
