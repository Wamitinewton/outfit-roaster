package com.spring.outfit_rater.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.spring.outfit_rater.dto.ChatMessageDto;
import com.spring.outfit_rater.service.ChatService;
import com.spring.outfit_rater.service.FileService;

import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class OutfitController {

    private final ChatService chatService;
    private final FileService fileService;

    public OutfitController(ChatService chatService, FileService fileService) {
        this.chatService = chatService;
        this.fileService = fileService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/chat";
    }

    @GetMapping("/chat")
    public String chatRoom(Model model, HttpServletRequest request) {
        String userIp = getUserIp(request);
        String displayName = chatService.generateDisplayName(userIp);
        
        model.addAttribute("userIp", userIp);
        model.addAttribute("displayName", displayName);
        
        return "chat";
    }


    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = fileService.saveImage(file);
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

    @GetMapping("/api/chat/recent")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getRecentMessages() {
        List<ChatMessageDto> messages = chatService.getRecentMessages();
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/api/user-info")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getUserInfo(HttpServletRequest request) {
        String userIp = getUserIp(request);
        String displayName = chatService.generateDisplayName(userIp);
        
        return ResponseEntity.ok(Map.of(
            "userIp", userIp,
            "displayName", displayName
        ));
    }


    private String getUserIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}