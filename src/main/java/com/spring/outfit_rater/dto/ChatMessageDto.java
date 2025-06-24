package com.spring.outfit_rater.dto;

import com.spring.outfit_rater.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    
    private Long id;
    private String userId;
    private String content;
    private String imageUrl;
    private ChatMessage.MessageType type;
    private LocalDateTime createdAt;
    
    public static ChatMessageDto fromEntity(ChatMessage entity) {
        return ChatMessageDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .content(entity.getContent())
                .imageUrl(entity.getImageUrl())
                .type(entity.getType())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    public ChatMessage toEntity() {
        return ChatMessage.builder()
                .userId(this.userId)
                .content(this.content)
                .imageUrl(this.imageUrl)
                .type(this.type != null ? this.type : ChatMessage.MessageType.USER)
                .build();
    }
}