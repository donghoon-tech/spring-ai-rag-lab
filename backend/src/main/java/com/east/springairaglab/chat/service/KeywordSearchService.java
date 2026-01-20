package com.east.springairaglab.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Keyword-based search service using PostgreSQL full-text search (BM25-like)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordSearchService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Search documents using full-text search
     * 
     * @param query Search query
     * @param topK  Number of results to return
     * @return List of documents with BM25 scores
     */
    public List<DocumentWithScore> search(String query, int topK) {
        String sql = """
                SELECT
                    id,
                    content,
                    metadata,
                    ts_rank_cd(content_tsv, plainto_tsquery('english', ?)) as score
                FROM vector_store
                WHERE content_tsv @@ plainto_tsquery('english', ?)
                ORDER BY score DESC
                LIMIT ?
                """;

        try {
            return jdbcTemplate.query(
                    sql,
                    new Object[] { query, query, topK },
                    (rs, rowNum) -> {
                        Document doc = new Document(
                                rs.getString("content"),
                                parseMetadata(rs.getString("metadata")));
                        doc.getMetadata().put("id", rs.getString("id"));

                        double score = rs.getDouble("score");
                        return new DocumentWithScore(doc, score);
                    });
        } catch (Exception e) {
            log.error("Error performing keyword search", e);
            return List.of();
        }
    }

    /**
     * Parse JSON metadata string to Map
     */
    private Map<String, Object> parseMetadata(String metadataJson) {
        // Simple JSON parsing - in production, use Jackson or similar
        try {
            if (metadataJson == null || metadataJson.isBlank()) {
                return Map.of();
            }
            // For now, return empty map - will be populated from actual JSON
            return Map.of();
        } catch (Exception e) {
            log.warn("Failed to parse metadata: {}", metadataJson, e);
            return Map.of();
        }
    }

    /**
     * Document with associated score
     */
    public record DocumentWithScore(Document document, double score) {
    }
}
