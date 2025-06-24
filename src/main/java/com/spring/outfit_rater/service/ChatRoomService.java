package com.spring.outfit_rater.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.spring.outfit_rater.dto.ChatMessageDto;
import com.spring.outfit_rater.model.ChatMessage;
import com.spring.outfit_rater.repository.ChatMessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatRoomService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatRoomService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public ChatMessage saveMessage(ChatMessageDto messageDto) {
        ChatMessage message = ChatMessage.builder()
                .userIp(messageDto.getUserIp())
                .message(messageDto.getMessage())
                .imageUrl(messageDto.getImageUrl())
                .messageType(messageDto.getMessageType() != null ? 
                    messageDto.getMessageType() : ChatMessage.MessageType.USER_MESSAGE)
                .build();
        
        log.info("Saving message: {} from {}", message.getMessage(), message.getUserIp());
        return chatMessageRepository.save(message);
    }

    public List<ChatMessageDto> getRecentMessages() {
        return chatMessageRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ChatMessageDto> getMessagesSince(LocalDateTime since) {
        return chatMessageRepository.findRecentMessages(since)
                .stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ChatMessageDto> getUserMessages(String userIp) {
        return chatMessageRepository.findByUserIpOrderByCreatedAtDesc(userIp)
                .stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    public String generateUserDisplayName(String userIp) {
        String[] adjectives = {"Stylish", "Trendy", "Chic", "Fabulous", "Dapper", "Elegant", "Bold", "Vibrant"};
        String[] nouns = {"Fashionista", "StyleIcon", "TrendSetter", "Designer", "Model", "Influencer", "Creator", "Guru"};
        
        int hash = Math.abs(userIp.hashCode());
        String adjective = adjectives[hash % adjectives.length];
        String noun = nouns[(hash / adjectives.length) % nouns.length];
        
        return adjective + noun + (hash % 100);
    }
}
