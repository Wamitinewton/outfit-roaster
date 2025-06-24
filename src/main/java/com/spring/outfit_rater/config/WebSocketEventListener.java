package com.spring.outfit_rater.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private final ConcurrentHashMap<String, String> userSessions = new ConcurrentHashMap<>();

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId").toString();
        log.info("New WebSocket connection established. Session ID: {}", sessionId);
        
        int currentConnections = connectionCount.incrementAndGet();
        log.info("Total active connections: {}", currentConnections);
        
        broadcastConnectionCount(currentConnections);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("WebSocket connection closed. Session ID: {}", sessionId);
        
        String userIp = userSessions.remove(sessionId);
        
        int currentConnections = connectionCount.decrementAndGet();
        log.info("Total active connections: {}", currentConnections);
        
        broadcastConnectionCount(currentConnections);
        
        if (userIp != null) {
            log.info("User {} disconnected", userIp);
        }
    }

    private void broadcastConnectionCount(int count) {
        try {
            messagingTemplate.convertAndSend("/topic/connection-count", 
                String.format("👥 %d fashion enthusiasts online", count));
        } catch (Exception e) {
            log.warn("Failed to broadcast connection count", e);
        }
    }

    public void associateUserWithSession(String sessionId, String userIp) {
        userSessions.put(sessionId, userIp);
    }

    public int getConnectionCount() {
        return connectionCount.get();
    }
}
