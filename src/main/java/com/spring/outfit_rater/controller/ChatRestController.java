package com.spring.outfit_rater.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.spring.outfit_rater.dto.ChatMessageDto;
import com.spring.outfit_rater.service.ChatRoomService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatRestController {

    private final ChatRoomService chatRoomService;

    public ChatRestController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ChatMessageDto>> getRecentMessages() {
        List<ChatMessageDto> messages = chatRoomService.getRecentMessages();
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/since")
    public ResponseEntity<List<ChatMessageDto>> getMessagesSince(@RequestParam String since) {
        LocalDateTime sinceDateTime = LocalDateTime.parse(since);
        List<ChatMessageDto> messages = chatRoomService.getMessagesSince(sinceDateTime);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/user-info")
    public ResponseEntity<Map<String, String>> getUserInfo(HttpServletRequest request) {
        String userIp = getClientIpAddress(request);
        String displayName = chatRoomService.generateUserDisplayName(userIp);
        
        return ResponseEntity.ok(Map.of(
            "userIp", userIp,
            "displayName", displayName
        ));
    }

    @GetMapping("/my-messages")
    public ResponseEntity<List<ChatMessageDto>> getMyMessages(HttpServletRequest request) {
        String userIp = getClientIpAddress(request);
        List<ChatMessageDto> messages = chatRoomService.getUserMessages(userIp);
        return ResponseEntity.ok(messages);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0];
        }
    }
}