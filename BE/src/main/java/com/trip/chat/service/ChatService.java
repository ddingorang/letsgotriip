package com.trip.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trip.chat.dto.ChatRoomParticipantsResponse;
import com.trip.chat.dto.MessageResponseDto;
import com.trip.chat.dto.MessageSendRequestDto;
import com.trip.chat.dto.converter.MessageDtoConverter;
import com.trip.chat.entity.ChatMessage;
import com.trip.chat.entity.ChatRoomMembership;
import com.trip.chat.repository.ChatRoomMembershipRepository;
import com.trip.chat.repository.mongo.ChatMessageRepository;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.user.entity.User;
import com.trip.user.repository.UserRepository;

import java.time.LocalDateTime;
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
    private final ChatRoomMembershipRepository chatRoomMembershipRepository;

    public void sendMessage(final MessageSendRequestDto messageSendRequest, final Long senderId, final Long chatRoomId) {

        log.info("[1/3] 메시지 전송 프로세스 시작. senderId: {}, chatRoomId: {}", senderId, chatRoomId);

        // STOMP 송신 인가 — 발신자가 해당 방의 활성 멤버(미탈퇴·미강퇴)인지 검증한다.
        // 페이로드의 chatRoomId는 스푸핑 가능하므로 신뢰하지 않고, STOMP 경로(@DestinationVariable)
        // 에서 전달된 chatRoomId만 권위 있는 값으로 사용한다(영속/브로드캐스트 전에 검증).
        boolean isActiveMember = chatRoomMembershipRepository.findByChatRoomId(chatRoomId).stream()
                .filter(m -> m.getUserId().equals(senderId))
                .anyMatch(ChatRoomMembership::isActiveMember);
        if (!isActiveMember) {
            log.warn("STOMP 송신 인가 거부 — 비멤버 발신 시도. senderId: {}, chatRoomId: {}", senderId, chatRoomId);
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }

        String senderNickname = userRepository.findById(senderId)
                .map(u -> u.getNickname())
                .orElse("알 수 없음");

        // 클라이언트 요청 DTO에 서버 생성 값(TSID, senderId, timestamp 등) 주입.
        // 권위 있는 chatRoomId(경로 변수)를 사용해 페이로드 roomId 스푸핑을 무력화한다.
        final MessageResponseDto messageDtoWithId =
                withGeneratedMessageId(messageSendRequest, senderId, senderNickname, chatRoomId);
        log.info("[2/3] 메시지 ID 생성 완료. messageTSID: {}", messageDtoWithId.messageTSID());

        // MongoDB에 채팅 메시지 영구 저장
        final ChatMessage chatMessage = MessageDtoConverter.toMessage(messageDtoWithId);
        chatMessageRepository.save(chatMessage);
        log.info("[3/3] MongoDB 저장 완료. messageTSID: {}", chatMessage.getMessageTSID());

        // STOMP 브로커(/topic) 목적지로 명시 발행 → 구독 중인 클라이언트에게 브로드캐스트.
        // (이전의 rabbitTemplate.convertAndSend(payload) 단일 인자 호출은 exchange/routing key가
        //  지정되지 않아 실제 라우팅되지 않는 dead code였으므로 제거)
        final String destination = "/topic/chat.room." + chatRoomId;
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

    /**
     * 채팅방 나가기 — 본인 멤버십의 leftAt 을 현재 시각으로 설정(소프트 탈퇴).
     * - 멤버가 아니면 403
     * - 방장(host)은 나갈 수 없음 → 400(_BAD_REQUEST)
     * - 이미 나간 상태면 멱등 처리(그대로 통과)
     */
    @Transactional
    public void leaveRoom(final Long chatRoomId, final Long userId) {
        ChatRoomMembership membership = chatRoomMembershipRepository.findByChatRoomId(chatRoomId).stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));

        if (Boolean.TRUE.equals(membership.getIsHost())) {
            // 방장은 채팅방을 나갈 수 없다(동행 게시글 삭제/취소로만 정리).
            throw new GeneralException(ResponseCode._BAD_REQUEST, "방장은 채팅방을 나갈 수 없습니다.");
        }

        if (membership.getLeftAt() == null) {
            membership.leave(LocalDateTime.now());
        }
        log.info("채팅방 나가기 완료. chatRoomId: {}, userId: {}", chatRoomId, userId);
    }

    /**
     * 채팅방 참여자(활성 멤버) 목록/인원수 조회.
     * 본인이 활성 멤버여야 조회 가능.
     */
    @Transactional(readOnly = true)
    public ChatRoomParticipantsResponse getParticipants(final Long chatRoomId, final Long userId) {
        List<ChatRoomMembership> memberships = chatRoomMembershipRepository.findByChatRoomId(chatRoomId);

        boolean isActiveMember = memberships.stream()
                .anyMatch(m -> m.getUserId().equals(userId) && m.isActiveMember());
        if (!isActiveMember) {
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }

        List<ChatRoomParticipantsResponse.Participant> participants = memberships.stream()
                .filter(ChatRoomMembership::isActiveMember)
                .map(m -> ChatRoomParticipantsResponse.Participant.builder()
                        .userId(m.getUserId())
                        .nickname(userRepository.findById(m.getUserId())
                                .map(User::getNickname).orElse("알 수 없음"))
                        .isHost(Boolean.TRUE.equals(m.getIsHost()))
                        .build())
                .toList();

        return ChatRoomParticipantsResponse.builder()
                .chatRoomId(chatRoomId)
                .count(participants.size())
                .participants(participants)
                .build();
    }
}
