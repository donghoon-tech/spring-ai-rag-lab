package com.east.springairaglab.security.advisor;

import com.east.springairaglab.security.detector.PiiDetector;
import com.east.springairaglab.security.dto.MaskingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PiiMaskingAdvisorTest {

    @Mock
    private PiiDetector piiDetector;

    private PiiMaskingAdvisor advisor;

    @BeforeEach
    void setUp() {
        advisor = new PiiMaskingAdvisor(piiDetector);
    }

    @Test
    void shouldMaskPiiInRequest() {
        String originalText = "Contact me at john@example.com";
        String maskedText = "Contact me at [EMAIL_REDACTED_1]";
        Map<String, String> mappings = Map.of("[EMAIL_REDACTED_1]", "john@example.com");

        when(piiDetector.maskPii(originalText))
                .thenReturn(new MaskingResult(maskedText, mappings));

        AdvisedRequest originalRequest = AdvisedRequest.from("test-user")
                .withUserText(originalText)
                .build();

        Map<String, Object> context = new HashMap<>();

        AdvisedRequest result = advisor.adviseRequest(originalRequest, context);

        assertThat(result.userText()).isEqualTo(maskedText);
        assertThat(context).containsKey("pii_mappings");
        assertThat(context.get("pii_mappings")).isEqualTo(mappings);
    }

    @Test
    void shouldNotModifyRequestWithNoPii() {
        String originalText = "This is a normal text";

        when(piiDetector.maskPii(originalText))
                .thenReturn(new MaskingResult(originalText, Map.of()));

        AdvisedRequest originalRequest = AdvisedRequest.from("test-user")
                .withUserText(originalText)
                .build();

        Map<String, Object> context = new HashMap<>();

        AdvisedRequest result = advisor.adviseRequest(originalRequest, context);

        assertThat(result.userText()).isEqualTo(originalText);
        assertThat(context).doesNotContainKey("pii_mappings");
    }

    @Test
    void shouldHandleNullPrompt() {
        AdvisedRequest originalRequest = AdvisedRequest.from("test-user")
                .withUserText(null)
                .build();

        Map<String, Object> context = new HashMap<>();

        AdvisedRequest result = advisor.adviseRequest(originalRequest, context);

        assertThat(result.userText()).isNull();
    }

    @Test
    void shouldHandleEmptyPrompt() {
        AdvisedRequest originalRequest = AdvisedRequest.from("test-user")
                .withUserText("")
                .build();

        Map<String, Object> context = new HashMap<>();

        AdvisedRequest result = advisor.adviseRequest(originalRequest, context);

        assertThat(result.userText()).isEmpty();
    }

    @Test
    void shouldHaveCorrectName() {
        assertThat(advisor.getName()).isEqualTo("PiiMaskingAdvisor");
    }

    @Test
    void shouldHaveHighestPriority() {
        assertThat(advisor.getOrder()).isEqualTo(0);
    }
}
