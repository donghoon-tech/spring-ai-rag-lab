package com.east.springairaglab.evaluation.service;

import com.east.springairaglab.chat.dto.ChatRequest;
import com.east.springairaglab.chat.service.RagService;
import com.east.springairaglab.evaluation.dto.EvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RagEvaluatorTest {

        @Mock
        private RagService ragService;

        @Mock
        private ChatModel chatModel;

        private RagEvaluator ragEvaluator;

        @BeforeEach
        void setUp() {
                ragEvaluator = new RagEvaluator(ragService, chatModel);
        }

        @Test
        void evaluate_ShouldReturnScores() {
                // Given
                String query = "Optimistic locking?";
                String answer = "Use @Version annotation.";

                // Mock RagService response
                com.east.springairaglab.chat.dto.ChatResponse.SourceDocument sourceDoc = com.east.springairaglab.chat.dto.ChatResponse.SourceDocument
                                .builder()
                                .content("Content about optimistic locking")
                                .score(0.9)
                                .build();

                com.east.springairaglab.chat.dto.ChatResponse.ResponseMetadata metadata = com.east.springairaglab.chat.dto.ChatResponse.ResponseMetadata
                                .builder()
                                .documentsRetrieved(1)
                                .processingTimeMs(100L)
                                .build();

                com.east.springairaglab.chat.dto.ChatResponse ragResponse = com.east.springairaglab.chat.dto.ChatResponse
                                .builder()
                                .answer(answer)
                                .sources(List.of(sourceDoc))
                                .metadata(metadata)
                                .build();

                when(ragService.chat(any(ChatRequest.class))).thenReturn(ragResponse);

                // Mock ChatModel response (Evaluator LLM) - Sequence of calls

                // 1. Relevance Response
                Generation relevanceGen = mock(Generation.class);
                when(relevanceGen.getOutput()).thenReturn(new AssistantMessage("5"));
                org.springframework.ai.chat.model.ChatResponse relevanceResponse = mock(
                                org.springframework.ai.chat.model.ChatResponse.class);
                when(relevanceResponse.getResult()).thenReturn(relevanceGen);

                // 2. Faithfulness Response
                Generation faithfulnessGen = mock(Generation.class);
                when(faithfulnessGen.getOutput()).thenReturn(new AssistantMessage("4"));
                org.springframework.ai.chat.model.ChatResponse faithfulnessResponse = mock(
                                org.springframework.ai.chat.model.ChatResponse.class);
                when(faithfulnessResponse.getResult()).thenReturn(faithfulnessGen);

                // Return sequence of responses
                when(chatModel.call(any(Prompt.class)))
                                .thenReturn(relevanceResponse)
                                .thenReturn(faithfulnessResponse);

                // When
                EvaluationResult result = ragEvaluator.evaluate(query);

                // Then
                assertThat(result.query()).isEqualTo(query);
                assertThat(result.answer()).isEqualTo(answer);
                assertThat(result.scores()).containsEntry("relevance", 5);
                assertThat(result.scores()).containsEntry("faithfulness", 4);
        }
}
