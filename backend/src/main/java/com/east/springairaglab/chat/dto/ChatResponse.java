package com.east.springairaglab.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for chat queries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * Generated answer from LLM
     */
    private String answer;

    /**
     * Source documents used for context
     */
    private List<SourceDocument> sources;

    /**
     * Metadata about the response
     */
    private ResponseMetadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceDocument {
        /**
         * Citation number (e.g., [1], [2])
         */
        private Integer citationNumber;

        /**
         * File path
         */
        private String source;

        /**
         * File name
         */
        private String filename;

        /**
         * Content snippet
         */
        private String content;

        /**
         * Similarity score
         */
        private Double score;

        /**
         * Additional metadata (class_name, method_name, etc.)
         */
        private String metadata;

        /**
         * Line range in source file (e.g., "45-67")
         */
        private String lineRange;

        /**
         * Class name (for Java files)
         */
        private String className;

        /**
         * Method name (for Java files)
         */
        private String methodName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseMetadata {
        /**
         * Number of documents retrieved
         */
        private Integer documentsRetrieved;

        /**
         * Total processing time in milliseconds
         */
        private Long processingTimeMs;

        /**
         * Model used for generation
         */
        private String model;
    }
}
