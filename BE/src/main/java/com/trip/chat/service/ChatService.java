package com.trip.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.trip.chat.dto.MessageResponseDto;
import com.trip.chat.dto.MessageSendRequestDto;
import com.trip.chat.dto.converter.MessageDtoConverter;
import com.trip.chat.entity.ChatMessage;
import com.trip.chat.repository.mongo.ChatMessageRepository;
import com.trip.user.entity.User;
import com.trip.user.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.trip.chat.dto.converter.MessageDtoIdInjector.withGeneratedMessageId;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "chat.enabled", havingValue = "true", matchIfMissing = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate; // STOMP 브로커(/topic) 목적지로 메시지 브로드캐스트
    private final UserRepository userRepository;

    public void sendMessage(final MessageSendRequestDto messageSendRequest, final Long senderId) {

        log.info("[1/3] 메시지 전송 프로세스 시작. senderId: {}, chatRoomId: {}", senderId, messageSendRequest.chatRoomId());

        String senderNickname = userRepository.findById(senderId)
                .map(u -> u.getNickname())
                .orElse("알 수 없음");

        // 클라이언트 요청 DTO에 서버 생성 값(TSID, senderId, timestamp 등) 주입
        final MessageResponseDto messageDtoWithId = withGeneratedMessageId(messageSendRequest, senderId, senderNickname);
        log.info("[2/3] 메시지 ID 생성 완료. messageTSID: {}", messageDtoWithId.messageTSID());

        // MongoDB에 채팅 메시지 영구 저장
        final ChatMessage chatMessage = MessageDtoConverter.toMessage(messageDtoWithId);
        chatMessageRepository.save(chatMessage);
        log.info("[3/3] MongoDB 저장 완료. messageTSID: {}", chatMessage.getMessageTSID());

        // STOMP 브로커(/topic) 목적지로 명시 발행 → 구독 중인 클라이언트에게 브로드캐스트.
        // (이전의 rabbitTemplate.convertAndSend(payload) 단일 인자 호출은 exchange/routing key가
        //  지정되지 않아 실제 라우팅되지 않는 dead code였으므로 제거)
        final String destination = "/topic/chat.room." + messageSendRequest.chatRoomId();
        messagingTemplate.convertAndSend(destination, messageDtoWithId);
        log.info("메시지 전송 완료. destination: {}, messageTSID: {}", destination, chatMessage.getMessageTSID());
    }

    /**
     * 채팅방 메시지 히스토리 조회 (오래된 → 최신 순).
     * 새로고침/재입장 시 이전 대화를 복원하기 위한 REST 경로.
     */
    public List<MessageResponseDto> getHistory(final Long chatRoomId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByTimestampDesc(chatRoomId);

        // 발신자 닉네임 일괄 조회(메시지마다 쿼리하지 않도록 캐싱)
        Map<Long, String> nicknameCache = new HashMap<>();

        // timestamp DESC로 조회되므로 화면 표시용으로 ASC(오래된→최신)로 뒤집는다.
        return messages.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .map(m -> MessageResponseDto.builder()
                        .messageTSID(m.getMessageTSID())
                        .correlationId(null)
                        .chatRoomId(m.getChatRoomId())
                        .senderId(m.getSenderId())
                        .senderNickname(nicknameCache.computeIfAbsent(m.getSenderId(), id ->
                                userRepository.findById(id).map(User::getNickname).orElse("알 수 없음")))
                        .messageType(m.getMessageType())
                        .content(m.getContent())
                        .timestamp(m.getTimestamp())
                        .unreadCount(0)
                        .build())
                .toList();
    }
}
