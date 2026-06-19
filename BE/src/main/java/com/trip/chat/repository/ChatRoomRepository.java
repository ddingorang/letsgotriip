package com.trip.chat.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.trip.chat.entity.ChatRoom;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 정원 체크~멤버십 등록을 원자적으로 처리하기 위한 비관적 쓰기 락 조회.
     * (companion 도메인의 approveApplication 직렬화 패턴과 동일)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ChatRoom r where r.id = :id")
    Optional<ChatRoom> findByIdForUpdate(@Param("id") Long id);
}
