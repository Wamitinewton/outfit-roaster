package com.spring.outfit_rater.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.spring.outfit_rater.dto.ChatMessageDto;
import com.spring.outfit_rater.model.ChatMessage;
import com.spring.outfit_rater.service.ChatRoomService;
import com.spring.outfit_rater.service.OutfitAiService;

@Controller
@Slf4j
@CrossOrigin(origins = "*")
public class ChatWebSocketController {

    private final ChatRoomService chatRoomService;
    private final OutfitAiService outfitAiService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatRoomService chatRoomService, 
                                 OutfitAiService outfitAiService,
                                 SimpMessagingTemplate messagingTemplate) {
        this.chatRoomService = chatRoomService;
        this.outfitAiService = outfitAiService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessageDto sendMessage(ChatMessageDto chatMessage) {
        log.info("Received message from {}: {}", chatMessage.getUserIp(), chatMessage.getMessage());
        
        ChatMessage savedMessage = chatRoomService.saveMessage(chatMessage);
        ChatMessageDto response = ChatMessageDto.fromEntity(savedMessage);
        
        if (chatMessage.getMessage().toLowerCase().contains("@ai")) {
            handleAiTag(chatMessage);
        }
        
        return response;
    }

    @MessageMapping("/chat.uploadOutfit")
    @SendTo("/topic/public")
    public ChatMessageDto uploadOutfit(ChatMessageDto outfitMessage) {
        log.info("Received outfit upload from {}: {}", outfitMessage.getUserIp(), outfitMessage.getImageUrl());
        
        outfitMessage.setMessageType(ChatMessage.MessageType.OUTFIT_UPLOAD);
        outfitMessage.setMessage("📸 Uploaded outfit for rating!");
        ChatMessage savedMessage = chatRoomService.saveMessage(outfitMessage);
        
        handleOutfitRating(outfitMessage);
        
        return ChatMessageDto.fromEntity(savedMessage);
    }

    @MessageMapping("/chat.join")
    @SendTo("/topic/public")
    public ChatMessageDto addUser(ChatMessageDto chatMessage) {
        log.info("User joined: {}", chatMessage.getUserIp());
        
        ChatMessageDto welcomeMessage = ChatMessageDto.builder()
                .userIp("AI")
                .message(outfitAiService.getWelcomeMessage())
                .messageType(ChatMessage.MessageType.AI_RESPONSE)
                .build();
        
        chatRoomService.saveMessage(welcomeMessage);
        
        return welcomeMessage;
    }

    private void handleAiTag(ChatMessageDto chatMessage) {
        new Thread(() -> {
            try {
                String aiResponse = outfitAiService.handleTaggedMessage(
                    chatMessage.getMessage(), 
                    chatMessage.getUserIp()
                );
                
                ChatMessageDto aiMessage = ChatMessageDto.builder()
                        .userIp("AI")
                        .message(aiResponse)
                        .messageType(ChatMessage.MessageType.AI_RESPONSE)
                        .build();
                
                chatRoomService.saveMessage(aiMessage);
                messagingTemplate.convertAndSend("/topic/public", aiMessage);
                
            } catch (Exception e) {
                log.error("Error processing AI response", e);
                sendErrorMessage("Oops! My fashion circuits are a bit tangled 🤖💫");
            }
        }).start();
    }

    private void handleOutfitRating(ChatMessageDto outfitMessage) {
        new Thread(() -> {
            try {
                String aiRating = outfitAiService.rateOutfit(outfitMessage.getImageUrl());
                
                ChatMessageDto aiMessage = ChatMessageDto.builder()
                        .userIp("AI")
                        .message(aiRating)
                        .messageType(ChatMessage.MessageType.AI_RESPONSE)
                        .build();
                
                chatRoomService.saveMessage(aiMessage);
                messagingTemplate.convertAndSend("/topic/public", aiMessage);
                
            } catch (Exception e) {
                log.error("Error processing outfit rating", e);
                sendErrorMessage("Sorry, my fashion scanner needs a coffee break! ☕✨");
            }
        }).start();
    }

    private void sendErrorMessage(String message) {
        ChatMessageDto errorMessage = ChatMessageDto.builder()
                .userIp("AI")
                .message(message)
                .messageType(ChatMessage.MessageType.AI_RESPONSE)
                .build();
        
        messagingTemplate.convertAndSend("/topic/public", errorMessage);
    }
}