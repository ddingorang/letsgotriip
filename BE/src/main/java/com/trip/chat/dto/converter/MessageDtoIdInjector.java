package com.trip.chat.dto.converter;

import com.github.f4b6a3.tsid.TsidCreator;

import com.trip.chat.dto.MessageResponseDto;
import com.trip.chat.dto.MessageSendRequestDto;
import java.time.LocalDateTime;

public class MessageDtoIdInjector {

    public static MessageResponseDto withGeneratedMessageId(final MessageSendRequestDto messageSendRequest, final Long senderId, final String senderNickname, final Long chatRoomId) {

        return MessageResponseDto.builder()
                .messageTSID(String.valueOf(TsidCreator.getTsid().toLong())) //TSID 기반 ID 생성기, 시간에 따라 증가하는 값을 가지며 최신 데이터일수록 더 큰 uniqute한 ID가 생성된다.
                .correlationId(String.valueOf(messageSendRequest.correlationId()))
                // 권위 있는 chatRoomId(STOMP 경로 변수)를 사용한다.
                // 페이로드의 chatRoomId는 스푸핑 가능하므로 신뢰하지 않는다.
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .senderNickname(senderNickname)
                .messageType(messageSendRequest.messageType())
                .content(messageSendRequest.content())
                .timestamp(LocalDateTime.now())
                .unreadCount(0)
                .build();
    }
}
