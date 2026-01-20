package com.east.springairaglab.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for chat queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * User's question
     */
    private String query;

    /**
     * Number of similar documents to retrieve (default: 5)
     */
    private Integer topK = 5;

    /**
     * Minimum similarity threshold (0.0 - 1.0)
     */
    private Double similarityThreshold = 0.7;
}
