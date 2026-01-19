package com.east.springairaglab.ingestion.service;

import com.east.springairaglab.ingestion.factory.SplitterFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service for ingesting documents into the vector store.
 * Uses code-aware chunking strategies based on file type.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final VectorStore vectorStore;
    private final SplitterFactory splitterFactory;

    /**
     * Ingest all supported documents from the given path (recursively).
     * 
     * @param startPath Directory or file path to ingest
     * @return Number of chunks stored in vector store
     */
    public int ingest(Path startPath) {
        List<Document> allChunks = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(startPath)) {
            stream.filter(p -> !Files.isDirectory(p))
                    .filter(splitterFactory::isSupported)
                    .forEach(path -> {
                        log.debug("Processing file: {}", path);
                        try {
                            List<Document> chunks = processFile(path);
                            allChunks.addAll(chunks);
                            log.info("Processed {}: {} chunks created", path.getFileName(), chunks.size());
                        } catch (Exception e) {
                            log.error("Failed to process file: {}", path, e);
                            // Continue processing other files
                        }
                    });
        } catch (IOException e) {
            log.error("Error walking directory: {}", startPath, e);
            throw new RuntimeException("Failed to ingest documents", e);
        }

        if (!allChunks.isEmpty()) {
            log.info("Storing {} total chunks to VectorStore...", allChunks.size());
            vectorStore.add(allChunks);
            return allChunks.size();
        }

        log.warn("No documents found to ingest from: {}", startPath);
        return 0;
    }

    /**
     * Process a single file: load and split using appropriate strategy
     */
    private List<Document> processFile(Path path) {
        // Load document
        List<Document> documents = loadDocument(path);

        if (documents.isEmpty()) {
            log.warn("No content loaded from: {}", path);
            return List.of();
        }

        // Get appropriate splitter for this file type
        TextSplitter splitter = splitterFactory.getSplitter(path);

        // Split using code-aware strategy
        return splitter.apply(documents);
    }

    /**
     * Load document from file system
     */
    private List<Document> loadDocument(Path path) {
        try {
            String extension = getFileExtension(path);

            // PDF files require special handling
            if ("pdf".equalsIgnoreCase(extension)) {
                return loadPdfDocument(path);
            }

            // Text-based files use TextReader
            FileSystemResource resource = new FileSystemResource(path);
            TextReader textReader = new TextReader(resource);

            // Add rich metadata
            textReader.getCustomMetadata().put("source", path.toString());
            textReader.getCustomMetadata().put("filename", path.getFileName().toString());
            textReader.getCustomMetadata().put("file_type", extension);

            return textReader.read();
        } catch (Exception e) {
            log.error("Failed to load document: {}", path, e);
            return List.of();
        }
    }

    /**
     * Load PDF document using PdfDocumentLoader
     */
    private List<Document> loadPdfDocument(Path path) {
        com.east.springairaglab.ingestion.loader.PdfDocumentLoader pdfLoader = new com.east.springairaglab.ingestion.loader.PdfDocumentLoader();
        return pdfLoader.load(path);
    }

    private String getFileExtension(Path path) {
        String filename = path.getFileName().toString();
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "unknown";
    }
}
