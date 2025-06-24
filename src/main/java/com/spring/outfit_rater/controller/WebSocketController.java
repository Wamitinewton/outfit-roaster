package com.spring.outfit_rater.controller;

import com.spring.outfit_rater.dto.ChatMessageDto;
import com.spring.outfit_rater.model.ChatMessage;
import com.spring.outfit_rater.service.AiService;
import com.spring.outfit_rater.service.ChatService;
import lombok.extern.slf4j.Slf4j;
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
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(ChatService chatService, AiService aiService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.aiService = aiService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.message")
    @SendTo("/topic/public")
    public ChatMessageDto sendMessage(ChatMessageDto message) {
        try {
            message.setType(ChatMessage.MessageType.USER);
            ChatMessage saved = chatService.saveMessage(message);
            
            if (message.getContent().toLowerCase().contains("@ai") || 
                message.getContent().toLowerCase().contains("@styleai")) {
                handleAiResponse(message.getContent(), message.getUserId());
            }
            
            return ChatMessageDto.fromEntity(saved);
        } catch (Exception e) {
            log.error("Error sending message", e);
            return createErrorMessage("Failed to send message");
        }
    }

    @MessageMapping("/chat.outfit")
    @SendTo("/topic/public")
    public ChatMessageDto shareOutfit(ChatMessageDto message) {
        try {
            message.setType(ChatMessage.MessageType.OUTFIT);
            ChatMessage saved = chatService.saveMessage(message);
            
            handleOutfitAnalysis(message.getImageUrl(), message.getUserId());
            
            return ChatMessageDto.fromEntity(saved);
        } catch (Exception e) {
            log.error("Error sharing outfit", e);
            return createErrorMessage("Failed to share outfit");
        }
    }

    @MessageMapping("/chat.join")
    @SendTo("/topic/public")
    public ChatMessageDto userJoined(ChatMessageDto message) {
        return ChatMessageDto.builder()
                .userId("StyleAI")
                .content(aiService.getWelcomeMessage())
                .type(ChatMessage.MessageType.AI)
                .build();
    }

    private void handleAiResponse(String userMessage, String userId) {
        CompletableFuture.runAsync(() -> {
            try {
                String response = aiService.handleChatMessage(userMessage, userId);
                sendAiMessage(response, userId);
            } catch (Exception e) {
                log.error("AI chat error for user: {}", userId, e);
                sendAiMessage("I'm having a quick wardrobe malfunction! Could you try that again? 💫", userId);
            }
        });
    }

    private void handleOutfitAnalysis(String imageUrl, String userId) {
        CompletableFuture.runAsync(() -> {
            try {
                String analysis = aiService.analyzeOutfit(imageUrl, userId);
                sendAiMessage(analysis, userId);
            } catch (Exception e) {
                log.error("AI analysis error for user: {}", userId, e);
                sendAiMessage("Your style is looking great! My fashion scanner needs a quick refresh. ✨", userId);
            }
        });
    }

    private void sendAiMessage(String content, String contextUserId) {
        ChatMessageDto aiMessage = ChatMessageDto.builder()
                .userId("StyleAI")
                .content(content)
                .type(ChatMessage.MessageType.AI)
                .build();
        
        aiMessage.setUserId(contextUserId);
        chatService.saveMessage(aiMessage);
        
        aiMessage.setUserId("StyleAI");
        messagingTemplate.convertAndSend("/topic/public", aiMessage);
    }

    private ChatMessageDto createErrorMessage(String content) {
        return ChatMessageDto.builder()
                .userId("System")
                .content(content)
                .type(ChatMessage.MessageType.USER)
                .build();
    }
}