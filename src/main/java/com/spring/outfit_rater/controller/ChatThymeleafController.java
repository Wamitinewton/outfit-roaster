package com.spring.outfit_rater.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.spring.outfit_rater.service.ChatRoomService;

@Controller
public class ChatThymeleafController {

    private final ChatRoomService chatRoomService;

    public ChatThymeleafController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/chat";
    }

    @GetMapping("/chat")
    public String chatRoom(Model model, HttpServletRequest request) {
        String userIp = getClientIpAddress(request);
        String displayName = chatRoomService.generateUserDisplayName(userIp);
        
        model.addAttribute("userIp", userIp);
        model.addAttribute("displayName", displayName);
        
        return "chat";
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