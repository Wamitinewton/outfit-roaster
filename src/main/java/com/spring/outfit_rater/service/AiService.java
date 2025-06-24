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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
            return "Uh, hello? I can't roast what I can't see. Upload a pic so I can properly judge your fashion choices 📸";
        }

        try {
            return analyzeOutfitWithVision(imageUrl, userId);
        } catch (Exception e) {
            log.error("Failed to analyze outfit for user: {}", userId, e);
            return "My roasting servers are down, but honestly your outfit probably deserves it anyway 🔥";
        }
    }

    public String handleChatMessage(String message, String userId) {
        try {
            String conversationContext = buildConversationContext(userId);

            String prompt = String.format("""
                    You are StyleAI, a brutally honest and hilarious fashion roaster. You're like that friend who tells it like it is but makes everyone laugh.

                    Previous conversation context:
                    %s

                    Current message: "%s"

                    Your personality:
                    - Roast people's fashion choices but keep it funny and playful
                    - Use casual, sarcastic language with lots of attitude
                    - Make jokes about trends, colors, fits, and styling fails
                    - Call out questionable fashion decisions with humor
                    - Be sassy but not actually mean-spirited
                    - Use Gen Z/millennial slang and internet humor
                    - Still give actual fashion advice, just wrapped in roasts

                    Keep responses short and punchy (80-120 words max). Use emojis like 💀, 😭, 🔥, 👀, etc. 
                    Be the fashion roaster everyone secretly wants feedback from.
                    """, conversationContext, message);

            Prompt chatPrompt = new Prompt(prompt);
            return chatModel.call(chatPrompt).getResult().getOutput().getText();

        } catch (Exception e) {
            log.error("Failed to generate chat response for user: {}", userId, e);
            return "Even my AI is having a fashion emergency rn. Try again bestie 💀";
        }
    }

    private String analyzeOutfitWithVision(String imageUrl, String userId) throws Exception {
        String conversationContext = buildConversationContext(userId);
        
        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.put("model", "gpt-4o");
        requestJson.put("max_tokens", 400);
        requestJson.put("temperature", 0.8);
        
        ArrayNode messages = objectMapper.createArrayNode();
        
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        
        ArrayNode content = objectMapper.createArrayNode();
        
        ObjectNode textContent = objectMapper.createObjectNode();
        textContent.put("type", "text");
        
        String promptText = "You are StyleAI, the sassiest fashion roaster on the internet. Analyze this outfit like you're reviewing it for your brutally honest fashion TikTok. " +
                "Previous roasts/convos: " + conversationContext + 
                "\n\nYour roast should include:\n" +
                "• A savage but funny rating out of 10 (be harsh but creative)\n" +
                "• Roast what's NOT working (colors, fit, styling choices, etc.)\n" +
                "• Maybe find ONE thing that doesn't make you cry\n" +
                "• Give 2-3 actually helpful tips but make them sound like friendly insults\n" +
                "• Reference their past fashion crimes if relevant\n\n" +
                "Use casual, sarcastic language. Think 'best friend who has no filter' energy. " +
                "Be funny, not actually cruel. Keep it under 180 words and use emojis like 💀😭🔥👀🚫✨";
        
        textContent.put("text", promptText);
        content.add(textContent);
        
        ObjectNode imageContent = objectMapper.createObjectNode();
        imageContent.put("type", "image_url");
        
        ObjectNode imageUrlObj = objectMapper.createObjectNode();
        imageUrlObj.put("url", imageUrl);
        imageUrlObj.put("detail", "high");
        imageContent.set("image_url", imageUrlObj);
        content.add(imageContent);
        
        userMessage.set("content", content);
        messages.add(userMessage);
        requestJson.set("messages", messages);
        
        log.info("Sending roast request to OpenAI with image URL: {}", imageUrl);
        
        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        
            log.info("OpenAI roast response received successfully");
        
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode choicesArray = jsonResponse.path("choices");
            
            if (choicesArray.isEmpty()) {
                return "Bestie, I can't even process this mess. Try again with a clearer pic 💀";
            }
            
            JsonNode contentNode = choicesArray.get(0).path("message").path("content");
            
            if (contentNode.isMissingNode() || contentNode.isNull() || contentNode.asText().trim().isEmpty()) {
                return "Your outfit broke my AI. That's... actually impressive in the worst way 😭";
            }
        
            String analysisText = contentNode.asText();
            
            return formatAnalysisResponse(analysisText);
            
        } catch (WebClientResponseException e) {
            log.error("OpenAI API error - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode().value() == 400) {
                String errorBody = e.getResponseBodyAsString();
                if (errorBody.contains("invalid_image_url") || errorBody.contains("image")) {
                    return "That image link is as broken as your fashion sense. Upload properly bestie 📸";
                } else if (errorBody.contains("invalid_api_key")) {
                    return "Technical difficulties. Even my roasting skills have limits apparently 🔧";
                } else {
                    log.error("Bad request details: {}", errorBody);
                    return "Something's glitched. Maybe it's your outfit, maybe it's my code 🤷‍♀️";
                }
            } else if (e.getStatusCode().value() == 401) {
                return "Authentication failed harder than your outfit coordination 🔐";
            } else if (e.getStatusCode().value() == 429) {
                return "I'm too busy roasting other people's fits. Give me a sec ⏰";
            } else {
                return "Technical meltdown in progress. At least it's not as bad as your outfit choice 💀";
            }
        }
    }

    private String formatAnalysisResponse(String rawResponse) {
        String formatted = rawResponse
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("\\*(.*?)\\*", "$1")
                .replaceAll("•", "💀")
                .replaceAll("- ", "🔥 ")
                .trim();

        return formatted;
    }

    private String buildConversationContext(String userId) {
        List<ChatMessage> recentMessages = messageRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        if (recentMessages.isEmpty()) {
            return "No previous roasts to reference.";
        }

        StringBuilder context = new StringBuilder("Previous roasting session:\n");
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            ChatMessage msg = recentMessages.get(i);
            String role = msg.getType() == ChatMessage.MessageType.AI ? "StyleAI" : "User";

            if (msg.getImageUrl() != null) {
                context.append(String.format("- %s posted another questionable outfit\n", role));
            } else {
                String cleanContent = msg.getContent()
                        .replaceAll("[\r\n]+", " ")
                        .replaceAll("\"", "'")
                        .trim();

                if (cleanContent.length() > 100) {
                    cleanContent = cleanContent.substring(0, 100) + "...";
                }

                context.append(String.format("- %s: %s\n", role, cleanContent));
            }
        }

        return context.toString();
    }

    public String getWelcomeMessage() {
        return "Welcome to the roast zone! 🔥 I'm your brutally honest AI fashion critic. Send me your fits and I'll tell you exactly what I think... no sugar-coating, just pure roasting energy. Ready to get humbled? 💀";
    }
}