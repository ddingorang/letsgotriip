package com.trip.chat.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.trip.chat.entity.ChatMessage;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    // 채팅방 메시지 최신순 조회
    List<ChatMessage> findByChatRoomIdOrderByTimestampDesc(Long chatRoomId);
}
