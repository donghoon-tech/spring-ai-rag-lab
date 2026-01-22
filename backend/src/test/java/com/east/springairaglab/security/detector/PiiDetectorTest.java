package com.east.springairaglab.security.detector;

import com.east.springairaglab.security.dto.MaskingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiDetectorTest {

    private PiiDetector piiDetector;

    @BeforeEach
    void setUp() {
        piiDetector = new PiiDetector();
    }

    @Test
    void shouldDetectAndMaskEmail() {
        String text = "Contact me at john.doe@example.com for more info";

        MaskingResult result = piiDetector.maskPii(text);

        assertThat(result.maskedText()).contains("[EMAIL_REDACTED_1]");
        assertThat(result.maskedText()).doesNotContain("john.doe@example.com");
        assertThat(result.mappings()).containsEntry("[EMAIL_REDACTED_1]", "john.doe@example.com");
    }

    @Test
    void shouldDetectAndMaskPhoneNumber() {
        String text = "Call me at 555-123-4567 or 555.987.6543";

        MaskingResult result = piiDetector.maskPii(text);

        assertThat(result.maskedText()).contains("[PHONE_REDACTED_1]", "[PHONE_REDACTED_2]");
        assertThat(result.maskedText()).doesNotContain("555-123-4567", "555.987.6543");
        assertThat(result.mappings()).hasSize(2);
    }

    @Test
    void shouldDetectAndMaskApiKey() {
        String text = "Use this api_key: sk_test_1234567890abcdefghij for authentication";

        MaskingResult result = piiDetector.maskPii(text);

        assertThat(result.maskedText()).contains("[API_KEY_REDACTED_1]");
        assertThat(result.maskedText()).doesNotContain("sk_test_1234567890abcdefghij");
        assertThat(result.mappings()).containsEntry("[API_KEY_REDACTED_1]", "sk_test_1234567890abcdefghij");
    }

    @Test
    void shouldDetectAndMaskPassword() {
        String text = "Login with password: MySecretPass123!";

        MaskingResult result = piiDetector.maskPii(text);

        assertThat(result.maskedText()).contains("[PASSWORD_REDACTED_1]");
        assertThat(result.maskedText()).doesNotContain("MySecretPass123!");
        assertThat(result.mappings()).containsEntry("[PASSWORD_REDACTED_1]", "MySecretPass123!");
    }

    @Test
    void shouldDetectMultiplePiiTypes() {
        String text = "Email: admin@company.com, Phone: 555-1234, API Key: token abc123def456ghi789jkl012";

        MaskingResult result = piiDetector.maskPii(text);

        assertThat(result.maskedText()).contains(
                "[EMAIL_REDACTED_1]",
                "[PHONE_REDACTED_1]",
                "[API_KEY_REDACTED_1]");
        assertThat(result.mappings()).hasSize(3);
    }

    @Test
    void shouldHandleTextWithNoPii() {
        String text = "This is a normal text without any sensitive information";

        MaskingResult result = piiDetector.maskPii(text);

        assertThat(result.maskedText()).isEqualTo(text);
        assertThat(result.mappings()).isEmpty();
    }

    @Test
    void shouldHandleNullText() {
        MaskingResult result = piiDetector.maskPii(null);

        assertThat(result.maskedText()).isNull();
        assertThat(result.mappings()).isEmpty();
    }

    @Test
    void shouldHandleEmptyText() {
        MaskingResult result = piiDetector.maskPii("");

        assertThat(result.maskedText()).isEmpty();
        assertThat(result.mappings()).isEmpty();
    }

    @Test
    void shouldRestorePiiFromMaskedText() {
        String original = "Contact john@example.com at 555-1234";

        MaskingResult masked = piiDetector.maskPii(original);
        String restored = piiDetector.restorePii(masked.maskedText(), masked.mappings());

        assertThat(restored).isEqualTo(original);
    }

    @Test
    void shouldHandleMultipleEmailsInSameText() {
        String text = "Send to alice@example.com and bob@example.com";

        MaskingResult result = piiDetector.maskPii(text);

        assertThat(result.maskedText()).contains("[EMAIL_REDACTED_1]", "[EMAIL_REDACTED_2]");
        assertThat(result.mappings()).hasSize(2);
        assertThat(result.mappings()).containsEntry("[EMAIL_REDACTED_1]", "alice@example.com");
        assertThat(result.mappings()).containsEntry("[EMAIL_REDACTED_2]", "bob@example.com");
    }

    @Test
    void shouldDetectApiKeyWithDifferentFormats() {
        String text1 = "api-key: sk_live_abcdefghijklmnopqrst";
        String text2 = "secret=\"ghp_1234567890abcdefghij\"";

        MaskingResult result1 = piiDetector.maskPii(text1);
        MaskingResult result2 = piiDetector.maskPii(text2);

        assertThat(result1.maskedText()).contains("[API_KEY_REDACTED_1]");
        assertThat(result2.maskedText()).contains("[API_KEY_REDACTED_1]");
    }
}
