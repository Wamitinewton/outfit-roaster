package com.spring.outfit_rater.service;

import com.spring.outfit_rater.dto.*;
import com.spring.outfit_rater.exception.RoomException;
import com.spring.outfit_rater.model.RoastRoom;
import com.spring.outfit_rater.model.RoomParticipant;
import com.spring.outfit_rater.repository.RoastRoomRepository;
import com.spring.outfit_rater.repository.RoomParticipantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class RoomService {

    private final RoastRoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final ChatService chatService;
    
    private static final int MAX_ROOMS_PER_USER = 3;
    private static final String ROOM_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random random = new Random();

    public RoomService(RoastRoomRepository roomRepository, 
                      RoomParticipantRepository participantRepository,
                      ChatService chatService) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.chatService = chatService;
    }

    public RoomResponseDto createRoom(CreateRoomRequestDto request, String creatorId) {
        try {
            // Validate creator limits
            validateCreatorLimits(creatorId);
            
            // Generate unique room code
            String roomCode = generateUniqueRoomCode();
            
            // Create room entity
            RoastRoom room = RoastRoom.builder()
                    .roomCode(roomCode)
                    .roomName(request.getRoomName().trim())
                    .description(request.getDescription() != null ? request.getDescription().trim() : null)
                    .creatorId(creatorId)
                    .maxParticipants(request.getMaxParticipants())
                    .isPrivate(request.getIsPrivate())
                    .isActive(true)
                    .expiresAt(LocalDateTime.now().plusHours(request.getDurationHours()))
                    .build();
            
            room = roomRepository.save(room);
            
            // Add creator as first participant
            addCreatorAsParticipant(room, creatorId);
            
            // Send system message
            sendSystemMessage(roomCode, String.format("🎉 %s created the roast room!", 
                    chatService.generateDisplayName(creatorId)));
            
            log.info("Room created successfully - Code: {}, Creator: {}", roomCode, creatorId);
            
            return RoomResponseDto.success(
                    RoomDto.fromEntity(room),
                    "Room created successfully! Share the code with friends."
            );
            
        } catch (RoomException e) {
            log.warn("Room creation failed: {}", e.getMessage());
            return RoomResponseDto.error(e.getMessage(), e.getErrorCode());
        } catch (Exception e) {
            log.error("Unexpected error creating room", e);
            return RoomResponseDto.error("Failed to create room. Please try again.", "INTERNAL_ERROR");
        }
    }

    public RoomResponseDto joinRoom(JoinRoomRequestDto request, String userId) {
        try {
            String roomCode = request.getRoomCode().toUpperCase().trim();
            
            // Find room
            RoastRoom room = roomRepository.findByRoomCode(roomCode)
                    .orElseThrow(() -> new RoomException("Room not found", "ROOM_NOT_FOUND"));
            
            // Validate room state
            validateRoomForJoining(room, userId);
            
            // Check if user is already a participant
            Optional<RoomParticipant> existingParticipant = 
                    participantRepository.findByRoomCodeAndUserId(roomCode, userId);
            
            if (existingParticipant.isPresent()) {
                RoomParticipant participant = existingParticipant.get();
                if (participant.getIsActive()) {
                    return RoomResponseDto.error("You're already in this room", "ALREADY_JOINED");
                } else {
                    // Reactivate participant
                    participant.setIsActive(true);
                    participant.updateLastSeen();
                    participantRepository.save(participant);
                }
            } else {
                // Create new participant
                String displayName = request.getDisplayName() != null && !request.getDisplayName().trim().isEmpty()
                        ? request.getDisplayName().trim()
                        : chatService.generateDisplayName(userId);
                
                RoomParticipant participant = RoomParticipant.builder()
                        .room(room)
                        .userId(userId)
                        .displayName(displayName)
                        .role(RoomParticipant.ParticipantRole.MEMBER)
                        .isActive(true)
                        .build();
                
                participantRepository.save(participant);
            }
            
            // Send join message
            sendSystemMessage(roomCode, String.format("👋 %s joined the roast session!", 
                    chatService.generateDisplayName(userId)));
            
            log.info("User {} joined room {}", userId, roomCode);
            
            // Refresh room data
            room = roomRepository.findByRoomCode(roomCode).get();
            return RoomResponseDto.success(
                    RoomDto.fromEntity(room),
                    "Successfully joined the roast room!"
            );
            
        } catch (RoomException e) {
            log.warn("Room join failed: {}", e.getMessage());
            return RoomResponseDto.error(e.getMessage(), e.getErrorCode());
        } catch (Exception e) {
            log.error("Unexpected error joining room", e);
            return RoomResponseDto.error("Failed to join room. Please try again.", "INTERNAL_ERROR");
        }
    }

    @Transactional(readOnly = true)
    public Optional<RoomDto> getRoomByCode(String roomCode) {
        return roomRepository.findByRoomCode(roomCode.toUpperCase().trim())
                .map(RoomDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<RoomDto> getUserRooms(String userId) {
        return roomRepository.findRoomsByParticipant(userId)
                .stream()
                .map(RoomDto::fromEntityBasic)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomDto> getCreatedRooms(String creatorId) {
        return roomRepository.findByCreatorIdOrderByCreatedAtDesc(creatorId)
                .stream()
                .map(RoomDto::fromEntityBasic)
                .collect(Collectors.toList());
    }

    public RoomResponseDto leaveRoom(String roomCode, String userId) {
        try {
            RoomParticipant participant = participantRepository
                    .findByRoomCodeAndUserId(roomCode.toUpperCase().trim(), userId)
                    .orElseThrow(() -> new RoomException("You're not in this room", "NOT_PARTICIPANT"));
            
            // Deactivate participant
            participant.setIsActive(false);
            participantRepository.save(participant);
            
            // Send leave message
            sendSystemMessage(roomCode, String.format("👋 %s left the roast session", 
                    participant.getDisplayName()));
            
            log.info("User {} left room {}", userId, roomCode);
            
            return RoomResponseDto.success(null, "Successfully left the room");
            
        } catch (RoomException e) {
            return RoomResponseDto.error(e.getMessage(), e.getErrorCode());
        } catch (Exception e) {
            log.error("Error leaving room", e);
            return RoomResponseDto.error("Failed to leave room", "INTERNAL_ERROR");
        }
    }

    public RoomResponseDto updateRoomSettings(String roomCode, RoomSettingsUpdateDto request, String userId) {
        try {
            RoastRoom room = roomRepository.findByRoomCode(roomCode.toUpperCase().trim())
                    .orElseThrow(() -> new RoomException("Room not found", "ROOM_NOT_FOUND"));
            
            // Validate user is creator
            if (!room.isCreator(userId)) {
                throw new RoomException("Only room creator can update settings", "UNAUTHORIZED");
            }
            
            // Update room settings
            if (request.getRoomName() != null) {
                room.setRoomName(request.getRoomName().trim());
            }
            
            if (request.getDescription() != null) {
                room.setDescription(request.getDescription().trim());
            }
            
            if (request.getMaxParticipants() != null) {
                int currentParticipants = room.getParticipantCount();
                if (request.getMaxParticipants() < currentParticipants) {
                    throw new RoomException(
                            String.format("Cannot reduce limit below current participants (%d)", currentParticipants),
                            "INVALID_PARTICIPANT_LIMIT"
                    );
                }
                room.setMaxParticipants(request.getMaxParticipants());
            }
            
            if (request.getExtendHours() != null) {
                room.setExpiresAt(room.getExpiresAt().plusHours(request.getExtendHours()));
            }
            
            if (request.getIsActive() != null) {
                room.setIsActive(request.getIsActive());
            }
            
            room = roomRepository.save(room);
            
            log.info("Room {} settings updated by creator {}", roomCode, userId);
            
            return RoomResponseDto.success(
                    RoomDto.fromEntity(room),
                    "Room settings updated successfully"
            );
            
        } catch (RoomException e) {
            return RoomResponseDto.error(e.getMessage(), e.getErrorCode());
        } catch (Exception e) {
            log.error("Error updating room settings", e);
            return RoomResponseDto.error("Failed to update room settings", "INTERNAL_ERROR");
        }
    }

    public void updateParticipantActivity(String roomCode, String userId) {
        try {
            participantRepository.updateLastSeenAt(userId, roomCode.toUpperCase().trim(), LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Failed to update participant activity for user {} in room {}", userId, roomCode);
        }
    }

    @Transactional(readOnly = true)
    public boolean isUserInRoom(String roomCode, String userId) {
        return participantRepository.findByRoomCodeAndUserId(roomCode.toUpperCase().trim(), userId)
                .map(participant -> participant.getIsActive())
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canUserJoinRoom(String roomCode, String userId) {
        Optional<RoastRoom> roomOpt = roomRepository.findByRoomCode(roomCode.toUpperCase().trim());
        if (roomOpt.isEmpty()) {
            return false;
        }
        
        RoastRoom room = roomOpt.get();
        return room.getIsActive() && !room.isExpired() && !room.isFull();
    }

    public void deactivateExpiredRooms() {
        try {
            List<RoastRoom> expiredRooms = roomRepository.findExpiredRooms(LocalDateTime.now());
            for (RoastRoom room : expiredRooms) {
                room.setIsActive(false);
                roomRepository.save(room);
                
                sendSystemMessage(room.getRoomCode(), 
                        "⏰ This roast room has expired and is now closed. Thanks for the fashion fun!");
                
                log.info("Deactivated expired room: {}", room.getRoomCode());
            }
        } catch (Exception e) {
            log.error("Error deactivating expired rooms", e);
        }
    }

    private void validateCreatorLimits(String creatorId) {
        int activeRooms = roomRepository.countActiveRoomsByCreator(creatorId, LocalDateTime.now());
        if (activeRooms >= MAX_ROOMS_PER_USER) {
            throw new RoomException(
                    String.format("You can only have %d active rooms at a time", MAX_ROOMS_PER_USER),
                    "ROOM_LIMIT_EXCEEDED"
            );
        }
    }

    private String generateUniqueRoomCode() {
        String roomCode;
        int attempts = 0;
        final int maxAttempts = 100;
        
        do {
            roomCode = generateRoomCode();
            attempts++;
            
            if (attempts > maxAttempts) {
                throw new RoomException("Unable to generate unique room code", "CODE_GENERATION_FAILED");
            }
        } while (roomRepository.existsByRoomCode(roomCode));
        
        return roomCode;
    }

    private String generateRoomCode() {
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            code.append(ROOM_CODE_CHARS.charAt(random.nextInt(ROOM_CODE_CHARS.length())));
        }
        return code.toString();
    }

    private void validateRoomForJoining(RoastRoom room, String userId) {
        if (!room.getIsActive()) {
            throw new RoomException("This room is no longer active", "ROOM_INACTIVE");
        }
        
        if (room.isExpired()) {
            throw new RoomException("This room has expired", "ROOM_EXPIRED");
        }
        
        if (room.isFull()) {
            throw new RoomException("This room is full", "ROOM_FULL");
        }
    }

    private void addCreatorAsParticipant(RoastRoom room, String creatorId) {
        RoomParticipant creator = RoomParticipant.builder()
                .room(room)
                .userId(creatorId)
                .displayName(chatService.generateDisplayName(creatorId))
                .role(RoomParticipant.ParticipantRole.CREATOR)
                .isActive(true)
                .build();
        
        participantRepository.save(creator);
    }

    private void sendSystemMessage(String roomCode, String content) {
        try {
            ChatMessageDto systemMessage = ChatMessageDto.builder()
                    .userId("System")
                    .content(content)
                    .type(com.spring.outfit_rater.model.ChatMessage.MessageType.SYSTEM)
                    .roomCode(roomCode)
                    .build();
            
            chatService.saveMessage(systemMessage);
        } catch (Exception e) {
            log.warn("Failed to send system message to room {}: {}", roomCode, e.getMessage());
        }
    }}