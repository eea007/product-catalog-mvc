package com.example.catalog.controller;

import com.example.catalog.assistant.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Same contract as REACTIVE: the request body is the prompt as plain text, and
 * the reply is written as a raw string labeled application/json.
 */
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public String chat(@RequestBody String prompt,
                       @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return assistantService.chat(prompt, authorization);
    }
}
