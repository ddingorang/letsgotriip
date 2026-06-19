package com.trip.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trip.chat.entity.ChatRoom;
import com.trip.chat.entity.ChatRoomMembership;
import java.util.List;
import java.util.Optional;

public interface ChatRoomMembershipRepository extends JpaRepository<ChatRoomMembership, Long> {

    Optional<ChatRoomMembership> findByUserIdAndChatRoom(Long userId, ChatRoom chatRoom);
    List<ChatRoomMembership> findByChatRoomId(Long chatRoomId);
    List<ChatRoomMembership> findByUserId(Long userId);

    /** 채팅방 인원수 — N+1 회피용 카운트 쿼리 */
    int countByChatRoomId(Long chatRoomId);
}
