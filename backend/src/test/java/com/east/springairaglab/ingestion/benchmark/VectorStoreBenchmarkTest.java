package com.east.springairaglab.ingestion.benchmark;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Benchmark tests for HNSW index performance.
 * These tests measure search latency and recall quality.
 * 
 * Run with: ./gradlew test --tests "VectorStoreBenchmarkTest"
 */
@SpringBootTest
@Disabled("Manual benchmark - enable when needed")
class VectorStoreBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreBenchmarkTest.class);

    @Autowired
    private VectorStore vectorStore;

    @Test
    void benchmarkSearchLatency() {
        // Given: Sample documents
        List<Document> testDocs = createTestDocuments(1000);
        vectorStore.add(testDocs);

        // When: Perform multiple searches
        int iterations = 100;
        long totalTime = 0;

        for (int i = 0; i < iterations; i++) {
            String query = "test query " + i;

            long startTime = System.nanoTime();
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.query(query).withTopK(10));
            long endTime = System.nanoTime();

            totalTime += (endTime - startTime);

            assertThat(results).isNotEmpty();
        }

        // Then: Calculate average latency
        double avgLatencyMs = (totalTime / iterations) / 1_000_000.0;
        log.info("Average search latency: {:.2f} ms", avgLatencyMs);

        // Assert: Should be under 50ms for 1,000 vectors
        assertThat(avgLatencyMs).isLessThan(50.0);
    }

    @Test
    void benchmarkScalability() {
        // Test search performance at different scales
        int[] scales = { 100, 1_000, 10_000 };

        for (int scale : scales) {
            // Add documents
            List<Document> docs = createTestDocuments(scale);
            vectorStore.add(docs);

            // Measure search time
            long startTime = System.nanoTime();
            vectorStore.similaritySearch(
                    SearchRequest.query("test query").withTopK(10));
            long endTime = System.nanoTime();

            double latencyMs = (endTime - startTime) / 1_000_000.0;
            log.info("Scale: {} vectors, Latency: {:.2f} ms", scale, latencyMs);

            // Cleanup for next iteration
            // Note: In real scenario, you'd use separate test databases
        }
    }

    @Test
    void benchmarkRecall() {
        // Given: Known similar documents
        List<Document> groundTruth = createSimilarDocuments();
        vectorStore.add(groundTruth);

        // When: Search for similar document
        String queryText = groundTruth.get(0).getContent();
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.query(queryText).withTopK(5));

        // Then: Calculate recall
        int relevantFound = 0;
        for (Document result : results) {
            if (groundTruth.stream().anyMatch(doc -> doc.getId().equals(result.getId()))) {
                relevantFound++;
            }
        }

        double recall = (double) relevantFound / Math.min(5, groundTruth.size());
        log.info("Recall@5: {:.2f}%", recall * 100);

        // Assert: Should have high recall
        assertThat(recall).isGreaterThan(0.8); // 80% recall
    }

    private List<Document> createTestDocuments(int count) {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            documents.add(new Document(
                    "Test document " + i + " with some content for embedding",
                    Map.of("index", i, "type", "test")));
        }
        return documents;
    }

    private List<Document> createSimilarDocuments() {
        return List.of(
                new Document("Java class UserService handles user operations",
                        Map.of("class", "UserService")),
                new Document("UserService class manages user data",
                        Map.of("class", "UserService")),
                new Document("The UserService handles authentication",
                        Map.of("class", "UserService")),
                new Document("UserService provides user CRUD operations",
                        Map.of("class", "UserService")),
                new Document("Service layer UserService for user management",
                        Map.of("class", "UserService")));
    }
}
