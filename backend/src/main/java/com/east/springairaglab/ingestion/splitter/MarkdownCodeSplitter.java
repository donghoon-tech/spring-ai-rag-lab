package com.east.springairaglab.ingestion.splitter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown-aware text splitter that preserves document structure.
 * Splits by headers while maintaining hierarchy and context.
 */
@Slf4j
public class MarkdownCodeSplitter extends TextSplitter {

    private static final int DEFAULT_MAX_CHUNK_SIZE = 1000; // tokens
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    private final int maxChunkSize;

    public MarkdownCodeSplitter() {
        this(DEFAULT_MAX_CHUNK_SIZE);
    }

    public MarkdownCodeSplitter(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    @Override
    public List<Document> apply(List<Document> documents) {
        List<Document> allChunks = new ArrayList<>();
        for (Document doc : documents) {
            allChunks.addAll(doSplit(doc));
        }
        return allChunks;
    }

    @Override
    protected List<String> splitText(String text) {
        // This method is required by TextSplitter but we override apply() instead
        List<String> result = new ArrayList<>();
        result.add(text);
        return result;
    }

    private List<Document> doSplit(Document document) {
        String content = document.getText();
        String source = document.getMetadata().getOrDefault("source", "unknown").toString();

        log.debug("Splitting Markdown file: {} (length: {} chars)", source, content.length());

        List<Document> chunks = new ArrayList<>();
        List<MarkdownSection> sections = extractSections(content);

        // Group sections into chunks
        List<String> chunkedContent = groupSections(sections);

        for (int i = 0; i < chunkedContent.size(); i++) {
            Document chunk = new Document(chunkedContent.get(i));
            chunk.getMetadata().putAll(document.getMetadata());
            chunk.getMetadata().put("chunk_index", i);
            chunk.getMetadata().put("total_chunks", chunkedContent.size());
            chunk.getMetadata().put("chunk_type", "markdown");

            chunks.add(chunk);
        }

        log.info("Split Markdown file {} into {} semantic chunks", source, chunks.size());
        return chunks;
    }

    private List<MarkdownSection> extractSections(String content) {
        List<MarkdownSection> sections = new ArrayList<>();
        String[] lines = content.split("\n");
        StringBuilder currentContent = new StringBuilder();
        String currentHeader = null;
        int currentLevel = 0;

        for (String line : lines) {
            Matcher matcher = HEADER_PATTERN.matcher(line);

            if (matcher.find()) {
                // Save previous section
                if (currentContent.length() > 0) {
                    sections.add(new MarkdownSection(currentHeader, currentLevel, currentContent.toString()));
                    currentContent = new StringBuilder();
                }

                currentLevel = matcher.group(1).length();
                currentHeader = matcher.group(2);
                currentContent.append(line).append("\n");
            } else {
                currentContent.append(line).append("\n");
            }
        }

        // Add last section
        if (currentContent.length() > 0) {
            sections.add(new MarkdownSection(currentHeader, currentLevel, currentContent.toString()));
        }

        return sections;
    }

    private List<String> groupSections(List<MarkdownSection> sections) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;

        for (MarkdownSection section : sections) {
            int sectionSize = estimateTokens(section.content);

            if (currentSize + sectionSize > maxChunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
                currentSize = 0;
            }

            currentChunk.append(section.content);
            currentSize += sectionSize;
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks.isEmpty() ? List.of("") : chunks;
    }

    private int estimateTokens(String text) {
        return text.length() / 4;
    }

    private static class MarkdownSection {
        String content;

        MarkdownSection(String header, int level, String content) {
            this.content = content;
        }
    }
}
