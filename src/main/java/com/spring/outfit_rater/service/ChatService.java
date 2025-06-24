package com.spring.outfit_rater.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.spring.outfit_rater.dto.ChatMessageDto;
import com.spring.outfit_rater.model.ChatMessage;
import com.spring.outfit_rater.repository.ChatMessageRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatRepository;

    public ChatService(ChatMessageRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public ChatMessage saveMessage(ChatMessageDto dto) {
        ChatMessage message = ChatMessage.builder()
                .userIp(dto.getUserIp())
                .message(dto.getMessage())
                .imageUrl(dto.getImageUrl())
                .messageType(dto.getMessageType() != null ? dto.getMessageType() : ChatMessage.MessageType.USER_MESSAGE)
                .build();
        
        return chatRepository.save(message);
    }

    public List<ChatMessageDto> getRecentMessages() {
        return chatRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    public String generateDisplayName(String userIp) {
        String[] adjectives = {"Stylish", "Trendy", "Chic", "Fabulous", "Dapper", "Elegant"};
        String[] nouns = {"Fashionista", "StyleIcon", "TrendSetter", "Designer"};
        
        int hash = Math.abs(userIp.hashCode());
        return adjectives[hash % adjectives.length] + 
               nouns[(hash / adjectives.length) % nouns.length] + 
               (hash % 100);
    }
}