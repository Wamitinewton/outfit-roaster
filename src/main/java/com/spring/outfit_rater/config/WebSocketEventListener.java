package com.spring.outfit_rater.config;

import com.spring.outfit_rater.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    private final AtomicInteger globalConnectionCount = new AtomicInteger(0);
    private final ConcurrentHashMap<String, String> sessionToUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> roomConnectionCounts = new ConcurrentHashMap<>();

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate, RoomService roomService) {
        this.messagingTemplate = messagingTemplate;
        this.roomService = roomService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        String sessionId = getSessionId(event.getMessage().getHeaders());
        log.info("New WebSocket connection established. Session ID: {}", sessionId);
        
        int currentConnections = globalConnectionCount.incrementAndGet();
        log.info("Total active connections: {}", currentConnections);
        
        broadcastGlobalConnectionCount(currentConnections);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("WebSocket connection closed. Session ID: {}", sessionId);
        
        String userId = sessionToUser.remove(sessionId);
        
        int currentConnections = globalConnectionCount.decrementAndGet();
        log.info("Total active connections: {}", currentConnections);
        
        broadcastGlobalConnectionCount(currentConnections);
        
        if (userId != null) {
            log.info("User {} disconnected", userId);
        }
    }

    @EventListener
    public void handleSubscriptionEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();
        
        if (destination != null && destination.startsWith("/topic/room/")) {
            String roomCode = extractRoomCodeFromDestination(destination);
            if (roomCode != null) {
                AtomicInteger roomCount = roomConnectionCounts.computeIfAbsent(roomCode, k -> new AtomicInteger(0));
                int currentRoomConnections = roomCount.incrementAndGet();
                
                log.info("User subscribed to room {} (Session: {}). Room connections: {}", 
                        roomCode, sessionId, currentRoomConnections);
                
                broadcastRoomConnectionCount(roomCode, currentRoomConnections);
            }
        }
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();
        
        if (destination != null && destination.startsWith("/topic/room/")) {
            String roomCode = extractRoomCodeFromDestination(destination);
            if (roomCode != null) {
                AtomicInteger roomCount = roomConnectionCounts.get(roomCode);
                if (roomCount != null) {
                    int currentRoomConnections = roomCount.decrementAndGet();
                    
                    log.info("User unsubscribed from room {} (Session: {}). Room connections: {}", 
                            roomCode, sessionId, currentRoomConnections);
                    
                    if (currentRoomConnections <= 0) {
                        roomConnectionCounts.remove(roomCode);
                    } else {
                        broadcastRoomConnectionCount(roomCode, currentRoomConnections);
                    }
                }
            }
        }
    }

    public void associateUserWithSession(String sessionId, String userId) {
        sessionToUser.put(sessionId, userId);
        log.debug("Associated user {} with session {}", userId, sessionId);
    }

    public int getGlobalConnectionCount() {
        return globalConnectionCount.get();
    }

    public int getRoomConnectionCount(String roomCode) {
        AtomicInteger count = roomConnectionCounts.get(roomCode.toUpperCase());
        return count != null ? count.get() : 0;
    }

    private void broadcastGlobalConnectionCount(int count) {
        try {
            String message = String.format("👥 %d fashion enthusiasts online", count);
            messagingTemplate.convertAndSend("/topic/connection-count", message);
        } catch (Exception e) {
            log.warn("Failed to broadcast global connection count", e);
        }
    }

    private void broadcastRoomConnectionCount(String roomCode, int count) {
        try {
            String message = String.format("👥 %d roasters in the room", count);
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/connection-count", message);
        } catch (Exception e) {
            log.warn("Failed to broadcast room connection count for room {}", roomCode, e);
        }
    }

    private String extractRoomCodeFromDestination(String destination) {
        if (destination.startsWith("/topic/room/")) {
            String[] parts = destination.split("/");
            if (parts.length >= 4) {
                return parts[3];
            }
        }
        return null;
    }

    private String getSessionId(Object headers) {
        if (headers instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> headerMap = (java.util.Map<String, Object>) headers;
            Object sessionId = headerMap.get("simpSessionId");
            return sessionId != null ? sessionId.toString() : "unknown";
        }
        return "unknown";
    }
}