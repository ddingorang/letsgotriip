package com.trip.gamification.repository;

import com.trip.gamification.entity.UserGameStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserGameStatRepository extends JpaRepository<UserGameStat, Long> {

    Optional<UserGameStat> findByUserId(Long userId);

    /**
     * 포인트/EXP 를 원자적으로 증가시키고 레벨(=exp/100+1)을 재계산한다.
     * 읽고-수정-쓰기(lost update) 경합을 피하기 위해 단일 UPDATE 로 처리한다.
     * 반환값은 갱신된 행 수(0 이면 해당 userId 행이 아직 없음).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserGameStat s
               set s.points = s.points + :points,
                   s.exp = s.exp + :exp,
                   s.level = (s.exp + :exp) / 100 + 1,
                   s.updatedAt = :now
             where s.userId = :userId
            """)
    int accrue(@Param("userId") Long userId,
               @Param("points") int points,
               @Param("exp") int exp,
               @Param("now") LocalDateTime now);
}
