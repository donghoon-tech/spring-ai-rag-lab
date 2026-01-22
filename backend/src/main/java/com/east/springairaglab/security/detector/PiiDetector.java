package com.east.springairaglab.security.detector;

import com.east.springairaglab.security.dto.MaskingResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects and masks Personally Identifiable Information (PII) in text
 */
@Component
public class PiiDetector {

    // Regex patterns for PII detection
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b\\d{3}[-.]?\\d{3,4}(?:[-.]?\\d{4})?\\b");

    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|token|secret)[:\\s=]*['\"]?([a-zA-Z0-9_-]{16,})['\"]?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(?i)(password|passwd|pwd)[:\\s]*['\"]?([^\\s'\"]{8,})['\"]?",
            Pattern.CASE_INSENSITIVE);

    /**
     * Mask PII in the given text
     * 
     * @param text Input text potentially containing PII
     * @return MaskingResult with masked text and mappings
     */
    public MaskingResult maskPii(String text) {
        if (text == null || text.isEmpty()) {
            return new MaskingResult(text, Map.of());
        }

        Map<String, String> mappings = new HashMap<>();
        String maskedText = text;

        // Mask emails
        maskedText = maskPattern(maskedText, EMAIL_PATTERN, "EMAIL", mappings, new AtomicInteger(1));

        // Mask phone numbers
        maskedText = maskPattern(maskedText, PHONE_PATTERN, "PHONE", mappings, new AtomicInteger(1));

        // Mask API keys (capture group 2 contains the actual key)
        maskedText = maskPatternWithGroup(maskedText, API_KEY_PATTERN, "API_KEY", mappings, new AtomicInteger(1), 2);

        // Mask passwords (capture group 2 contains the actual password)
        maskedText = maskPatternWithGroup(maskedText, PASSWORD_PATTERN, "PASSWORD", mappings, new AtomicInteger(1), 2);

        return new MaskingResult(maskedText, mappings);
    }

    /**
     * Mask all occurrences of a pattern
     */
    private String maskPattern(String text, Pattern pattern, String type,
            Map<String, String> mappings, AtomicInteger counter) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String original = matcher.group();
            String placeholder = String.format("[%s_REDACTED_%d]", type, counter.getAndIncrement());
            mappings.put(placeholder, original);
            matcher.appendReplacement(result, Matcher.quoteReplacement(placeholder));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Mask specific capture group of a pattern (for patterns with prefix like
     * "api_key: value")
     */
    private String maskPatternWithGroup(String text, Pattern pattern, String type,
            Map<String, String> mappings, AtomicInteger counter, int groupIndex) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String original = matcher.group(groupIndex);
            String placeholder = String.format("[%s_REDACTED_%d]", type, counter.getAndIncrement());
            mappings.put(placeholder, original);

            // Replace only the sensitive part (group), keep the prefix
            String replacement = matcher.group().replace(original, placeholder);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Restore original values from masked text
     * 
     * @param maskedText Text with placeholders
     * @param mappings   Placeholder to original value mappings
     * @return Original text
     */
    public String restorePii(String maskedText, Map<String, String> mappings) {
        if (maskedText == null || mappings.isEmpty()) {
            return maskedText;
        }

        String result = maskedText;
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
