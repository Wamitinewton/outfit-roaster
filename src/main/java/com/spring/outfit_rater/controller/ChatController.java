package com.spring.outfit_rater.controller;

import com.spring.outfit_rater.dto.ChatMessageDto;
import com.spring.outfit_rater.service.ChatService;
import com.spring.outfit_rater.service.RoomService;
import com.spring.outfit_rater.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final StorageService storageService;
    private final RoomService roomService;

    public ChatController(ChatService chatService, StorageService storageService, RoomService roomService) {
        this.chatService = chatService;
        this.storageService = storageService;
        this.roomService = roomService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/chat";
    }

    @GetMapping("/chat")
    public String chatRoom(Model model, HttpServletRequest request) {
        String userId = getUserId(request);
        String displayName = chatService.generateDisplayName(userId);
        
        model.addAttribute("userId", userId);
        model.addAttribute("displayName", displayName);
        
        return "chat";
    }

    @GetMapping("/room/{roomCode}")
    public String roomChat(@PathVariable String roomCode, Model model, HttpServletRequest request) {
        String userId = getUserId(request);
        String displayName = chatService.generateDisplayName(userId);
        
        if (!roomService.isUserInRoom(roomCode, userId) && !roomService.canUserJoinRoom(roomCode, userId)) {
            return "redirect:/chat?error=room_access_denied";
        }
        
        model.addAttribute("userId", userId);
        model.addAttribute("displayName", displayName);
        model.addAttribute("roomCode", roomCode.toUpperCase());
        
        return "chat";
    }

    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = storageService.uploadImage(file);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "imageUrl", imageUrl
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Upload failed. Please try again."
            ));
        }
    }

    @GetMapping("/api/messages/recent")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getRecentMessages() {
        List<ChatMessageDto> messages = chatService.getRecentPublicMessages();
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/api/messages/room/{roomCode}")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getRoomMessages(@PathVariable String roomCode,
                                                               @RequestParam(defaultValue = "50") int limit,
                                                               HttpServletRequest request) {
        String userId = getUserId(request);
        
        if (!roomService.isUserInRoom(roomCode, userId)) {
            return ResponseEntity.badRequest().body(null);
        }
        
        List<ChatMessageDto> messages = chatService.getRoomMessages(roomCode, limit);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/api/messages/{userId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getUserMessages(@PathVariable String userId) {
        List<ChatMessageDto> messages = chatService.getUserConversation(userId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/api/messages/{userId}/room/{roomCode}")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getUserRoomMessages(@PathVariable String userId,
                                                                   @PathVariable String roomCode,
                                                                   HttpServletRequest request) {
        String requestUserId = getUserId(request);
        
        if (!requestUserId.equals(userId)) {
            return ResponseEntity.badRequest().body(null);
        }
        
        if (!roomService.isUserInRoom(roomCode, userId)) {
            return ResponseEntity.badRequest().body(null);
        }
        
        List<ChatMessageDto> messages = chatService.getUserRoomConversation(userId, roomCode);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/api/user")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getUserInfo(HttpServletRequest request) {
        String userId = getUserId(request);
        String displayName = chatService.generateDisplayName(userId);
        
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "displayName", displayName
        ));
    }

    @PostMapping("/api/activity/room/{roomCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateRoomActivity(@PathVariable String roomCode,
                                                                 HttpServletRequest request) {
        String userId = getUserId(request);
        
        try {
            roomService.updateParticipantActivity(roomCode, userId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.warn("Failed to update room activity for user {} in room {}", userId, roomCode);
            return ResponseEntity.ok(Map.of("success", false));
        }
    }

    private String getUserId(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}