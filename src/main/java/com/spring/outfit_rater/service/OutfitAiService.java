package com.spring.outfit_rater.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;

@Service
public class OutfitAiService {

    private final ChatModel chatModel;
    private final Random random = new Random();

    private final String[] aiPersonalities = {
        "sassy fashion guru who loves to roast but always ends with love",
        "supportive style mentor who gives constructive feedback with encouragement", 
        "witty fashion critic who makes clever observations and pop culture references",
        "trendy influencer who speaks in fashion slang and uses lots of emojis",
        "professional stylist who gives detailed technical fashion advice"
    };

    public OutfitAiService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String rateOutfit(String imageUrl) {
        String personality = aiPersonalities[random.nextInt(aiPersonalities.length)];
        
        String template = """
                You are a {personality} in a fun outfit rating chatroom.
                
                Someone just uploaded an outfit image! Even though I can't see the specific details right now,
                I want to create an engaging response that:
                
                1. Shows excitement about the outfit upload 
                2. Gives a fun preliminary reaction (rate it 7-9/10 to be encouraging)
                3. Asks them to describe key elements (colors, style, pieces, occasion)
                4. Provides 2-3 general styling tips that work for most outfits
                5. Creates anticipation for a detailed review once they describe it
                
                Be enthusiastic, use emojis strategically, and keep the energy high!
                Keep response under 150 words.
                
                Start with something attention-grabbing like:
                "🔥 OUTFIT DROP DETECTED! 🔥" or "👗 FASHION MOMENT INCOMING! 👗"
                """;

        PromptTemplate promptTemplate = new PromptTemplate(template);
        Map<String, Object> params = Map.of("personality", personality);
        
        Prompt prompt = promptTemplate.create(params);

        try {
            return chatModel.call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            return getFallbackOutfitResponse();
        }
    }

    public String handleTaggedMessage(String message, String userIp) {
        String personality = aiPersonalities[random.nextInt(aiPersonalities.length)];
        
        String[] funGreetings = {
            "🤖 Your AI Fashion Oracle is here!",
            "💫 Digital Style Wizard activated!",
            "✨ Fashion AI reporting for fabulous duty!",
            "🎭 Your virtual style bestie checking in!",
            "🦄 AI fashionista ready to serve looks and advice!"
        };

        String template = """
                You are a {personality} responding to this message: "{message}"
                
                Guidelines:
                - If they're asking about fashion/style: Give specific, helpful advice
                - If they're asking about colors/patterns: Be specific about combinations  
                - If they mention specific clothing items: Give targeted styling tips
                - If they're asking about occasions: Suggest appropriate outfit ideas
                - If they're just chatting: Be fun and engaging
                - Always maintain the fun chatroom energy
                - Use emojis but don't overdo it
                - Keep responses under 150 words
                - End with a question to keep conversation flowing
                
                Be authentic to your personality while being helpful!
                """;

        String greeting = funGreetings[random.nextInt(funGreetings.length)];
        
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Map<String, Object> params = Map.of(
            "personality", personality,
            "message", message
        );
        
        Prompt chatPrompt = promptTemplate.create(params);
        
        try {
            String aiResponse = chatModel.call(chatPrompt).getResult().getOutput().getText();
            return greeting + "\n\n" + aiResponse;
        } catch (Exception e) {
            return greeting + "\n\nOops! My fashion circuits are buzzing! Try asking me again! ✨💫";
        }
    }

    public String getWelcomeMessage() {
        String[] welcomeMessages = {
            "👋 Hey style mavens! I'm your AI fashion buddy ready to rate fits, give styling tips, and chat about all things fashion! Tag me with @AI anytime! 💅✨",
            "🎉 Welcome to the chicest corner of the internet! Upload your outfits for honest (but loving) AI ratings, or just tag me for fashion chat! 🔥👗",
            "💃 Ready to serve some serious style realness? I'm here to help you look fabulous! Drop those outfit pics or tag me for advice! ✨💎",
            "🌟 Your personal AI stylist has entered the chat! Let's make every outfit a moment, darling! Tag @AI when you need me! 💫👠"
        };
        
        return welcomeMessages[random.nextInt(welcomeMessages.length)];
    }

    public String getRandomFashionTip() {
        String[] tips = {
            "💡 Pro tip: The rule of thirds works in fashion too! Balance your proportions for a flattering silhouette.",
            "✨ Color coordination hack: Use the 60-30-10 rule - 60% dominant color, 30% secondary, 10% accent!",
            "👗 Fit is everything! A well-fitted basic piece always beats an ill-fitting designer item.",
            "💅 Confidence is your best accessory - wear it with everything!",
            "🎨 When in doubt, add texture! Mix materials like silk, denim, and knits for visual interest.",
            "👠 Invest in good basics: quality white tee, perfect jeans, classic blazer, and comfortable shoes.",
            "💎 The power of accessories: they can transform any basic outfit into something special!"
        };
        
        return tips[random.nextInt(tips.length)];
    }

    private String getFallbackOutfitResponse() {
        String[] fallbackResponses = {
            "🔥 OUTFIT SPOTTED! 🔥\n\nOkay bestie, I can sense the style energy from here! While I'm getting my digital eyes calibrated ☕, I can tell you've got that fashion confidence going! Here's what I always say: fit is everything, and confidence is your best accessory!\n\n✨ Quick tip: Tell me about your favorite piece in this look and I'll give you styling ideas! What's the vibe you're going for? 💅",
            
            "💅 STYLE ALERT! 💅\n\nI'm picking up major fashion vibes! Even though my outfit scanner is having a moment 🤖💫, I KNOW you're bringing the looks! Remember: personal style is about expressing the real YOU.\n\n🎯 Pro tip: The best outfit is one that makes you feel unstoppable! What inspired this look? Drop some details and let's chat styling! ✨",
            
            "👗 FASHION MOMENT! 👗\n\nThe style radar is going off! While my fashion computer is rebooting 😅, I'm here with the eternal truth: great style is about confidence and having fun with fashion!\n\n💡 Style secret: When you love what you're wearing, it shows! Tell me what you love most about this outfit and I'll help you style it even better! 🔥"
        };
        
        return fallbackResponses[random.nextInt(fallbackResponses.length)];
    }

    public String getSeasonalSuggestion() {
        int month = java.time.LocalDate.now().getMonthValue();
        
        if (month >= 3 && month <= 5) { 
            return "🌸 Spring vibes: Time for pastels, light layers, and flowy fabrics! Think cardigans over sundresses and cute sneakers! 🌿";
        } else if (month >= 6 && month <= 8) { 
            return "☀️ Summer energy: Embrace linen, bright colors, and breezy silhouettes! Don't forget your statement sunglasses! 😎";
        } else if (month >= 9 && month <= 11) { 
            return "🍂 Autumn aesthetic: Cozy knits, warm earth tones, and perfect layering weather! Boots and scarves season! 🧣";
        } else { // Winter
            return "❄️ Winter warmth: Chic coats, rich textures, and statement jewelry to brighten dark days! Layer like a pro! 🧥";
        }
    }
}