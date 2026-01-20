package com.east.springairaglab.chat.service;

import com.east.springairaglab.chat.dto.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HybridSearchService
 */
@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private KeywordSearchService keywordSearchService;

    private HybridSearchService hybridSearchService;

    @BeforeEach
    void setUp() {
        hybridSearchService = new HybridSearchService(vectorStore, keywordSearchService);

        // Set default configuration values
        ReflectionTestUtils.setField(hybridSearchService, "alpha", 0.7);
        ReflectionTestUtils.setField(hybridSearchService, "retrievalMultiplier", 2);
    }

    @Test
    void search_ShouldCombineSemanticAndKeywordResults() {
        // Given
        String query = "test query";
        int topK = 3;
        double threshold = 0.7;

        List<Document> semanticDocs = List.of(
                createDocument("doc1", "Semantic content 1"),
                createDocument("doc2", "Semantic content 2"));

        List<KeywordSearchService.DocumentWithScore> keywordDocs = List.of(
                new KeywordSearchService.DocumentWithScore(
                        createDocument("doc2", "Semantic content 2"), 0.9),
                new KeywordSearchService.DocumentWithScore(
                        createDocument("doc3", "Keyword content 3"), 0.8));

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(semanticDocs);
        when(keywordSearchService.search(eq(query), anyInt()))
                .thenReturn(keywordDocs);

        // When
        List<Document> results = hybridSearchService.search(query, topK, threshold, null);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.size()).isLessThanOrEqualTo(topK);

        // Verify both searches were called
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
        verify(keywordSearchService, times(1)).search(eq(query), anyInt());

        // Verify hybrid scores were added
        results.forEach(doc -> {
            assertThat(doc.getMetadata()).containsKey("hybrid_score");
            assertThat(doc.getMetadata()).containsKey("semantic_score");
            assertThat(doc.getMetadata()).containsKey("keyword_score");
        });
    }

    @Test
    void search_ShouldApplyMetadataFilters() {
        // Given
        String query = "test query";

        List<Document> semanticDocs = List.of(
                createDocumentWithMetadata("doc1", Map.of("file_type", "java", "class_name", "UserService")),
                createDocumentWithMetadata("doc2", Map.of("file_type", "md", "class_name", "README")));

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(semanticDocs);
        when(keywordSearchService.search(anyString(), anyInt()))
                .thenReturn(List.of());

        ChatRequest.MetadataFilter filter = new ChatRequest.MetadataFilter();
        filter.setFileType("java");

        // When
        List<Document> results = hybridSearchService.search(query, 5, 0.7, filter);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMetadata().get("file_type")).isEqualTo("java");
    }

    @Test
    void search_ShouldHandleSemanticSearchFailure() {
        // Given
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Vector store error"));

        List<KeywordSearchService.DocumentWithScore> keywordDocs = List.of(
                new KeywordSearchService.DocumentWithScore(
                        createDocument("doc1", "Keyword content"), 0.9));
        when(keywordSearchService.search(anyString(), anyInt()))
                .thenReturn(keywordDocs);

        // When
        List<Document> results = hybridSearchService.search("query", 5, 0.7, null);

        // Then - should still return keyword results
        assertThat(results).isNotEmpty();
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
        verify(keywordSearchService, times(1)).search(anyString(), anyInt());
    }

    @Test
    void search_ShouldHandleKeywordSearchFailure() {
        // Given
        List<Document> semanticDocs = List.of(
                createDocument("doc1", "Semantic content"));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(semanticDocs);
        when(keywordSearchService.search(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Keyword search error"));

        // When
        List<Document> results = hybridSearchService.search("query", 5, 0.7, null);

        // Then - should still return semantic results
        assertThat(results).isNotEmpty();
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
        verify(keywordSearchService, times(1)).search(anyString(), anyInt());
    }

    @Test
    void search_ShouldRankByHybridScore() {
        // Given
        List<Document> semanticDocs = List.of(
                createDocument("doc1", "High semantic relevance"),
                createDocument("doc2", "Low semantic relevance"));

        List<KeywordSearchService.DocumentWithScore> keywordDocs = List.of(
                new KeywordSearchService.DocumentWithScore(
                        createDocument("doc2", "Low semantic relevance"), 1.0), // High keyword score
                new KeywordSearchService.DocumentWithScore(
                        createDocument("doc1", "High semantic relevance"), 0.5));

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(semanticDocs);
        when(keywordSearchService.search(anyString(), anyInt()))
                .thenReturn(keywordDocs);

        // When
        List<Document> results = hybridSearchService.search("query", 5, 0.7, null);

        // Then - results should be ranked by combined score
        assertThat(results).isNotEmpty();

        // First result should have highest hybrid score
        double firstScore = (double) results.get(0).getMetadata().get("hybrid_score");
        for (int i = 1; i < results.size(); i++) {
            double currentScore = (double) results.get(i).getMetadata().get("hybrid_score");
            assertThat(firstScore).isGreaterThanOrEqualTo(currentScore);
        }
    }

    @Test
    void search_ShouldRespectTopKLimit() {
        // Given
        int topK = 2;

        List<Document> semanticDocs = List.of(
                createDocument("doc1", "Content 1"),
                createDocument("doc2", "Content 2"),
                createDocument("doc3", "Content 3"),
                createDocument("doc4", "Content 4"));

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(semanticDocs);
        when(keywordSearchService.search(anyString(), anyInt()))
                .thenReturn(List.of());

        // When
        List<Document> results = hybridSearchService.search("query", topK, 0.7, null);

        // Then
        assertThat(results).hasSize(topK);
    }

    @Test
    void search_ShouldFilterByClassName() {
        // Given
        List<Document> semanticDocs = List.of(
                createDocumentWithMetadata("doc1", Map.of("class_name", "UserService")),
                createDocumentWithMetadata("doc2", Map.of("class_name", "OrderService")),
                createDocumentWithMetadata("doc3", Map.of("class_name", "UserService")));

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(semanticDocs);
        when(keywordSearchService.search(anyString(), anyInt()))
                .thenReturn(List.of());

        ChatRequest.MetadataFilter filter = new ChatRequest.MetadataFilter();
        filter.setClassName("UserService");

        // When
        List<Document> results = hybridSearchService.search("query", 10, 0.7, filter);

        // Then
        assertThat(results).hasSize(2);
        results.forEach(doc -> assertThat(doc.getMetadata().get("class_name")).isEqualTo("UserService"));
    }

    @Test
    void search_ShouldReturnEmptyWhenNoResults() {
        // Given
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());
        when(keywordSearchService.search(anyString(), anyInt()))
                .thenReturn(List.of());

        // When
        List<Document> results = hybridSearchService.search("query", 5, 0.7, null);

        // Then
        assertThat(results).isEmpty();
    }

    // Helper methods
    private Document createDocument(String id, String content) {
        return new Document(content, Map.of("id", id, "source", "/path/to/" + id));
    }

    private Document createDocumentWithMetadata(String id, Map<String, Object> metadata) {
        Map<String, Object> fullMetadata = new java.util.HashMap<>(metadata);
        fullMetadata.put("id", id);
        fullMetadata.put("source", "/path/to/" + id);
        return new Document("Content for " + id, fullMetadata);
    }
}
