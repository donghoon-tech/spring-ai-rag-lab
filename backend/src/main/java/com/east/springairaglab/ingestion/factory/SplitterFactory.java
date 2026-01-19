package com.east.springairaglab.ingestion.factory;

import com.east.springairaglab.ingestion.splitter.JavaCodeSplitter;
import com.east.springairaglab.ingestion.splitter.MarkdownCodeSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Factory for creating appropriate text splitters based on file type.
 * This enables code-aware chunking for different file formats.
 */
@Slf4j
@Component
public class SplitterFactory {

    /**
     * Get the appropriate splitter for the given file path
     */
    public TextSplitter getSplitter(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".java")) {
            log.debug("Using JavaCodeSplitter for: {}", fileName);
            return new JavaCodeSplitter();
        } else if (fileName.endsWith(".md")) {
            log.debug("Using MarkdownCodeSplitter for: {}", fileName);
            return new MarkdownCodeSplitter();
        } else {
            // Default: Token-based splitting for other file types
            log.debug("Using TokenTextSplitter for: {}", fileName);
            return new TokenTextSplitter();
        }
    }

    /**
     * Check if the file type is supported for ingestion
     */
    public boolean isSupported(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        return fileName.endsWith(".java") ||
                fileName.endsWith(".md") ||
                fileName.endsWith(".txt") ||
                fileName.endsWith(".gradle") ||
                fileName.endsWith(".properties") ||
                fileName.endsWith(".yaml") ||
                fileName.endsWith(".yml") ||
                fileName.endsWith(".pdf");
    }
}
