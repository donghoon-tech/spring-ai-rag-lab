package com.east.springairaglab.ingestion.service;

import com.east.springairaglab.ingestion.factory.SplitterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    private IngestionService ingestionService;
    private SplitterFactory splitterFactory;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        splitterFactory = new SplitterFactory();
        ingestionService = new IngestionService(vectorStore, splitterFactory);
        tempDir = Files.createTempDirectory("spring-ai-rag-lab-test");
    }

    @Test
    void ingest_ShouldProcessSupportedFiles() throws IOException {
        // Given
        Path markdownFile = tempDir.resolve("test.md");
        Files.writeString(markdownFile, "# Test Content");

        // When
        int count = ingestionService.ingest(tempDir);

        // Then
        assertThat(count).isGreaterThan(0);
        verify(vectorStore).add(anyList());
    }

    @Test
    void ingest_ShouldIgnoreUnsupportedFiles() throws IOException {
        // Given
        Path unsupportedFile = tempDir.resolve("test.unsupported");
        Files.writeString(unsupportedFile, "Ignored Content");

        // When
        int count = ingestionService.ingest(tempDir);

        // Then
        assertThat(count).isEqualTo(0);
    }
}
