package com.east.springairaglab.chat.service;

import com.east.springairaglab.chat.dto.ChatRequest;
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
 * with metadata filtering support
 * 
 * Algorithm:
 * 1. Retrieve results from both semantic and keyword search
 * 2. Apply metadata filters
 * 3. Normalize scores using consistent strategy
 * 4. Merge results with weighted combination (alpha * semantic + (1-alpha) *
 * keyword)
 * 5. Rank by combined score and return top-k
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final VectorStore vectorStore;
    private final KeywordSearchService keywordSearchService;

    @Value("${spring.ai.rag.hybrid.alpha:0.7}")
    private double alpha; // Weight for semantic search (1-alpha for keyword)

    @Value("${spring.ai.rag.hybrid.retrieval-multiplier:2}")
    private int retrievalMultiplier; // Retrieve topK * multiplier for better recall

    /**
     * Perform hybrid search combining semantic and keyword results
     */
    public List<Document> search(String query, int topK, double similarityThreshold,
            ChatRequest.MetadataFilter filters) {
        log.info("Hybrid search: query='{}', topK={}, alpha={}, filters={}",
                query, topK, alpha, filters != null);

        // 1. Retrieve from both sources
        SearchResults rawResults = retrieveDocuments(query, topK, similarityThreshold);

        // 2. Apply filters
        SearchResults filteredResults = applyFilters(rawResults, filters);

        // 3. Normalize and merge scores
        Map<String, ScoredDocument> mergedScores = mergeAndScoreResults(filteredResults);

        // 4. Rank and limit
        List<Document> finalResults = rankAndLimit(mergedScores, topK);

        log.info("Hybrid search completed: {} results (semantic: {}, keyword: {})",
                finalResults.size(), rawResults.semantic.size(), rawResults.keyword.size());

        return finalResults;
    }

    /**
     * Retrieve documents from both semantic and keyword search
     */
    private SearchResults retrieveDocuments(String query, int topK, double threshold) {
        int retrievalSize = topK * retrievalMultiplier;

        List<Document> semanticDocs = performSemanticSearch(query, retrievalSize, threshold);
        List<KeywordSearchService.DocumentWithScore> keywordDocs = performKeywordSearch(query, retrievalSize);

        return new SearchResults(semanticDocs, keywordDocs);
    }

    /**
     * Perform semantic search with error handling
     */
    private List<Document> performSemanticSearch(String query, int topK, double threshold) {
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.query(query)
                            .withTopK(topK)
                            .withSimilarityThreshold(threshold));
        } catch (Exception e) {
            log.error("Semantic search failed for query: {}", query, e);
            return List.of();
        }
    }

    /**
     * Perform keyword search with error handling
     */
    private List<KeywordSearchService.DocumentWithScore> performKeywordSearch(String query, int topK) {
        try {
            return keywordSearchService.search(query, topK);
        } catch (Exception e) {
            log.error("Keyword search failed for query: {}", query, e);
            return List.of();
        }
    }

    /**
     * Apply metadata filters to search results
     */
    private SearchResults applyFilters(SearchResults results, ChatRequest.MetadataFilter filters) {
        if (filters == null) {
            return results;
        }

        List<Document> filteredSemantic = results.semantic.stream()
                .filter(doc -> matchesFilter(doc, filters))
                .collect(Collectors.toList());

        List<KeywordSearchService.DocumentWithScore> filteredKeyword = results.keyword.stream()
                .filter(result -> matchesFilter(result.document(), filters))
                .collect(Collectors.toList());

        return new SearchResults(filteredSemantic, filteredKeyword);
    }

    /**
     * Check if document matches all filter criteria
     */
    private boolean matchesFilter(Document doc, ChatRequest.MetadataFilter filters) {
        Map<String, Object> metadata = doc.getMetadata();

        return matchesFileType(metadata, filters.getFileType())
                && matchesSourcePath(metadata, filters.getSourcePath())
                && matchesClassName(metadata, filters.getClassName())
                && matchesMethodName(metadata, filters.getMethodName())
                && matchesFilename(metadata, filters.getFilename());
    }

    private boolean matchesFileType(Map<String, Object> metadata, String fileType) {
        if (fileType == null || fileType.isBlank())
            return true;
        String actual = metadata.getOrDefault("file_type", "").toString();
        return actual.equalsIgnoreCase(fileType);
    }

    private boolean matchesSourcePath(Map<String, Object> metadata, String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank())
            return true;
        String actual = metadata.getOrDefault("source", "").toString();
        return actual.contains(sourcePath);
    }

    private boolean matchesClassName(Map<String, Object> metadata, String className) {
        if (className == null || className.isBlank())
            return true;
        String actual = metadata.getOrDefault("class_name", "").toString();
        return actual.equalsIgnoreCase(className);
    }

    private boolean matchesMethodName(Map<String, Object> metadata, String methodName) {
        if (methodName == null || methodName.isBlank())
            return true;
        String actual = metadata.getOrDefault("method_name", "").toString();
        return actual.equalsIgnoreCase(methodName);
    }

    private boolean matchesFilename(Map<String, Object> metadata, String filename) {
        if (filename == null || filename.isBlank())
            return true;
        String actual = metadata.getOrDefault("filename", "").toString();
        return actual.contains(filename);
    }

    /**
     * Merge and score results from both searches
     */
    private Map<String, ScoredDocument> mergeAndScoreResults(SearchResults results) {
        Map<String, ScoredDocument> combinedScores = new HashMap<>();

        // Add semantic scores
        addSemanticScores(combinedScores, results.semantic);

        // Add keyword scores
        addKeywordScores(combinedScores, results.keyword);

        return combinedScores;
    }

    /**
     * Add normalized semantic scores to combined map
     */
    private void addSemanticScores(Map<String, ScoredDocument> combinedScores, List<Document> semanticDocs) {
        for (int i = 0; i < semanticDocs.size(); i++) {
            Document doc = semanticDocs.get(i);
            String docId = getDocumentId(doc);

            // Rank-based normalization: higher rank = higher score
            double normalizedScore = 1.0 - (i / (double) Math.max(semanticDocs.size(), 1));
            double weightedScore = alpha * normalizedScore;

            combinedScores.put(docId, new ScoredDocument(
                    doc,
                    weightedScore,
                    normalizedScore,
                    0.0));
        }
    }

    /**
     * Add normalized keyword scores to combined map
     */
    private void addKeywordScores(Map<String, ScoredDocument> combinedScores,
            List<KeywordSearchService.DocumentWithScore> keywordDocs) {
        if (keywordDocs.isEmpty()) {
            return;
        }

        // Find max score for normalization
        double maxScore = keywordDocs.stream()
                .mapToDouble(KeywordSearchService.DocumentWithScore::score)
                .max()
                .orElse(1.0);

        for (KeywordSearchService.DocumentWithScore result : keywordDocs) {
            String docId = getDocumentId(result.document());
            double normalizedScore = result.score() / maxScore;
            double weightedScore = (1 - alpha) * normalizedScore;

            combinedScores.compute(docId, (id, existing) -> {
                if (existing != null) {
                    // Document found in both searches - combine scores
                    return new ScoredDocument(
                            existing.document,
                            existing.combinedScore + weightedScore,
                            existing.semanticScore,
                            normalizedScore);
                } else {
                    // Document only in keyword search
                    return new ScoredDocument(
                            result.document(),
                            weightedScore,
                            0.0,
                            normalizedScore);
                }
            });
        }
    }

    /**
     * Rank by combined score and return top-k with metadata
     */
    private List<Document> rankAndLimit(Map<String, ScoredDocument> scoredDocs, int topK) {
        return scoredDocs.values().stream()
                .sorted(Comparator.comparingDouble(ScoredDocument::combinedScore).reversed())
                .limit(topK)
                .map(this::enrichWithScoreMetadata)
                .collect(Collectors.toList());
    }

    /**
     * Add score metadata to document
     */
    private Document enrichWithScoreMetadata(ScoredDocument scored) {
        scored.document.getMetadata().put("hybrid_score", scored.combinedScore);
        scored.document.getMetadata().put("semantic_score", scored.semanticScore);
        scored.document.getMetadata().put("keyword_score", scored.keywordScore);
        return scored.document;
    }

    /**
     * Generate unique document identifier
     */
    private String getDocumentId(Document doc) {
        String source = doc.getMetadata().getOrDefault("source", "unknown").toString();
        String contentHash = String.valueOf(doc.getContent().hashCode());
        return source + "_" + contentHash;
    }

    /**
     * Container for search results from both sources
     */
    private record SearchResults(
            List<Document> semantic,
            List<KeywordSearchService.DocumentWithScore> keyword) {
    }

    /**
     * Document with combined scores from both searches
     */
    private record ScoredDocument(
            Document document,
            double combinedScore,
            double semanticScore,
            double keywordScore) {
    }
}
