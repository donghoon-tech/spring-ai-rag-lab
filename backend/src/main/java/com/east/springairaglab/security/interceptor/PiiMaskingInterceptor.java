package com.east.springairaglab.security.interceptor;

import com.east.springairaglab.security.detector.PiiDetector;
import com.east.springairaglab.security.dto.MaskingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Intercepts prompts to mask PII before sending to LLM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PiiMaskingInterceptor {

    private final PiiDetector piiDetector;

    /**
     * Mask PII in the given prompt
     * 
     * @param prompt Original user prompt
     * @return Masked prompt
     */
    public String maskPrompt(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return prompt;
        }

        MaskingResult result = piiDetector.maskPii(prompt);

        if (result.mappings().isEmpty()) {
            log.debug("No PII detected in prompt");
            return prompt;
        }

        log.info("Masked {} PII instances in prompt", result.mappings().size());
        log.debug("PII types masked: {}", result.mappings().keySet());

        return result.maskedText();
    }
}
