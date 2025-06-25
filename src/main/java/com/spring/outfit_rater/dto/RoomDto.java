package com.spring.outfit_rater.dto;

import com.spring.outfit_rater.model.RoastRoom;
import com.spring.outfit_rater.model.RoomParticipant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {
    
    private Long id;
    private String roomCode;
    private String roomName;
    private String description;
    private String creatorId;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Boolean isActive;
    private Boolean isPrivate;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean isExpired;
    private Boolean isFull;
    private List<ParticipantDto> participants;
    
    public static RoomDto fromEntity(RoastRoom entity) {
        return RoomDto.builder()
                .id(entity.getId())
                .roomCode(entity.getRoomCode())
                .roomName(entity.getRoomName())
                .description(entity.getDescription())
                .creatorId(entity.getCreatorId())
                .maxParticipants(entity.getMaxParticipants())
                .currentParticipants(entity.getParticipantCount())
                .isActive(entity.getIsActive())
                .isPrivate(entity.getIsPrivate())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .isExpired(entity.isExpired())
                .isFull(entity.isFull())
                .participants(entity.getParticipants().stream()
                        .map(ParticipantDto::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }
    
    public static RoomDto fromEntityBasic(RoastRoom entity) {
        return RoomDto.builder()
                .id(entity.getId())
                .roomCode(entity.getRoomCode())
                .roomName(entity.getRoomName())
                .description(entity.getDescription())
                .creatorId(entity.getCreatorId())
                .maxParticipants(entity.getMaxParticipants())
                .currentParticipants(entity.getParticipantCount())
                .isActive(entity.getIsActive())
                .isPrivate(entity.getIsPrivate())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .isExpired(entity.isExpired())
                .isFull(entity.isFull())
                .build();
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ParticipantDto {
    
    private Long id;
    private String userId;
    private String displayName;
    private RoomParticipant.ParticipantRole role;
    private Boolean isActive;
    private Boolean isOnline;
    private LocalDateTime joinedAt;
    private LocalDateTime lastSeenAt;
    
    public static ParticipantDto fromEntity(RoomParticipant entity) {
        return ParticipantDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .displayName(entity.getDisplayName())
                .role(entity.getRole())
                .isActive(entity.getIsActive())
                .isOnline(entity.isOnline())
                .joinedAt(entity.getJoinedAt())
                .lastSeenAt(entity.getLastSeenAt())
                .build();
    }
}
