package com.spring.outfit_rater.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.spring.outfit_rater.dto.ChatMessageDto;
import com.spring.outfit_rater.model.ChatMessage;
import com.spring.outfit_rater.service.ChatService;
import com.spring.outfit_rater.service.OutfitAiService;

import java.util.concurrent.CompletableFuture;

@Controller
@Slf4j
public class WebSocketController {

    private final ChatService chatService;
    private final OutfitAiService aiService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(ChatService chatService, OutfitAiService aiService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.aiService = aiService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessageDto sendMessage(ChatMessageDto message) {
        try {
            ChatMessage saved = chatService.saveMessage(message);
            
            if (message.getMessage().toLowerCase().contains("@ai")) {
                handleAiMention(message.getMessage());
            }
            
            return ChatMessageDto.fromEntity(saved);
        } catch (Exception e) {
            log.error("Error sending message", e);
            return createErrorMessage("Failed to send message");
        }
    }

    @MessageMapping("/chat.uploadOutfit")
    @SendTo("/topic/public")
    public ChatMessageDto uploadOutfit(ChatMessageDto message) {
        try {
            message.setMessageType(ChatMessage.MessageType.OUTFIT_UPLOAD);
            ChatMessage saved = chatService.saveMessage(message);
            
            handleOutfitRating(message.getImageUrl());
            
            return ChatMessageDto.fromEntity(saved);
        } catch (Exception e) {
            log.error("Error uploading outfit", e);
            return createErrorMessage("Failed to upload outfit");
        }
    }

    @MessageMapping("/chat.join")
    @SendTo("/topic/public")
    public ChatMessageDto userJoined(ChatMessageDto message) {
        return ChatMessageDto.builder()
                .userIp("AI")
                .message(aiService.getWelcomeMessage())
                .messageType(ChatMessage.MessageType.AI_RESPONSE)
                .build();
    }

    private void handleAiMention(String message) {
        CompletableFuture.runAsync(() -> {
            try {
                String response = aiService.handleChatMessage(message);
                sendAiMessage(response);
            } catch (Exception e) {
                log.error("AI chat error", e);
                sendAiMessage("🤖 Oops! My circuits are a bit tangled. Try again! ✨");
            }
        });
    }

    private void handleOutfitRating(String imageUrl) {
        CompletableFuture.runAsync(() -> {
            try {
                String rating = aiService.rateOutfit(imageUrl);
                sendAiMessage(rating);
            } catch (Exception e) {
                log.error("AI rating error", e);
                sendAiMessage("🔥 Looking good! My scanner needs a coffee break ☕ but I can tell you've got style! ✨");
            }
        });
    }

    private void sendAiMessage(String message) {
        ChatMessageDto aiMessage = ChatMessageDto.builder()
                .userIp("AI")
                .message(message)
                .messageType(ChatMessage.MessageType.AI_RESPONSE)
                .build();
        
        chatService.saveMessage(aiMessage);
        messagingTemplate.convertAndSend("/topic/public", aiMessage);
    }

    private ChatMessageDto createErrorMessage(String message) {
        return ChatMessageDto.builder()
                .userIp("System")
                .message(message)
                .messageType(ChatMessage.MessageType.SYSTEM_MESSAGE)
                .build();
    }
}