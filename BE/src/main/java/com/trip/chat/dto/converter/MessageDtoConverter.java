package com.trip.chat.dto.converter;

import com.trip.chat.dto.MessageResponseDto;
import com.trip.chat.entity.ChatMessage;

public class MessageDtoConverter {

    public static ChatMessage toMessage(final MessageResponseDto messageDto) {

        return ChatMessage.builder()
                .messageTSID(messageDto.messageTSID())
                .chatRoomId(messageDto.chatRoomId())
                .senderId(messageDto.senderId())
                .messageType(messageDto.messageType())
                .content(messageDto.content())
                .timestamp(messageDto.timestamp())
                .build();
    }
}
