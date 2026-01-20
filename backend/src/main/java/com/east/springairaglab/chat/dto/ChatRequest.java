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

    /**
     * Metadata filters for scoped search
     */
    private MetadataFilter filters;

    /**
     * Constructor for backward compatibility (without filters)
     */
    public ChatRequest(String query, Integer topK, Double similarityThreshold) {
        this.query = query;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
        this.filters = null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetadataFilter {
        /**
         * Filter by file type (e.g., "java", "md", "pdf")
         */
        private String fileType;

        /**
         * Filter by source path (supports wildcards)
         */
        private String sourcePath;

        /**
         * Filter by class name (for Java files)
         */
        private String className;

        /**
         * Filter by method name (for Java files)
         */
        private String methodName;

        /**
         * Filter by filename
         */
        private String filename;
    }
}
