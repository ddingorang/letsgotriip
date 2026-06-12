package com.trip.chat.entity;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.trip.chat.entity.enums.MessageType;
import java.time.LocalDateTime;

@Document(collection = "chat_messages")
@Getter
@Builder
public class ChatMessage {

    @Id
    private String messageTSID;          // 메시지 고유 ID (TSID)
    private Long chatRoomId;             // 목적지(전달할 그룹 채팅방) ID
    private Long senderId;               // 발신인 ID
    private MessageType messageType;     // 메시지 타입(텍스트, 사진, 영수증)
    private String content;              // 메시지 내용
    private LocalDateTime timestamp;     // 메시지 전송 시각
}
