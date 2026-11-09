package com.example.catalog.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Same approach as REACTIVE: defining AnthropicApi and AnthropicChatModel here
 * makes Spring AI's autoconfigured beans back off, so none of the
 * spring.ai.anthropic.chat.options defaults are applied. Only model,
 * maxTokens and thinking are set, so requests carry no temperature, top_p
 * or top_k.
 */
@Configuration
public class AnthropicModelConfig {

    @Bean
    public AnthropicApi anthropicApi(@Value("${anthropic-api-key}") String apiKey) {
        return AnthropicApi.builder()
            .apiKey(apiKey)
            .build();
    }

    @Bean
    public AnthropicChatModel anthropicChatModel(AnthropicApi anthropicApi,
                                                 @Value("${assistant.model:claude-sonnet-5}") String model) {
        return AnthropicChatModel.builder()
            .anthropicApi(anthropicApi)
            .defaultOptions(AnthropicChatOptions.builder()
                .model(model)
                .maxTokens(2048)
                .thinking(AnthropicApi.ThinkingType.DISABLED, null)
                .build())
            .build();
    }
}
