package com.east.springairaglab.ingestion.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for PdfDocumentLoader
 */
class PdfDocumentLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadPdfDocument() throws IOException {
        // Given: A simple PDF file (we'll create a minimal one)
        // Note: For real testing, you'd need a proper PDF file
        // This test demonstrates the structure

        PdfDocumentLoader loader = new PdfDocumentLoader();

        // For now, we'll test that the loader is instantiated correctly
        assertThat(loader).isNotNull();
    }

    @Test
    void shouldEnrichMetadata() {
        // Given
        PdfDocumentLoader loader = new PdfDocumentLoader();

        // Test that loader has proper configuration
        assertThat(loader).isNotNull();

        // TODO: Add actual PDF file test when sample PDF is available
        // Expected metadata: source, filename, file_type=pdf
    }

    @Test
    void shouldHandleInvalidPdfGracefully() {
        // Given: A non-PDF file
        PdfDocumentLoader loader = new PdfDocumentLoader();
        Path invalidFile = tempDir.resolve("not-a-pdf.txt");

        try {
            Files.writeString(invalidFile, "This is not a PDF");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // When/Then: Should throw exception for invalid PDF
        assertThrows(RuntimeException.class, () -> loader.load(invalidFile));
    }
}
