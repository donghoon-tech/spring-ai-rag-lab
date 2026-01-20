package com.east.springairaglab.chat.service;

import com.east.springairaglab.chat.dto.ChatRequest;
import com.east.springairaglab.chat.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) Service
 * Combines vector similarity search with LLM generation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final HybridSearchService hybridSearchService; // Optional: for hybrid search

    /**
     * Process a chat query using RAG pipeline
     */
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("Processing RAG query: {}", request.getQuery());

        // 1. Retrieve similar documents from vector store
        List<Document> similarDocs = retrieveDocuments(request);

        if (similarDocs.isEmpty()) {
            log.warn("No relevant documents found for query: {}", request.getQuery());
            return buildNoResultsResponse(request, startTime);
        }

        log.info("Retrieved {} similar documents", similarDocs.size());

        // 2. Build context from retrieved documents
        String context = buildContext(similarDocs);

        // 3. Generate answer using LLM
        String answer = generateAnswer(request.getQuery(), context);

        // 4. Build response with sources
        return buildResponse(answer, similarDocs, startTime);
    }

    /**
     * Retrieve similar documents from vector store
     * Uses hybrid search if available, otherwise falls back to semantic search
     */
    private List<Document> retrieveDocuments(ChatRequest request) {
        try {
            // Use hybrid search for better results
            return hybridSearchService.search(
                    request.getQuery(),
                    request.getTopK(),
                    request.getSimilarityThreshold(),
                    request.getFilters());
        } catch (Exception e) {
            log.error("Error retrieving documents", e);
            return List.of();
        }
    }

    /**
     * Build context string from retrieved documents
     */
    private String buildContext(List<Document> documents) {
        return documents.stream()
                .map(doc -> {
                    String source = doc.getMetadata().getOrDefault("source", "unknown").toString();
                    String filename = doc.getMetadata().getOrDefault("filename", "unknown").toString();
                    String content = doc.getContent();

                    return String.format("""
                            [Source: %s]
                            [File: %s]
                            %s
                            """, source, filename, content);
                })
                .collect(Collectors.joining("\n---\n"));
    }

    /**
     * Generate answer using LLM with context
     */
    private String generateAnswer(String query, String context) {
        String systemPrompt = """
                You are a helpful code assistant with deep knowledge of software engineering.
                Answer the user's question based ONLY on the provided code context.

                Guidelines:
                - Be concise and technical
                - ALWAYS cite sources using [1], [2], etc. when referencing specific information
                - Cite specific file names, class names, and method names when available
                - If the context doesn't contain enough information, say so
                - Use code examples from the context when helpful
                - Format code blocks with proper syntax highlighting
                - Place citations immediately after the relevant statement

                Example: "The UserService class handles authentication [1] using JWT tokens [2]."
                """;

        String userPrompt = String.format("""
                Context from codebase:
                %s

                Question: %s

                Answer:
                """, context, query);

        try {
            ChatClient chatClient = ChatClient.create(chatModel);

            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Error generating answer", e);
            return "Sorry, I encountered an error generating the answer. Please try again.";
        }
    }

    /**
     * Build response DTO with citations
     */
    private ChatResponse buildResponse(String answer, List<Document> documents, long startTime) {
        List<ChatResponse.SourceDocument> sources = new ArrayList<>();

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Map<String, Object> metadata = doc.getMetadata();

            sources.add(ChatResponse.SourceDocument.builder()
                    .citationNumber(i + 1)
                    .source(metadata.getOrDefault("source", "unknown").toString())
                    .filename(metadata.getOrDefault("filename", "unknown").toString())
                    .content(truncate(doc.getContent(), 200))
                    .score(metadata.containsKey("hybrid_score")
                            ? (Double) metadata.get("hybrid_score")
                            : (metadata.containsKey("distance")
                                    ? 1.0 - (Double) metadata.get("distance")
                                    : null))
                    .metadata(formatMetadata(doc))
                    .lineRange(extractLineRange(metadata))
                    .className(metadata.getOrDefault("class_name", "").toString())
                    .methodName(metadata.getOrDefault("method_name", "").toString())
                    .build());
        }

        long processingTime = System.currentTimeMillis() - startTime;

        return ChatResponse.builder()
                .answer(answer)
                .sources(sources)
                .metadata(ChatResponse.ResponseMetadata.builder()
                        .documentsRetrieved(documents.size())
                        .processingTimeMs(processingTime)
                        .model(chatModel.getClass().getSimpleName())
                        .build())
                .build();
    }

    /**
     * Extract line range from metadata
     */
    private String extractLineRange(Map<String, Object> metadata) {
        Object startLine = metadata.get("start_line");
        Object endLine = metadata.get("end_line");

        if (startLine != null && endLine != null) {
            return startLine + "-" + endLine;
        }
        return null;
    }

    /**
     * Build response when no documents found
     */
    private ChatResponse buildNoResultsResponse(ChatRequest request, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;

        return ChatResponse.builder()
                .answer("I couldn't find any relevant information in the codebase to answer your question. " +
                        "Please try rephrasing your query or check if the documents have been ingested.")
                .sources(List.of())
                .metadata(ChatResponse.ResponseMetadata.builder()
                        .documentsRetrieved(0)
                        .processingTimeMs(processingTime)
                        .model(chatModel.getClass().getSimpleName())
                        .build())
                .build();
    }

    /**
     * Truncate content for display
     */
    private String truncate(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * Format metadata for display
     */
    private String formatMetadata(Document doc) {
        return doc.getMetadata().entrySet().stream()
                .filter(e -> !e.getKey().equals("source") && !e.getKey().equals("filename"))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
