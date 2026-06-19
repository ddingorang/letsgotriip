package com.trip.notification.sse;

import com.trip.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 연결 보관소 — userId 별로 다중 SseEmitter(여러 탭/기기)를 관리한다.
 * <p>
 * 동시성: userId→리스트 맵은 {@link ConcurrentHashMap}, 리스트는 {@link CopyOnWriteArrayList}로
 * 락 없이 읽기/순회 안전. emitter 콜백(완료/타임아웃/에러)에서 자기 자신을 제거한다.
 */
@Slf4j
@Component
public class SseEmitterRegistry {

    /** SSE 타임아웃(ms) — 길게 잡고 끊기면 클라이언트가 재연결. */
    private static final long TIMEOUT_MS = 60L * 60L * 1000L; // 1시간

    /** 사용자당 동시 SSE 연결 상한 — 초과 시 가장 오래된 연결을 닫고 신규를 수용한다. */
    private static final int MAX_CONNECTIONS_PER_USER = 5;

    /** heartbeat 1회 순회당 처리할 연결 총량 상한 — 대량 연결 시 스케줄러 점유를 제한한다. */
    private static final int HEARTBEAT_MAX_EMITTERS_PER_TICK = 5_000;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 사용자 SSE 구독 등록. 연결 직후 초기 이벤트를 1건 보내 프록시 버퍼링/타임아웃을 방지한다.
     * 사용자당 동시 연결이 상한을 넘으면 가장 오래된 연결을 complete 후 신규를 추가한다(무제한 누적 방지).
     */
    public SseEmitter add(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        CopyOnWriteArrayList<SseEmitter> list =
                emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

        // 상한 초과분(가장 오래된 연결)을 먼저 정리. 신규 1건 자리 확보까지 반복.
        while (list.size() >= MAX_CONNECTIONS_PER_USER && !list.isEmpty()) {
            SseEmitter oldest = list.get(0);
            // complete()가 onCompletion 콜백을 통해 remove 를 유발하지만,
            // 콜백 누락/경쟁에 대비해 직접 제거도 보장한다.
            try {
                oldest.complete();
            } catch (Exception ignored) {
                // 이미 닫힌 연결 — 무시
            }
            remove(userId, oldest);
        }

        // remove()로 리스트가 비어 맵에서 분리됐을 수 있으므로 add 직전에 현재 맵의 리스트를 재확보한다.
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(userId, emitter);
        });
        emitter.onError(e -> remove(userId, emitter));

        // 연결 확인용 초기 이벤트(스트림 오픈을 클라이언트에 알림)
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        return emitter;
    }

    /**
     * 해당 수신자의 모든 활성 연결에 알림 전송. 전송 실패한 emitter는 정리한다.
     */
    public void push(Long userId, NotificationResponse payload) {
        if (userId == null) return;
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) return;

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
            } catch (Exception e) {
                // 끊긴 연결 — 콜백이 안 탔을 수 있으니 직접 정리
                remove(userId, emitter);
            }
        }
    }

    /**
     * keep-alive 주석(comment) 전송 — 유휴 연결이 프록시에서 끊기지 않도록 30초 주기로 보낸다.
     * 1회 순회 비용을 {@link #HEARTBEAT_MAX_EMITTERS_PER_TICK}로 제한해 대량 연결 시 스케줄러 점유를 막는다.
     */
    @Scheduled(fixedRate = 30_000L)
    public void heartbeat() {
        int budget = HEARTBEAT_MAX_EMITTERS_PER_TICK;
        for (Map.Entry<Long, CopyOnWriteArrayList<SseEmitter>> entry : emitters.entrySet()) {
            Long userId = entry.getKey();
            for (SseEmitter emitter : entry.getValue()) {
                if (budget-- <= 0) {
                    return; // 이번 tick 예산 소진 — 나머지는 다음 주기에 처리
                }
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (Exception e) {
                    remove(userId, emitter);
                }
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
        if (list == null) return;
        list.remove(emitter);
        // 빈 리스트는 맵에서 제거(메모리 누수 방지). 동시성 안전하게 비었을 때만.
        emitters.computeIfPresent(userId, (k, v) -> v.isEmpty() ? null : v);
    }
}
