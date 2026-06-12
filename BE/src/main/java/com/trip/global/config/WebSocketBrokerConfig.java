package com.trip.global.config;

import com.trip.global.interceptor.StompSessionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@ConditionalOnProperty(name = "chat.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

    private final StompSessionInterceptor stompSessionInterceptor;

    @Value("${spring.rabbitmq.host}")
    private String rabbitmqHost; // RabbitMQ 서버 주소

    @Value("${spring.rabbitmq.stomp.port}")
    private int rabbitmqStompPort; // RabbitMQ STOMP 포트 (AMQP 5672와 다름, 기본값 61613)

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername; // RabbitMQ 계정

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword; // RabbitMQ 비밀번호

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompSessionInterceptor);
    }

    /**
     * STOMP 엔드포인트 등록 메서드.
     * 클라이언트가 특정 엔드포인트로 연결할 수 있도록 STOMP 엔드포인트를 정의.
     *
     * @param registry StompEndpointRegistry 객체로 엔드포인트 설정을 수행
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // 웹 소켓 통신이 /ws으로 도착할 때, 해당 통신이 웹 소켓 통신 중 stomp 통신인 것을 확인하고, 이를 연결.
        // 클라이언트가 웹소켓 연결을 시도할 기본 엔드포인트 설정
        // /ws 경로로 연결을 수락하며, 모든 오리진(*: CORS 허용)을 허용
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*"); // FIXME: CORS 정책 허용 (필요 시 보안 강화를 위해 특정 Origin 설정 가능)
    }

    /**
     * 메시지 브로커 설정 메서드.
     * STOMP에서 사용되는 메시지 브로커와 라우팅 관련 설정을 정의.
     *
     * @param registry MessageBrokerRegistry 객체로 브로커 설정을 수행
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        registry.setPathMatcher(new AntPathMatcher(".")); // URL을 / -> .으로
        registry.setApplicationDestinationPrefixes("/pub");  //  @MessageMapping 메서드로 라우팅된다.  Client에서 SEND 요청을 처리

        // TODO: SSL이 필요하면 TCP 연결 설정이 필요할 수도 있다.

        // StompBrokerRelay 사용
        registry.enableStompBrokerRelay("/topic")
                .setRelayHost(rabbitmqHost)
                .setRelayPort(rabbitmqStompPort) // RabbitMQ STOMP 전용 포트 (AMQP 5672랑 다름)
                .setClientLogin(rabbitmqUsername)
                .setClientPasscode(rabbitmqPassword)
                .setSystemLogin(rabbitmqUsername)
                .setSystemPasscode(rabbitmqPassword);
    }
}
