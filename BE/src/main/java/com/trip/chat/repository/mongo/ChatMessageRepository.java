package com.trip.chat.repository.mongo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.trip.chat.entity.ChatMessage;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    // 채팅방 메시지 최신순 조회
    List<ChatMessage> findByChatRoomIdOrderByTimestampDesc(Long chatRoomId);

    /**
     * 채팅방 메시지를 최신순으로 상한(limit)만큼만 조회.
     * 전체 컬렉션 로드(메모리 폭증)를 막기 위해 Pageable(정렬+limit)을 Mongo 쿼리로 내려보낸다.
     * 히스토리 복원은 최근 N건만 필요하므로 충분하다.
     */
    List<ChatMessage> findByChatRoomIdOrderByTimestampDesc(Long chatRoomId, Pageable pageable);
}
