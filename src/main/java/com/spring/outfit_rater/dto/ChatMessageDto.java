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
    private String roomCode;
    private LocalDateTime createdAt;
    private Integer reactionCount;
    private Boolean isEdited;
    private LocalDateTime editedAt;
    
    public static ChatMessageDto fromEntity(ChatMessage entity) {
        return ChatMessageDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .content(entity.getContent())
                .imageUrl(entity.getImageUrl())
                .type(entity.getType())
                .roomCode(entity.getRoomCode())
                .createdAt(entity.getCreatedAt())
                .reactionCount(entity.getReactionCount())
                .isEdited(entity.getIsEdited())
                .editedAt(entity.getEditedAt())
                .build();
    }
    
    public ChatMessage toEntity() {
        return ChatMessage.builder()
                .userId(this.userId)
                .content(this.content)
                .imageUrl(this.imageUrl)
                .type(this.type != null ? this.type : ChatMessage.MessageType.USER)
                .roomCode(this.roomCode)
                .reactionCount(this.reactionCount != null ? this.reactionCount : 0)
                .isEdited(this.isEdited != null ? this.isEdited : false)
                .editedAt(this.editedAt)
                .build();
    }
    
    public boolean isRoomMessage() {
        return roomCode != null && !roomCode.trim().isEmpty();
    }
    
    public boolean isGlobalMessage() {
        return roomCode == null || roomCode.trim().isEmpty();
    }
}