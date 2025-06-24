package com.spring.outfit_rater.controller;

import com.spring.outfit_rater.dto.ChatMessageDto;
import com.spring.outfit_rater.service.ChatService;
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

    public ChatController(ChatService chatService, StorageService storageService) {
        this.chatService = chatService;
        this.storageService = storageService;
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

    @GetMapping("/api/messages/{userId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getUserMessages(@PathVariable String userId) {
        List<ChatMessageDto> messages = chatService.getUserConversation(userId);
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

    private String getUserId(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
