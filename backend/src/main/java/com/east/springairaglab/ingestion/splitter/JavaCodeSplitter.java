package com.east.springairaglab.ingestion.splitter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Code-aware text splitter that preserves Java class and method structures.
 * This splitter intelligently chunks Java source code by:
 * 1. Keeping class declarations with their methods
 * 2. Preserving method boundaries (not splitting mid-method)
 * 3. Maintaining context through metadata (class name, method name)
 * 4. Handling nested classes appropriately
 */
@Slf4j
public class JavaCodeSplitter extends TextSplitter {

    private static final int DEFAULT_MAX_CHUNK_SIZE = 1500; // tokens

    // Regex patterns for Java code structure
    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "(?:public|private|protected)?\\s*(?:static)?\\s*(?:final)?\\s*(?:abstract)?\\s*class\\s+(\\w+)",
            Pattern.MULTILINE);

    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?:public|private|protected)?\\s*(?:static)?\\s*(?:final)?\\s*(?:synchronized)?\\s*" +
                    "(?:<[^>]+>\\s*)?(?:\\w+(?:<[^>]+>)?(?:\\[\\])?\\s+)(\\w+)\\s*\\([^)]*\\)\\s*(?:throws\\s+[^{]+)?\\s*\\{",
            Pattern.MULTILINE);

    private final int maxChunkSize;

    public JavaCodeSplitter() {
        this(DEFAULT_MAX_CHUNK_SIZE);
    }

    public JavaCodeSplitter(int maxChunkSize) {
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
        // Return simple split as fallback
        List<String> result = new ArrayList<>();
        result.add(text);
        return result;
    }

    private List<Document> doSplit(Document document) {
        String content = document.getText();
        String source = document.getMetadata().getOrDefault("source", "unknown").toString();

        log.debug("Splitting Java file: {} (length: {} chars)", source, content.length());

        List<Document> chunks = new ArrayList<>();

        // Extract class name from content
        String className = extractClassName(content);

        // Split by methods while preserving structure
        List<CodeBlock> codeBlocks = extractCodeBlocks(content, className);

        // Group code blocks into chunks respecting max size
        List<String> chunkedContent = groupCodeBlocks(codeBlocks);

        // Create documents from chunks
        for (int i = 0; i < chunkedContent.size(); i++) {
            Document chunk = new Document(chunkedContent.get(i));
            chunk.getMetadata().putAll(document.getMetadata());
            chunk.getMetadata().put("chunk_index", i);
            chunk.getMetadata().put("total_chunks", chunkedContent.size());
            chunk.getMetadata().put("class_name", className);
            chunk.getMetadata().put("chunk_type", "java_code");

            chunks.add(chunk);
        }

        log.info("Split Java file {} into {} semantic chunks", source, chunks.size());
        return chunks;
    }

    /**
     * Extract class name from Java source code
     */
    private String extractClassName(String content) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "UnknownClass";
    }

    /**
     * Extract code blocks (imports, class declaration, methods) from Java source
     */
    private List<CodeBlock> extractCodeBlocks(String content, String className) {
        List<CodeBlock> blocks = new ArrayList<>();
        String[] lines = content.split("\n");

        StringBuilder currentBlock = new StringBuilder();
        String currentContext = "header"; // header, method, class
        int braceDepth = 0;
        int blockStartLine = 0;
        String methodName = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Track brace depth
            braceDepth += countChar(line, '{') - countChar(line, '}');

            // Detect method start
            Matcher methodMatcher = METHOD_PATTERN.matcher(line);
            if (methodMatcher.find() && braceDepth == 1) {
                // Save previous block if exists
                if (currentBlock.length() > 0 && !currentContext.equals("header")) {
                    blocks.add(
                            new CodeBlock(currentBlock.toString(), currentContext, methodName, blockStartLine, i - 1));
                    currentBlock = new StringBuilder();
                }

                methodName = methodMatcher.group(1);
                currentContext = "method";
                blockStartLine = i;
                currentBlock.append(line).append("\n");
                continue;
            }

            // Add line to current block
            currentBlock.append(line).append("\n");

            // Method end detection (when we return to class level)
            if (braceDepth == 1 && currentContext.equals("method") && line.contains("}")) {
                blocks.add(new CodeBlock(currentBlock.toString(), currentContext, methodName, blockStartLine, i));
                currentBlock = new StringBuilder();
                currentContext = "class";
                methodName = null;
                blockStartLine = i + 1;
            }
        }

        // Add remaining content
        if (currentBlock.length() > 0) {
            blocks.add(new CodeBlock(currentBlock.toString(), currentContext, methodName, blockStartLine,
                    lines.length - 1));
        }

        return blocks;
    }

    /**
     * Group code blocks into chunks respecting max size
     */
    private List<String> groupCodeBlocks(List<CodeBlock> blocks) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;

        // Always include header (package, imports, class declaration) in first chunk
        CodeBlock header = blocks.isEmpty() ? null : blocks.get(0);
        if (header != null && header.context.equals("header")) {
            currentChunk.append(header.content);
            currentSize = estimateTokens(header.content);
            blocks.remove(0);
        }

        for (CodeBlock block : blocks) {
            int blockSize = estimateTokens(block.content);

            // If adding this block exceeds max size, start new chunk
            if (currentSize + blockSize > maxChunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());

                // Start new chunk with overlap (include context)
                currentChunk = new StringBuilder();
                if (header != null) {
                    currentChunk.append("// ... continued from previous chunk\n");
                    currentChunk.append(getClassContext(header.content));
                }
                currentSize = estimateTokens(currentChunk.toString());
            }

            currentChunk.append(block.content);
            if (block.methodName != null) {
                currentChunk.append("\n// Method: ").append(block.methodName).append("\n");
            }
            currentSize += blockSize;
        }

        // Add final chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks.isEmpty() ? List.of(currentChunk.toString()) : chunks;
    }

    /**
     * Extract class context (package + class declaration) for chunk overlap
     */
    private String getClassContext(String headerContent) {
        StringBuilder context = new StringBuilder();
        String[] lines = headerContent.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("package ") || trimmed.startsWith("public class") ||
                    trimmed.startsWith("class ") || trimmed.startsWith("public interface")) {
                context.append(line).append("\n");
            }
        }

        return context.toString();
    }

    /**
     * Rough token estimation (1 token ≈ 4 characters for code)
     */
    private int estimateTokens(String text) {
        return text.length() / 4;
    }

    private int countChar(String str, char ch) {
        return (int) str.chars().filter(c -> c == ch).count();
    }

    /**
     * Internal class to represent a code block
     */
    private static class CodeBlock {
        String content;
        String context; // "header", "method", "class"
        String methodName;

        CodeBlock(String content, String context, String methodName, int startLine, int endLine) {
            this.content = content;
            this.context = context;
            this.methodName = methodName;
        }
    }
}
