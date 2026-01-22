package com.east.springairaglab.security.dto;

import java.util.Map;

/**
 * Result of PII masking operation
 */
public record MaskingResult(
        String maskedText,
        Map<String, String> mappings // placeholder -> original value
) {
}
