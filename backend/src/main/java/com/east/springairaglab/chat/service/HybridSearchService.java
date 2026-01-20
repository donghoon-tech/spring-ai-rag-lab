package com.east.springairaglab.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid search combining semantic (vector) and keyword (BM25) search
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final VectorStore vectorStore;
    private final KeywordSearchService keywordSearchService;

    @Value("${spring.ai.rag.hybrid.alpha:0.7}")
    private double alpha; // Weight for semantic search (1-alpha for keyword)

    /**
     * Perform hybrid search combining semantic and keyword results
     * 
     * @param query               Search query
     * @param topK                Number of results to return
     * @param similarityThreshold Minimum similarity for vector search
     * @return Combined and re-ranked documents
     */
    public List<Document> search(String query, int topK, double similarityThreshold) {
        log.info("Performing hybrid search: query='{}', topK={}, alpha={}", query, topK, alpha);

        // 1. Semantic search (vector similarity)
        List<Document> semanticResults = performSemanticSearch(query, topK * 2, similarityThreshold);

        // 2. Keyword search (BM25)
        List<KeywordSearchService.DocumentWithScore> keywordResults = keywordSearchService.search(query, topK * 2);

        // 3. Combine and re-rank
        Map<String, ScoredDocument> combinedScores = new HashMap<>();

        // Normalize and add semantic scores
        double maxSemanticScore = semanticResults.isEmpty() ? 1.0 : 1.0;
        for (int i = 0; i < semanticResults.size(); i++) {
            Document doc = semanticResults.get(i);
            String docId = getDocumentId(doc);
            double normalizedScore = 1.0 - (i / (double) semanticResults.size());

            combinedScores.put(docId, new ScoredDocument(
                    doc,
                    alpha * normalizedScore,
                    normalizedScore,
                    0.0));
        }

        // Normalize and add keyword scores
        double maxKeywordScore = keywordResults.isEmpty() ? 1.0
                : keywordResults.stream().mapToDouble(KeywordSearchService.DocumentWithScore::score).max().orElse(1.0);

        for (KeywordSearchService.DocumentWithScore result : keywordResults) {
            String docId = getDocumentId(result.document());
            double normalizedScore = result.score() / maxKeywordScore;
            double keywordWeight = (1 - alpha) * normalizedScore;

            combinedScores.compute(docId, (id, existing) -> {
                if (existing != null) {
                    return new ScoredDocument(
                            existing.document,
                            existing.combinedScore + keywordWeight,
                            existing.semanticScore,
                            normalizedScore);
                } else {
                    return new ScoredDocument(
                            result.document(),
                            keywordWeight,
                            0.0,
                            normalizedScore);
                }
            });
        }

        // 4. Sort by combined score and return top-k
        List<Document> results = combinedScores.values().stream()
                .sorted(Comparator.comparingDouble(ScoredDocument::combinedScore).reversed())
                .limit(topK)
                .map(scored -> {
                    // Add score metadata
                    scored.document.getMetadata().put("hybrid_score", scored.combinedScore);
                    scored.document.getMetadata().put("semantic_score", scored.semanticScore);
                    scored.document.getMetadata().put("keyword_score", scored.keywordScore);
                    return scored.document;
                })
                .collect(Collectors.toList());

        log.info("Hybrid search returned {} results (semantic: {}, keyword: {})",
                results.size(), semanticResults.size(), keywordResults.size());

        return results;
    }

    /**
     * Perform semantic search using vector store
     */
    private List<Document> performSemanticSearch(String query, int topK, double threshold) {
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.query(query)
                            .withTopK(topK)
                            .withSimilarityThreshold(threshold));
        } catch (Exception e) {
            log.error("Error in semantic search", e);
            return List.of();
        }
    }

    /**
     * Get unique document identifier
     */
    private String getDocumentId(Document doc) {
        // Use source + content hash as unique ID
        String source = doc.getMetadata().getOrDefault("source", "unknown").toString();
        String contentHash = String.valueOf(doc.getContent().hashCode());
        return source + "_" + contentHash;
    }

    /**
     * Document with combined scores
     */
    private record ScoredDocument(
            Document document,
            double combinedScore,
            double semanticScore,
            double keywordScore) {
    }
}
