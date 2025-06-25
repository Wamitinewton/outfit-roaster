package com.spring.outfit_rater.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(nullable = false, length = 2000)
    private String content;
    
    @Column(name = "image_url", length = 2000)
    private String imageUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;
    
    @Column(name = "room_code", length = 8)
    private String roomCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private RoastRoom room;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "reaction_count")
    @Builder.Default
    private Integer reactionCount = 0;
    
    @Column(name = "is_edited")
    @Builder.Default
    private Boolean isEdited = false;
    
    @Column(name = "edited_at")
    private LocalDateTime editedAt;
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
    
    public boolean isInRoom() {
        return roomCode != null && !roomCode.trim().isEmpty();
    }
    
    public boolean isGlobalMessage() {
        return roomCode == null || roomCode.trim().isEmpty();
    }
    
    public void markAsEdited() {
        this.isEdited = true;
        this.editedAt = LocalDateTime.now();
    }
    
    public enum MessageType {
        USER,
        AI,
        OUTFIT,
        SYSTEM,
        ROOM_JOIN,
        ROOM_LEAVE
    }
}