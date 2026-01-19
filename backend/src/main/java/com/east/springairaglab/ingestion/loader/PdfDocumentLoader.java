package com.east.springairaglab.ingestion.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Path;
import java.util.List;

/**
 * PDF document loader using Spring AI's PagePdfDocumentReader.
 * Loads PDF files and converts them into Document objects for RAG processing.
 */
@Slf4j
public class PdfDocumentLoader {

    private final PdfDocumentReaderConfig config;

    public PdfDocumentLoader() {
        this.config = PdfDocumentReaderConfig.builder()
                .withPageTopMargin(0)
                .withPageBottomMargin(0)
                .withPageExtractedTextFormatter(
                        PdfDocumentReaderConfig.builder().build().pagesPerDocument == 1
                                ? PdfDocumentReaderConfig.builder().build().pageExtractedTextFormatter
                                : null)
                .withPagesPerDocument(1) // One page per document for better chunking
                .build();
    }

    /**
     * Load a PDF file and convert it to a list of Document objects.
     *
     * @param path Path to the PDF file
     * @return List of Document objects, one per page
     */
    public List<Document> load(Path path) {
        log.debug("Loading PDF file: {}", path);

        try {
            FileSystemResource resource = new FileSystemResource(path.toFile());
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource, config);

            List<Document> documents = reader.get();

            // Enrich metadata
            documents.forEach(doc -> {
                doc.getMetadata().put("source", path.toString());
                doc.getMetadata().put("filename", path.getFileName().toString());
                doc.getMetadata().put("file_type", "pdf");
            });

            log.info("Loaded {} pages from PDF: {}", documents.size(), path.getFileName());
            return documents;

        } catch (Exception e) {
            log.error("Failed to load PDF file: {}", path, e);
            throw new RuntimeException("PDF loading failed: " + path, e);
        }
    }
}
