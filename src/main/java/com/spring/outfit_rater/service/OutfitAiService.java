package com.spring.outfit_rater.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Random;

@Service
@Slf4j
public class OutfitAiService {

    private final WebClient webClient;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    public OutfitAiService(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String rateOutfit(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return getDefaultOutfitResponse();
        }

        try {
            return analyzeOutfitWithVision(imageUrl);
        } catch (Exception e) {
            log.error("Failed to analyze outfit", e);
            return getDefaultOutfitResponse();
        }
    }

    private String analyzeOutfitWithVision(String imageUrl) throws Exception {
        String requestBody = String.format("""
            {
                "model": "gpt-4o",
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "You are a fun, friendly fashion AI in a chat room. Rate this outfit from 1-10 and give specific feedback about colors, fit, styling, and 2-3 actionable tips. Be encouraging but honest. Keep it under 150 words and use emojis! Format: Start with rating like '8.5/10' then feedback."
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": "%s"
                                }
                            }
                        ]
                    }
                ],
                "max_tokens": 300,
                "temperature": 0.7
            }
            """, imageUrl);

        String response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        JsonNode jsonResponse = objectMapper.readTree(response);
        String aiResponse = jsonResponse.path("choices").get(0).path("message").path("content").asText();
        
        return "🔥 **OUTFIT ANALYSIS** 🔥\n\n" + aiResponse;
    }

    public String handleChatMessage(String message) {
        try {
            String prompt = String.format("""
                You are a friendly fashion AI assistant in a chat room. 
                Respond to this message: "%s"
                
                Give fashion advice, styling tips, or just chat about fashion. 
                Be fun, use some emojis, and keep it under 100 words.
                """, message);

            Prompt chatPrompt = new Prompt(prompt);
            return "💬 " + chatModel.call(chatPrompt).getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Failed to generate chat response", e);
            return "💫 Hey there! My fashion brain is taking a quick break. Try asking me again! ✨";
        }
    }

    public String getWelcomeMessage() {
        String[] welcomes = {
            "👋 Welcome to StyleChat! Upload your outfits for real AI ratings and fashion advice! ✨",
            "🎉 Hey fashion lover! I'm here to rate your outfits and give styling tips! 💅",
            "💃 Ready to get some honest outfit feedback? Upload your pics and let's chat style! 🔥"
        };
        return welcomes[random.nextInt(welcomes.length)];
    }

    private String getDefaultOutfitResponse() {
        String[] responses = {
            "🔥 **OUTFIT SPOTTED!** 🔥\n\n7.5/10 - Looking good! 💅\n\nI can sense the style confidence! While I can't see all the details right now, here are some universal tips:\n\n✨ **Style Tips:**\n• Fit is everything - make sure clothes flatter your body\n• Add a pop of color with accessories\n• Confidence is your best accessory!\n\nTell me more about your look and I'll give better advice! 💫",
            
            "👗 **FASHION MOMENT!** 👗\n\n8/10 - Loving the effort! ✨\n\nYour style energy is radiating through the screen! Here's what always works:\n\n🎯 **Pro Tips:**\n• Balance proportions (loose top = fitted bottom)\n• Play with textures for visual interest\n• Don't forget the power of good shoes!\n\nWhat's your favorite piece in this outfit? 💎"
        };
        return responses[random.nextInt(responses.length)];
    }
}