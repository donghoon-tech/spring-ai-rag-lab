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

import java.util.List;
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
     */
    private List<Document> retrieveDocuments(ChatRequest request) {
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.query(request.getQuery())
                            .withTopK(request.getTopK())
                            .withSimilarityThreshold(request.getSimilarityThreshold()));
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
                - Cite specific file names and line numbers when possible
                - If the context doesn't contain enough information, say so
                - Use code examples from the context when helpful
                - Format code blocks with proper syntax highlighting
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
     * Build response DTO
     */
    private ChatResponse buildResponse(String answer, List<Document> documents, long startTime) {
        List<ChatResponse.SourceDocument> sources = documents.stream()
                .map(doc -> ChatResponse.SourceDocument.builder()
                        .source(doc.getMetadata().getOrDefault("source", "unknown").toString())
                        .filename(doc.getMetadata().getOrDefault("filename", "unknown").toString())
                        .content(truncate(doc.getContent(), 200))
                        .score(doc.getMetadata().containsKey("distance")
                                ? 1.0 - (Double) doc.getMetadata().get("distance")
                                : null)
                        .metadata(formatMetadata(doc))
                        .build())
                .collect(Collectors.toList());

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
