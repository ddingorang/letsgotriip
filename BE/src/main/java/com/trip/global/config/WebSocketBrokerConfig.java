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

    /**
     * 허용 Origin 목록 — REST CORS 화이트리스트(WebConfig)와 동일 프로퍼티를 사용해 일치시킨다.
     * 미설정 시 개발 기본값(localhost/127.0.0.1:5173)으로 좁게 제한한다.
     */
    @Value("${app.frontend.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String[] allowedOrigins;

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

        // /ws : 브라우저 native WebSocket(SockJS 미사용) 클라이언트가 직접 붙는 raw STOMP 엔드포인트.
        //       Origin은 REST CORS 화이트리스트와 동일하게 좁게 제한한다(개발: localhost/127.0.0.1:5173).
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);

        // /ws-sockjs : SockJS 폴백이 필요한 클라이언트용 엔드포인트(병행 등록).
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
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
