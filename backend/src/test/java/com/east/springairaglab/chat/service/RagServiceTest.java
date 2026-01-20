package com.east.springairaglab.chat.service;

import com.east.springairaglab.chat.dto.ChatRequest;
import com.east.springairaglab.chat.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RagService
 */
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

        @Mock
        private VectorStore vectorStore;

        @Mock
        private ChatModel chatModel;

        @Mock
        private HybridSearchService hybridSearchService;

        private RagService ragService;

        @BeforeEach
        void setUp() {
                ragService = new RagService(vectorStore, chatModel, hybridSearchService);
        }

        @Test
        void chat_ShouldReturnAnswerWithSources_WhenDocumentsFound() {
                // Given
                ChatRequest request = new ChatRequest("How does JavaCodeSplitter work?", 3, 0.7);

                List<Document> mockDocuments = List.of(
                                new Document("JavaCodeSplitter uses regex patterns to parse Java code",
                                                Map.of("source", "/path/to/JavaCodeSplitter.java",
                                                                "filename", "JavaCodeSplitter.java",
                                                                "class_name", "JavaCodeSplitter")),
                                new Document("It tracks brace depth to identify method boundaries",
                                                Map.of("source", "/path/to/JavaCodeSplitter.java",
                                                                "filename", "JavaCodeSplitter.java",
                                                                "method_name", "doSplit")));

                when(vectorStore.similaritySearch(any(SearchRequest.class)))
                                .thenReturn(mockDocuments);

                // Mock ChatModel response
                Generation mockGeneration = mock(Generation.class);
                when(mockGeneration.getOutput()).thenReturn(
                                new org.springframework.ai.chat.messages.AssistantMessage(
                                                "JavaCodeSplitter uses regex patterns to parse Java code and tracks brace depth."));

                org.springframework.ai.chat.model.ChatResponse mockChatResponse = mock(
                                org.springframework.ai.chat.model.ChatResponse.class);
                when(mockChatResponse.getResult()).thenReturn(mockGeneration);
                when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);

                // When
                ChatResponse response = ragService.chat(request);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getAnswer()).isNotBlank();
                assertThat(response.getSources()).hasSize(2);
                assertThat(response.getSources().get(0).getFilename()).isEqualTo("JavaCodeSplitter.java");
                assertThat(response.getMetadata().getDocumentsRetrieved()).isEqualTo(2);
                assertThat(response.getMetadata().getProcessingTimeMs()).isGreaterThan(0);

                verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
                verify(chatModel, times(1)).call(any(Prompt.class));
        }

        @Test
        void chat_ShouldReturnNoResultsMessage_WhenNoDocumentsFound() {
                // Given
                ChatRequest request = new ChatRequest("Unknown query", 3, 0.7);

                when(vectorStore.similaritySearch(any(SearchRequest.class)))
                                .thenReturn(List.of());

                // When
                ChatResponse response = ragService.chat(request);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getAnswer()).contains("couldn't find any relevant information");
                assertThat(response.getSources()).isEmpty();
                assertThat(response.getMetadata().getDocumentsRetrieved()).isEqualTo(0);

                verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
                verify(chatModel, never()).call(any(Prompt.class));
        }

        @Test
        void chat_ShouldHandleVectorStoreException_Gracefully() {
                // Given
                ChatRequest request = new ChatRequest("Test query", 3, 0.7);

                when(vectorStore.similaritySearch(any(SearchRequest.class)))
                                .thenThrow(new RuntimeException("Vector store error"));

                // When
                ChatResponse response = ragService.chat(request);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getAnswer()).contains("couldn't find any relevant information");
                assertThat(response.getSources()).isEmpty();
        }

        @Test
        void chat_ShouldIncludeMetadataInSources() {
                // Given
                ChatRequest request = new ChatRequest("Test query", 3, 0.7);

                List<Document> mockDocuments = List.of(
                                new Document("Test content",
                                                Map.of("source", "/test.java",
                                                                "filename", "test.java",
                                                                "class_name", "TestClass",
                                                                "method_name", "testMethod",
                                                                "chunk_type", "java_code")));

                when(vectorStore.similaritySearch(any(SearchRequest.class)))
                                .thenReturn(mockDocuments);

                Generation mockGeneration = mock(Generation.class);
                when(mockGeneration.getOutput()).thenReturn(
                                new org.springframework.ai.chat.messages.AssistantMessage("Test answer"));

                org.springframework.ai.chat.model.ChatResponse mockChatResponse = mock(
                                org.springframework.ai.chat.model.ChatResponse.class);
                when(mockChatResponse.getResult()).thenReturn(mockGeneration);
                when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);

                // When
                ChatResponse response = ragService.chat(request);

                // Then
                assertThat(response.getSources()).hasSize(1);
                ChatResponse.SourceDocument source = response.getSources().get(0);
                assertThat(source.getMetadata()).contains("class_name=TestClass");
                assertThat(source.getMetadata()).contains("method_name=testMethod");
        }
}
