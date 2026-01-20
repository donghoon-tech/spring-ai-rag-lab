package com.east.springairaglab.chat.api;

import com.east.springairaglab.chat.dto.ChatRequest;
import com.east.springairaglab.chat.dto.ChatResponse;
import com.east.springairaglab.chat.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for RAG-based chat queries
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

    /**
     * Process a chat query using RAG
     * 
     * @param request Chat request with query and optional parameters
     * @return Chat response with answer and sources
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getQuery());

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Set defaults if not provided
        if (request.getTopK() == null) {
            request.setTopK(5);
        }
        if (request.getSimilarityThreshold() == null) {
            request.setSimilarityThreshold(0.7);
        }

        try {
            ChatResponse response = ragService.chat(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Simple GET endpoint for quick testing
     * 
     * @param query User's question
     * @return Chat response
     */
    @GetMapping
    public ResponseEntity<ChatResponse> chatSimple(@RequestParam String query) {
        ChatRequest request = new ChatRequest(query, 5, 0.7);
        return chat(request);
    }
}
