package com.spring.outfit_rater.controller;

import com.spring.outfit_rater.dto.*;
import com.spring.outfit_rater.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@Slf4j
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/create")
    public ResponseEntity<RoomResponseDto> createRoom(@Valid @RequestBody CreateRoomRequestDto request,
                                                     HttpServletRequest httpRequest) {
        String userId = getUserId(httpRequest);
        log.info("Creating room request from user: {} with room name: {}", userId, request.getRoomName());
        
        RoomResponseDto response = roomService.createRoom(request, userId);
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/join")
    public ResponseEntity<RoomResponseDto> joinRoom(@Valid @RequestBody JoinRoomRequestDto request,
                                                   HttpServletRequest httpRequest) {
        String userId = getUserId(httpRequest);
        log.info("Join room request from user: {} for room: {}", userId, request.getRoomCode());
        
        RoomResponseDto response = roomService.joinRoom(request, userId);
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<?> getRoomDetails(@PathVariable String roomCode,
                                          HttpServletRequest httpRequest) {
        String userId = getUserId(httpRequest);
        
        return roomService.getRoomByCode(roomCode)
                .map(room -> {
                    if (roomService.isUserInRoom(roomCode, userId)) {
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "room", room,
                                "isParticipant", true
                        ));
                    } else {
                        RoomDto basicRoom = RoomDto.builder()
                                .roomCode(room.getRoomCode())
                                .roomName(room.getRoomName())
                                .description(room.getDescription())
                                .currentParticipants(room.getCurrentParticipants())
                                .maxParticipants(room.getMaxParticipants())
                                .isActive(room.getIsActive())
                                .isExpired(room.getIsExpired())
                                .isFull(room.getIsFull())
                                .build();
                        
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "room", basicRoom,
                                "isParticipant", false,
                                "canJoin", roomService.canUserJoinRoom(roomCode, userId)
                        ));
                    }
                })
                .orElse(ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Room not found"
                )));
    }

    @GetMapping("/my-rooms")
    public ResponseEntity<Map<String, Object>> getMyRooms(HttpServletRequest httpRequest) {
        String userId = getUserId(httpRequest);
        
        List<RoomDto> joinedRooms = roomService.getUserRooms(userId);
        List<RoomDto> createdRooms = roomService.getCreatedRooms(userId);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "joinedRooms", joinedRooms,
                "createdRooms", createdRooms
        ));
    }

    @PostMapping("/{roomCode}/leave")
    public ResponseEntity<RoomResponseDto> leaveRoom(@PathVariable String roomCode,
                                                    HttpServletRequest httpRequest) {
        String userId = getUserId(httpRequest);
        log.info("Leave room request from user: {} for room: {}", userId, roomCode);
        
        RoomResponseDto response = roomService.leaveRoom(roomCode, userId);
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{roomCode}/settings")
    public ResponseEntity<RoomResponseDto> updateRoomSettings(@PathVariable String roomCode,
                                                             @Valid @RequestBody RoomSettingsUpdateDto request,
                                                             HttpServletRequest httpRequest) {
        String userId = getUserId(httpRequest);
        log.info("Update room settings request from user: {} for room: {}", userId, roomCode);
        
        RoomResponseDto response = roomService.updateRoomSettings(roomCode, request, userId);
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{roomCode}/activity")
    public ResponseEntity<Map<String, Object>> updateActivity(@PathVariable String roomCode,
                                                             HttpServletRequest httpRequest) {
        String userId = getUserId(httpRequest);
        
        try {
            roomService.updateParticipantActivity(roomCode, userId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.warn("Failed to update activity for user {} in room {}", userId, roomCode);
            return ResponseEntity.ok(Map.of("success", false));
        }
    }

    @GetMapping("/validate/{roomCode}")
    public ResponseEntity<Map<String, Object>> validateRoomCode(@PathVariable String roomCode,
                                                               HttpServletRequest httpRequest) {
        String userId = getUserId(httpRequest);
        
        boolean canJoin = roomService.canUserJoinRoom(roomCode, userId);
        boolean isParticipant = roomService.isUserInRoom(roomCode, userId);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "exists", roomService.getRoomByCode(roomCode).isPresent(),
                "canJoin", canJoin,
                "isParticipant", isParticipant
        ));
    }

    private String getUserId(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}