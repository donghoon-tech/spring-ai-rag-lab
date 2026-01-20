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
     * @param filters             Metadata filters (optional)
     * @return Combined and re-ranked documents
     */
    public List<Document> search(String query, int topK, double similarityThreshold,
            ChatRequest.MetadataFilter filters) {
        log.info("Performing hybrid search: query='{}', topK={}, alpha={}, filters={}",
                query, topK, alpha, filters);

        // 1. Semantic search (vector similarity)
        List<Document> semanticResults = performSemanticSearch(query, topK * 2, similarityThreshold);

        // 2. Keyword search (BM25)
        List<KeywordSearchService.DocumentWithScore> keywordResults = keywordSearchService.search(query, topK * 2);

        // 3. Apply metadata filters if provided
        if (filters != null) {
            semanticResults = applyFilters(semanticResults, filters);
            keywordResults = keywordResults.stream()
                    .filter(result -> matchesFilter(result.document(), filters))
                    .collect(Collectors.toList());
        }

        // 4. Combine and re-rank
        Map<String, ScoredDocument> combinedScores = new HashMap<>();

        // Normalize and add semantic scores
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

        // 5. Sort by combined score and return top-k
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

        log.info("Hybrid search returned {} results (semantic: {}, keyword: {}, filtered: {})",
                results.size(), semanticResults.size(), keywordResults.size(), filters != null);

        return results;
    }

    /**
     * Apply metadata filters to document list
     */
    private List<Document> applyFilters(List<Document> documents, ChatRequest.MetadataFilter filters) {
        return documents.stream()
                .filter(doc -> matchesFilter(doc, filters))
                .collect(Collectors.toList());
    }

    /**
     * Check if document matches filter criteria
     */
    private boolean matchesFilter(Document doc, ChatRequest.MetadataFilter filters) {
        Map<String, Object> metadata = doc.getMetadata();

        // Filter by file type
        if (filters.getFileType() != null && !filters.getFileType().isBlank()) {
            String fileType = metadata.getOrDefault("file_type", "").toString();
            if (!fileType.equalsIgnoreCase(filters.getFileType())) {
                return false;
            }
        }

        // Filter by source path (supports wildcards)
        if (filters.getSourcePath() != null && !filters.getSourcePath().isBlank()) {
            String source = metadata.getOrDefault("source", "").toString();
            if (!source.contains(filters.getSourcePath())) {
                return false;
            }
        }

        // Filter by class name
        if (filters.getClassName() != null && !filters.getClassName().isBlank()) {
            String className = metadata.getOrDefault("class_name", "").toString();
            if (!className.equalsIgnoreCase(filters.getClassName())) {
                return false;
            }
        }

        // Filter by method name
        if (filters.getMethodName() != null && !filters.getMethodName().isBlank()) {
            String methodName = metadata.getOrDefault("method_name", "").toString();
            if (!methodName.equalsIgnoreCase(filters.getMethodName())) {
                return false;
            }
        }

        // Filter by filename
        if (filters.getFilename() != null && !filters.getFilename().isBlank()) {
            String filename = metadata.getOrDefault("filename", "").toString();
            if (!filename.contains(filters.getFilename())) {
                return false;
            }
        }

        return true;
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
