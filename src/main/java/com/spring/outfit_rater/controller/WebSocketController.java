package com.spring.outfit_rater.controller;

import com.spring.outfit_rater.dto.ChatMessageDto;
import com.spring.outfit_rater.model.ChatMessage;
import com.spring.outfit_rater.service.AiService;
import com.spring.outfit_rater.service.ChatService;
import com.spring.outfit_rater.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.concurrent.CompletableFuture;

@Controller
@Slf4j
public class WebSocketController {

    private final ChatService chatService;
    private final AiService aiService;
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(ChatService chatService, AiService aiService, 
                             RoomService roomService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.aiService = aiService;
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.message")
    public void sendGlobalMessage(ChatMessageDto message) {
        try {
            log.debug("Global message from user: {}", message.getUserId());
            
            message.setType(ChatMessage.MessageType.USER);
            message.setRoomCode(null);
            ChatMessage saved = chatService.saveMessage(message);
            
            ChatMessageDto response = ChatMessageDto.fromEntity(saved);
            messagingTemplate.convertAndSend("/topic/public", response);
            
            if (message.getContent().toLowerCase().contains("@ai") || 
                message.getContent().toLowerCase().contains("@styleai")) {
                handleAiResponse(message.getContent(), message.getUserId(), null);
            }
            
        } catch (Exception e) {
            log.error("Error sending global message", e);
            sendErrorMessage("/topic/public", "Failed to send message");
        }
    }

    @MessageMapping("/room/{roomCode}/message")
    public void sendRoomMessage(@DestinationVariable String roomCode, ChatMessageDto message) {
        try {
            String upperRoomCode = roomCode.toUpperCase();
            log.debug("Room message from user: {} in room: {}", message.getUserId(), upperRoomCode);
            
            if (!roomService.isUserInRoom(upperRoomCode, message.getUserId())) {
                log.warn("User {} tried to send message to room {} without being a member", 
                        message.getUserId(), upperRoomCode);
                return;
            }
            
            message.setType(ChatMessage.MessageType.USER);
            message.setRoomCode(upperRoomCode);
            ChatMessage saved = chatService.saveMessage(message);
            
            roomService.updateParticipantActivity(upperRoomCode, message.getUserId());
            
            ChatMessageDto response = ChatMessageDto.fromEntity(saved);
            messagingTemplate.convertAndSend("/topic/room/" + upperRoomCode, response);
            
            if (message.getContent().toLowerCase().contains("@ai") || 
                message.getContent().toLowerCase().contains("@styleai")) {
                handleAiResponse(message.getContent(), message.getUserId(), upperRoomCode);
            }
            
        } catch (Exception e) {
            log.error("Error sending room message", e);
            sendErrorMessage("/topic/room/" + roomCode.toUpperCase(), "Failed to send message");
        }
    }

    @MessageMapping("/chat.outfit")
    public void shareGlobalOutfit(ChatMessageDto message) {
        try {
            log.debug("Global outfit shared by user: {}", message.getUserId());
            
            message.setType(ChatMessage.MessageType.OUTFIT);
            message.setRoomCode(null); 
            ChatMessage saved = chatService.saveMessage(message);
            
            ChatMessageDto response = ChatMessageDto.fromEntity(saved);
            messagingTemplate.convertAndSend("/topic/public", response);
            
            handleOutfitAnalysis(message.getImageUrl(), message.getUserId(), null);
            
        } catch (Exception e) {
            log.error("Error sharing global outfit", e);
            sendErrorMessage("/topic/public", "Failed to share outfit");
        }
    }

    @MessageMapping("/room/{roomCode}/outfit")
    public void shareRoomOutfit(@DestinationVariable String roomCode, ChatMessageDto message) {
        try {
            String upperRoomCode = roomCode.toUpperCase();
            log.debug("Room outfit shared by user: {} in room: {}", message.getUserId(), upperRoomCode);
            
            if (!roomService.isUserInRoom(upperRoomCode, message.getUserId())) {
                log.warn("User {} tried to share outfit in room {} without being a member", 
                        message.getUserId(), upperRoomCode);
                return;
            }
            
            message.setType(ChatMessage.MessageType.OUTFIT);
            message.setRoomCode(upperRoomCode);
            ChatMessage saved = chatService.saveMessage(message);
            
            roomService.updateParticipantActivity(upperRoomCode, message.getUserId());
            
            ChatMessageDto response = ChatMessageDto.fromEntity(saved);
            messagingTemplate.convertAndSend("/topic/room/" + upperRoomCode, response);
            
            handleOutfitAnalysis(message.getImageUrl(), message.getUserId(), upperRoomCode);
            
        } catch (Exception e) {
            log.error("Error sharing room outfit", e);
            sendErrorMessage("/topic/room/" + roomCode.toUpperCase(), "Failed to share outfit");
        }
    }

    @MessageMapping("/room/{roomCode}/join")
    public void userJoinedRoom(@DestinationVariable String roomCode, ChatMessageDto message) {
        try {
            String upperRoomCode = roomCode.toUpperCase();
            log.debug("User {} joined room: {}", message.getUserId(), upperRoomCode);
            
            roomService.updateParticipantActivity(upperRoomCode, message.getUserId());
            
            ChatMessageDto joinMessage = ChatMessageDto.builder()
                    .userId("System")
                    .content(String.format("👋 %s joined the roast session!", 
                            chatService.generateDisplayName(message.getUserId())))
                    .type(ChatMessage.MessageType.SYSTEM)
                    .roomCode(upperRoomCode)
                    .build();
            
            messagingTemplate.convertAndSend("/topic/room/" + upperRoomCode, joinMessage);
            
        } catch (Exception e) {
            log.error("Error handling room join", e);
        }
    }

    @MessageMapping("/chat.join")
    public void userJoinedGlobal(ChatMessageDto message) {
        log.debug("User {} connected to global chat", message.getUserId());
    }

    private void handleAiResponse(String userMessage, String userId, String roomCode) {
        CompletableFuture.runAsync(() -> {
            try {
                String response = aiService.handleChatMessage(userMessage, userId);
                sendAiMessage(response, userId, roomCode);
            } catch (Exception e) {
                log.error("AI chat error for user: {} in room: {}", userId, roomCode, e);
                sendAiMessage("I'm having a quick wardrobe malfunction! Could you try that again? 💫", userId, roomCode);
            }
        });
    }

    private void handleOutfitAnalysis(String imageUrl, String userId, String roomCode) {
        CompletableFuture.runAsync(() -> {
            try {
                String analysis = aiService.analyzeOutfit(imageUrl, userId);
                sendAiMessage(analysis, userId, roomCode);
            } catch (Exception e) {
                log.error("AI analysis error for user: {} in room: {}", userId, roomCode, e);
                sendAiMessage("Your style is looking great! My fashion scanner needs a quick refresh. ✨", userId, roomCode);
            }
        });
    }

    private void sendAiMessage(String content, String contextUserId, String roomCode) {
        ChatMessageDto aiMessage = ChatMessageDto.builder()
                .userId("StyleAI")
                .content(content)
                .type(ChatMessage.MessageType.AI)
                .roomCode(roomCode)
                .build();
        
        aiMessage.setUserId(contextUserId);
        chatService.saveMessage(aiMessage);
        
        aiMessage.setUserId("StyleAI");
        
        if (roomCode != null && !roomCode.trim().isEmpty()) {
            messagingTemplate.convertAndSend("/topic/room/" + roomCode.toUpperCase(), aiMessage);
        } else {
            messagingTemplate.convertAndSend("/topic/public", aiMessage);
        }
    }

    private void sendErrorMessage(String destination, String content) {
        try {
            ChatMessageDto errorMessage = ChatMessageDto.builder()
                    .userId("System")
                    .content(content)
                    .type(ChatMessage.MessageType.SYSTEM)
                    .build();
            
            messagingTemplate.convertAndSend(destination, errorMessage);
        } catch (Exception e) {
            log.error("Failed to send error message to {}", destination, e);
        }
    }
}