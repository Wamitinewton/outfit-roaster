package com.spring.outfit_rater.service;

import com.spring.outfit_rater.dto.ChatMessageDto;
import com.spring.outfit_rater.model.ChatMessage;
import com.spring.outfit_rater.model.Conversation;
import com.spring.outfit_rater.repository.ChatMessageRepository;
import com.spring.outfit_rater.repository.ConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    public ChatService(ChatMessageRepository messageRepository, ConversationRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public ChatMessage saveMessage(ChatMessageDto dto) {
        ensureConversationExists(dto.getUserId());
        
        ChatMessage message = dto.toEntity();
        ChatMessage saved = messageRepository.save(message);
        
        log.info("Message saved - User: {}, Type: {}", dto.getUserId(), dto.getType());
        return saved;
    }

    public List<ChatMessageDto> getRecentPublicMessages() {
        return messageRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ChatMessageDto> getUserConversation(String userId) {
        return messageRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    private void ensureConversationExists(String userId) {
        if (conversationRepository.findByUserId(userId).isEmpty()) {
            Conversation conversation = Conversation.builder()
                    .userId(userId)
                    .build();
            conversationRepository.save(conversation);
            log.info("Created new conversation for user: {}", userId);
        }
    }

    public String generateDisplayName(String userId) {
        String[] adjectives = {"Stylish", "Trendy", "Chic", "Elegant", "Dapper", "Sophisticated"};
        String[] nouns = {"Fashionista", "StyleIcon", "TrendSetter", "Designer", "Curator", "Maven"};
        
        int hash = Math.abs(userId.hashCode());
        return adjectives[hash % adjectives.length] + 
               nouns[(hash / adjectives.length) % nouns.length] + 
               (hash % 1000);
    }
}