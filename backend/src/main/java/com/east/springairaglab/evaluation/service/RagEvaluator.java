package com.east.springairaglab.evaluation.service;

import com.east.springairaglab.chat.dto.ChatRequest;
import com.east.springairaglab.chat.dto.ChatResponse;
import com.east.springairaglab.chat.service.RagService;
import com.east.springairaglab.evaluation.dto.EvaluationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagEvaluator {

    private final RagService ragService;
    private final ChatClient chatClient;

    public RagEvaluator(RagService ragService, @Qualifier("ollamaChatModel") ChatModel chatModel) {
        this.ragService = ragService;
        this.chatClient = ChatClient.create(chatModel);
    }

    public EvaluationResult evaluate(String query) {
        log.info("Starting evaluation for query: {}", query);

        // 1. Run RAG Pipeline
        ChatRequest request = new ChatRequest(query, 3, 0.7);
        long start = System.currentTimeMillis();
        ChatResponse response = ragService.chat(request);
        long latency = System.currentTimeMillis() - start;

        String answer = response.getAnswer();
        String context = response.getSources().stream()
                .map(doc -> doc.getContent())
                .collect(Collectors.joining("\n\n"));

        // 2. Evaluate Relevance
        int relevanceScore = evaluateRelevance(query, answer);

        // 3. Evaluate Faithfulness (if context exists)
        int faithfulnessScore = 0;
        if (!context.isEmpty()) {
            faithfulnessScore = evaluateFaithfulness(context, answer);
        }

        Map<String, Integer> scores = new HashMap<>();
        scores.put("relevance", relevanceScore);
        scores.put("faithfulness", faithfulnessScore);

        // Reasoning is simplified for now, could be extracted from LLM response in
        // future
        Map<String, String> reasoning = new HashMap<>();
        reasoning.put("summary", "Automated evaluation using LLM-as-a-Judge");

        return new EvaluationResult(query, answer, scores, reasoning, latency);
    }

    private int evaluateRelevance(String query, String answer) {
        String prompt = String.format("""
                You are an expert evaluator for a RAG system.
                Your task is to rate the RELEVANCE of the answer to the query on a scale of 1 to 5.

                Query: %s
                Answer: %s

                Rating Criteria:
                1: Irrelevant answer, does not address the query at all.
                3: Partially relevant, addresses some aspects but misses key points.
                5: Highly relevant, directly and fully answers the query.

                OUTPUT ONLY A SINGLE INTEGER (1-5). DO NOT EXPLAIN.
                """, query, answer);

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return parseScore(result);
        } catch (Exception e) {
            log.error("Error evaluating relevance", e);
            return 0;
        }
    }

    private int evaluateFaithfulness(String context, String answer) {
        // Truncate context if too long to avoid exceeding token limits
        if (context.length() > 2000) {
            context = context.substring(0, 2000) + "...";
        }

        String prompt = String.format("""
                You are an expert evaluator for a RAG system.
                Your task is to rate the FAITHFULNESS of the answer based on the provided context on a scale of 1 to 5.

                Context:
                %s

                Answer: %s

                Rating Criteria:
                1: Hallucinated answer, contains information NOT found in the context.
                3: Mixed faithfulness, some statements supported, others not.
                5: Faithful answer, all statements are supported by the provided context.

                OUTPUT ONLY A SINGLE INTEGER (1-5). DO NOT EXPLAIN.
                """, context, answer);

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return parseScore(result);
        } catch (Exception e) {
            log.error("Error evaluating faithfulness", e);
            return 0;
        }
    }

    private int parseScore(String text) {
        try {
            // Extract the first digit found in the response
            String digit = text.replaceAll("[^0-9]", "");
            if (digit.isEmpty())
                return 0;
            return Integer.parseInt(digit.substring(0, 1)); // Take first digit only
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
