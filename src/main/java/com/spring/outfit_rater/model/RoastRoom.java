package com.spring.outfit_rater.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roast_rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoastRoom {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "room_code", nullable = false, unique = true, length = 8)
    private String roomCode;
    
    @Column(name = "room_name", nullable = false, length = 100)
    private String roomName;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "creator_id", nullable = false)
    private String creatorId;
    
    @Column(name = "max_participants")
    @Builder.Default
    private Integer maxParticipants = 20;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_private")
    @Builder.Default
    private Boolean isPrivate = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("joinedAt ASC")
    @Builder.Default
    private List<RoomParticipant> participants = new ArrayList<>();
    
    @OneToMany(mappedBy = "roomCode", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
    
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        
        if (this.expiresAt == null) {
            this.expiresAt = now.plusHours(24);
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isFull() {
        return participants.size() >= maxParticipants;
    }
    
    public boolean isCreator(String userId) {
        return creatorId.equals(userId);
    }
    
    public int getParticipantCount() {
        return (int) participants.stream()
                .filter(RoomParticipant::getIsActive)
                .count();
    }
}
