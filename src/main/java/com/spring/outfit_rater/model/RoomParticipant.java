package com.spring.outfit_rater.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_participants", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomParticipant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoastRoom room;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "display_name", nullable = false)
    private String displayName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ParticipantRole role = ParticipantRole.MEMBER;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
    
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;
    
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.joinedAt = now;
        this.lastSeenAt = now;
    }
    
    public void updateLastSeen() {
        this.lastSeenAt = LocalDateTime.now();
    }
    
    public boolean isOnline() {
        if (lastSeenAt == null) return false;
        return lastSeenAt.isAfter(LocalDateTime.now().minusMinutes(5));
    }
    
    public enum ParticipantRole {
        CREATOR,
        MODERATOR, 
        MEMBER
    }
}
