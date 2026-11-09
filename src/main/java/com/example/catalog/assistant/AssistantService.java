package com.example.catalog.assistant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Replaces REACTIVE's AssistantHandler. ChatClient.call() already blocks, so
 * it runs on the request thread; no boundedElastic hop is needed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantService {

    private final ChatClient assistantChatClient;

    public String chat(String prompt, String authorizationHeader) {
        log.info("Assistant chat request received");
        try {
            String reply = assistantChatClient.prompt(prompt)
                .toolContext(Map.of(AssistantTools.AUTHORIZATION_HEADER_KEY, authorizationHeader))
                .call()
                .content();
            log.info("Assistant reply length: {}", reply == null ? "null" : reply.length());
            return reply;
        } catch (RuntimeException e) {
            log.error("Assistant call failed", e);
            throw new AssistantUnavailableException(e.getMessage(), e);
        }
    }
}
