package com.spring.outfit_rater.service;

import com.spring.outfit_rater.model.ChatMessage;
import com.spring.outfit_rater.repository.ChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiService {

    private final WebClient webClient;
    private final ChatModel chatModel;
    private final ChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    public AiService(ChatModel chatModel, ChatMessageRepository messageRepository) {
        this.chatModel = chatModel;
        this.messageRepository = messageRepository;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String analyzeOutfit(String imageUrl, String userId) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "I need to see your outfit to give you feedback! Please upload an image.";
        }

        try {
            return analyzeOutfitWithVision(imageUrl, userId);
        } catch (Exception e) {
            log.error("Failed to analyze outfit for user: {}", userId, e);
            return "I'm having trouble analyzing your outfit right now. Your style looks great though! 💫";
        }
    }

    public String handleChatMessage(String message, String userId) {
        try {
            String conversationContext = buildConversationContext(userId);
            
            String prompt = String.format("""
                You are StyleAI, a knowledgeable and friendly fashion assistant in a chat room.
                
                Previous conversation context:
                %s
                
                Current message: "%s"
                
                Respond naturally to this message. You can:
                - Give fashion advice and styling tips
                - Answer questions about trends, colors, fit, styling
                - Reference previous messages in the conversation
                - Ask follow-up questions to give better advice
                - Be encouraging and supportive
                
                Keep responses conversational (100-150 words), helpful, and use 1-2 relevant emojis maximum.
                """, conversationContext, message);

            Prompt chatPrompt = new Prompt(prompt);
            return chatModel.call(chatPrompt).getResult().getOutput().getText();
            
        } catch (Exception e) {
            log.error("Failed to generate chat response for user: {}", userId, e);
            return "I'm having a quick wardrobe malfunction! Could you try that again? 💫";
        }
    }

    private String analyzeOutfitWithVision(String imageUrl, String userId) throws Exception {
        String conversationContext = buildConversationContext(userId);
    
        JsonNode requestJson = objectMapper.readTree(String.format("""
            {
              "model": "gpt-4o",
              "messages": [
                {
                  "role": "user",
                  "content": [
                    {
                      "type": "text",
                      "text": "You are StyleAI, a professional fashion consultant. Analyze this outfit considering the user's previous style conversations:\\n\\n%s\\n\\nProvide:\\n1. Overall rating (X/10)\\n2. What works well\\n3. 2-3 specific improvement suggestions\\n4. Consider their style preferences from past conversations\\n\\nBe constructive, specific, and encouraging. Keep under 200 words with minimal emojis."
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
              "max_tokens": 400,
              "temperature": 0.7
            }
            """, conversationContext, imageUrl));
    
        String response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    
        log.info("OpenAI response: {}", response);
    
        JsonNode jsonResponse = objectMapper.readTree(response);
        JsonNode contentNode = jsonResponse.path("choices").get(0).path("message").path("content");
        
        if (contentNode.isMissingNode() || contentNode.isNull()) {
            return "🧐 StyleAI couldn't process the outfit right now. Try again with a clear image!";
        }
    
        return contentNode.asText();
    }
    

    private String buildConversationContext(String userId) {
        List<ChatMessage> recentMessages = messageRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        if (recentMessages.isEmpty()) {
            return "No previous conversation history.";
        }

        StringBuilder context = new StringBuilder("Recent conversation:\n");
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            ChatMessage msg = recentMessages.get(i);
            String role = msg.getType() == ChatMessage.MessageType.AI ? "StyleAI" : "User";
            
            if (msg.getImageUrl() != null) {
                context.append(String.format("- %s shared an outfit image\n", role));
            } else {
                context.append(String.format("- %s: %s\n", role, 
                    msg.getContent().length() > 100 ? 
                    msg.getContent().substring(0, 100) + "..." : 
                    msg.getContent()));
            }
        }
        
        return context.toString();
    }

    public String getWelcomeMessage() {
        return "Welcome to StyleChat! I'm your AI fashion companion. Share your outfits for honest feedback or ask me anything about style, trends, and fashion advice! 👗";
    }
}