package com.example.catalog.config;

import com.example.catalog.assistant.AssistantTools;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantConfig {

    @Bean
    public ChatClient assistantChatClient(AnthropicChatModel chatModel, AssistantTools assistantTools) {
        return ChatClient.builder(chatModel)
            .defaultSystem("""
                You are an inventory assistant for an e-commerce catalog.
                Use the available tools to answer questions about stock status
                and reorder costs. Never guess a status, quantity, or cost —
                always call a tool. If asked about something the tools don't
                cover, such as a restock date, say plainly that this
                information isn't available rather than estimating one.
                """)
            .defaultTools(assistantTools)
            .build();
    }
}
