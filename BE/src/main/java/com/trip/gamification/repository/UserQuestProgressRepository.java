package com.trip.gamification.repository;

import com.trip.gamification.entity.UserQuestProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserQuestProgressRepository extends JpaRepository<UserQuestProgress, Long> {

    List<UserQuestProgress> findByUserId(Long userId);

    Optional<UserQuestProgress> findByUserIdAndQuestCode(Long userId, String questCode);

    /**
     * 진행도를 원자적으로 1 증가시킨다(lost update 방지). 이미 완료(progress>=goal)면 변화 없음(멱등).
     * goal 에 도달하면 completed/completedAt 을 함께 세팅한다. 반환값은 갱신된 행 수.
     * 동시 호출 시 단일 UPDATE 가 직렬화되어 마지막 커밋이 이전을 덮어쓰지 않는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserQuestProgress q
               set q.progress = q.progress + 1,
                   q.completed = (q.progress + 1 >= :goal),
                   q.completedAt = case when (q.progress + 1 >= :goal) then :now else q.completedAt end
             where q.userId = :userId
               and q.questCode = :questCode
               and q.progress < :goal
            """)
    int incrementProgress(@Param("userId") Long userId,
                          @Param("questCode") String questCode,
                          @Param("goal") int goal,
                          @Param("now") LocalDateTime now);
}
