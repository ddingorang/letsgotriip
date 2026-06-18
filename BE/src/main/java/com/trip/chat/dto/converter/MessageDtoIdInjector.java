package com.trip.chat.dto.converter;

import com.github.f4b6a3.tsid.TsidCreator;

import com.trip.chat.dto.MessageResponseDto;
import com.trip.chat.dto.MessageSendRequestDto;
import java.time.LocalDateTime;

public class MessageDtoIdInjector {

    public static MessageResponseDto withGeneratedMessageId(final MessageSendRequestDto messageSendRequest, final Long senderId, final String senderNickname) {

        return MessageResponseDto.builder()
                .messageTSID(String.valueOf(TsidCreator.getTsid().toLong())) //TSID 기반 ID 생성기, 시간에 따라 증가하는 값을 가지며 최신 데이터일수록 더 큰 uniqute한 ID가 생성된다.
                .correlationId(String.valueOf(messageSendRequest.correlationId()))
                .chatRoomId(messageSendRequest.chatRoomId())
                .senderId(senderId)
                .senderNickname(senderNickname)
                .messageType(messageSendRequest.messageType())
                .content(messageSendRequest.content())
                .timestamp(LocalDateTime.now())
                .unreadCount(0)
                .build();
    }
}
