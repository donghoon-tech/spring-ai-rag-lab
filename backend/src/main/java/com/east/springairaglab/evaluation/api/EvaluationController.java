package com.east.springairaglab.evaluation.api;

import com.east.springairaglab.evaluation.dto.EvaluationResult;
import com.east.springairaglab.evaluation.service.RagEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final RagEvaluator ragEvaluator;

    @PostMapping("/run")
    public ResponseEntity<EvaluationResult> runEvaluation(@RequestBody Map<String, String> payload) {
        String query = payload.get("query");
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        EvaluationResult result = ragEvaluator.evaluate(query);
        return ResponseEntity.ok(result);
    }
}
