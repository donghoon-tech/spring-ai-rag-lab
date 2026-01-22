package com.east.springairaglab.security.advisor;

import com.east.springairaglab.security.detector.PiiDetector;
import com.east.springairaglab.security.dto.MaskingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.RequestAdvisor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Request advisor that masks PII before sending to LLM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PiiMaskingAdvisor implements RequestAdvisor {

    private final PiiDetector piiDetector;

    private static final String PII_MAPPINGS_KEY = "pii_mappings";

    @Override
    public AdvisedRequest adviseRequest(AdvisedRequest request, Map<String, Object> context) {
        String originalPrompt = request.userText();

        if (originalPrompt == null || originalPrompt.isEmpty()) {
            return request;
        }

        // Mask PII in the prompt
        MaskingResult result = piiDetector.maskPii(originalPrompt);

        if (result.mappings().isEmpty()) {
            log.debug("No PII detected in request");
            return request;
        }

        // Store mappings in context for potential restoration
        context.put(PII_MAPPINGS_KEY, result.mappings());

        log.info("Masked {} PII instances in request", result.mappings().size());
        log.debug("PII types masked: {}", result.mappings().keySet());

        // Return request with masked text
        return AdvisedRequest.from(request)
                .withUserText(result.maskedText())
                .build();
    }

    @Override
    public String getName() {
        return "PiiMaskingAdvisor";
    }

    @Override
    public int getOrder() {
        return 0; // Run first, before other advisors
    }
}
