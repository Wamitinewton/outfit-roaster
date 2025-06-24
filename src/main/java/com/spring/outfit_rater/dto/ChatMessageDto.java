package com.spring.outfit_rater.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.spring.outfit_rater.model.ChatMessage;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    
    private Long id;
    private String userIp;
    private String message;
    private String imageUrl;
    private ChatMessage.MessageType messageType;
    private LocalDateTime createdAt;
    
    public static ChatMessageDto fromEntity(ChatMessage entity) {
        return ChatMessageDto.builder()
                .id(entity.getId())
                .userIp(entity.getUserIp())
                .message(entity.getMessage())
                .imageUrl(entity.getImageUrl())
                .messageType(entity.getMessageType())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    public ChatMessage toEntity() {
        return ChatMessage.builder()
                .id(this.id)
                .userIp(this.userIp)
                .message(this.message)
                .imageUrl(this.imageUrl)
                .messageType(this.messageType)
                .createdAt(this.createdAt)
                .build();
    }
}
