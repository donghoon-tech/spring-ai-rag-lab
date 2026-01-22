package com.east.springairaglab.evaluation.dto;

import java.util.Map;

public record EvaluationResult(
        String query,
        String answer,
        Map<String, Integer> scores, // e.g., "relevance": 5, "faithfulness": 4
        Map<String, String> reasoning, // e.g., "relevance": "The answer directly addresses..."
        long latencyMs) {
}
