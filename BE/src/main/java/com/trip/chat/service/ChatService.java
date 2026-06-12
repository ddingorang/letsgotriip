package com.trip.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.trip.chat.dto.MessageResponseDto;
import com.trip.chat.dto.MessageSendRequestDto;
import com.trip.chat.dto.converter.MessageDtoConverter;
import com.trip.chat.entity.ChatMessage;
import com.trip.chat.repository.mongo.ChatMessageRepository;

import static com.trip.chat.dto.converter.MessageDtoIdInjector.withGeneratedMessageId;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "chat.enabled", havingValue = "true", matchIfMissing = true)
public class ChatService {

    private final RabbitTemplate rabbitTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate; // TODO: 롤백 대상 - RabbitMQ Consumer 방식으로 전환 시 제거

    public void sendMessage(final MessageSendRequestDto messageSendRequest, final Long senderId) {

        log.info("[1/3] 메시지 전송 프로세스 시작. senderId: {}, chatRoomId: {}", senderId, messageSendRequest.chatRoomId());

        // 클라이언트 요청 DTO에 서버 생성 값(TSID, senderId, timestamp 등) 주입
        final MessageResponseDto messageDtoWithId = withGeneratedMessageId(messageSendRequest, senderId);
        log.info("[2/3] 메시지 ID 생성 완료. messageTSID: {}", messageDtoWithId.messageTSID());

        // MongoDB에 채팅 메시지 영구 저장
        final ChatMessage chatMessage = MessageDtoConverter.toMessage(messageDtoWithId);
        chatMessageRepository.save(chatMessage);
        log.info("[3/3] MongoDB 저장 완료. messageTSID: {}", chatMessage.getMessageTSID());

        // RabbitMQ Exchange로 메시지 발행 → 구독 중인 클라이언트에게 브로드캐스트
        rabbitTemplate.convertAndSend(chatMessage);

        // TODO: 롤백 대상 - SimpMessagingTemplate으로 직접 브로드캐스트 (RabbitMQ Consumer 방식으로 전환 시 제거)
        messagingTemplate.convertAndSend("/topic/chat.room." + messageSendRequest.chatRoomId(), messageDtoWithId);
        log.info("메시지 전송 완료. chatRoomId: {}, messageTSID: {}", messageSendRequest.chatRoomId(), chatMessage.getMessageTSID());
    }
}
