package com.trip.chat.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.trip.chat.entity.ChatRoom;
import com.trip.chat.entity.ChatRoomMembership;
import java.util.List;
import java.util.Optional;

public interface ChatRoomMembershipRepository extends JpaRepository<ChatRoomMembership, Long> {

    /** 시드 reset — 주어진 채팅방들의 멤버십 일괄 삭제 */
    @Modifying
    @Query("DELETE FROM ChatRoomMembership m WHERE m.chatRoom.id IN :chatRoomIds")
    int deleteByChatRoomIdIn(@Param("chatRoomIds") List<Long> chatRoomIds);

    Optional<ChatRoomMembership> findByUserIdAndChatRoom(Long userId, ChatRoom chatRoom);
    List<ChatRoomMembership> findByChatRoomId(Long chatRoomId);
    List<ChatRoomMembership> findByUserId(Long userId);

    /** 특정 방·사용자의 멤버십 단건(있으면) — 음소거/강퇴/재초대에서 사용 */
    Optional<ChatRoomMembership> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    /** 채팅방 전체 멤버십 수(탈퇴 포함) */
    int countByChatRoomId(Long chatRoomId);

    /** 채팅방 활성 멤버 수 — 탈퇴/강퇴 제외 */
    @Query("select count(m) from ChatRoomMembership m where m.chatRoom.id = :chatRoomId and m.leftAt is null and m.isBanned = false")
    int countActiveByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    /**
     * 특정 방·사용자의 멤버십 단건을 비관적 쓰기 락으로 조회.
     * 방장 위임 시 현 방장/신규 방장 행을 같은 트랜잭션 내 락으로 잡아
     * 동시 위임 레이스(방장 2명) 를 방지한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from ChatRoomMembership m where m.chatRoom.id = :chatRoomId and m.userId = :userId")
    Optional<ChatRoomMembership> findByChatRoomIdAndUserIdForUpdate(
            @Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}
