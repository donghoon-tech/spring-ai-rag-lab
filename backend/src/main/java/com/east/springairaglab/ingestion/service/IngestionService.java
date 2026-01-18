package com.east.springairaglab.ingestion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final VectorStore vectorStore;

    public int ingest(Path startPath) {
        List<Document> documents = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(startPath)) {
            stream.filter(p -> !Files.isDirectory(p))
                    .filter(this::isSupportedExtension)
                    .forEach(path -> {
                        log.debug("Processing file: {}", path);
                        documents.addAll(loadDocument(path));
                    });
        } catch (IOException e) {
            log.error("Error walking directory: {}", startPath, e);
            throw new RuntimeException("Failed to ingest documents", e);
        }

        if (!documents.isEmpty()) {
            // Split documents into chunks
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocuments = splitter.apply(documents);

            log.info("Storing {} chunks (from {} original files) to VectorStore...", splitDocuments.size(),
                    documents.size());
            vectorStore.add(splitDocuments);
            return splitDocuments.size(); // Return chunk count
        }

        return 0;
    }

    private boolean isSupportedExtension(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(".md") || filename.endsWith(".java") || filename.endsWith(".txt")
                || filename.endsWith(".gradle");
    }

    private List<Document> loadDocument(Path path) {
        // Simple TextReader for Text-based files
        // In the future, use factory pattern for PDF etc.
        FileSystemResource resource = new FileSystemResource(path);
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("source", path.toString());
        return textReader.read();
    }
}
