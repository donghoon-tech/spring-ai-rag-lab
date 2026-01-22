package com.east.springairaglab.agent.tools;

import com.east.springairaglab.agent.dto.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CodeSecurityScannerTest {

    private CodeSecurityScanner scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new CodeSecurityScanner();
    }

    @Test
    void shouldDetectHardcodedPassword() throws IOException {
        String code = """
                public class Config {
                    private String password = "MySecretPass123";
                }
                """;

        Path file = tempDir.resolve("Config.java");
        Files.writeString(file, code);

        ScanResult result = scanner.scanForHardcodedCredentials(file.toString());

        assertThat(result.issuesFound()).isEqualTo(1);
        assertThat(result.issues()).hasSize(1);
        assertThat(result.issues().get(0).type()).isEqualTo("HARDCODED_PASSWORD");
    }

    @Test
    void shouldDetectHardcodedApiKey() throws IOException {
        String code = """
                public class ApiClient {
                    private static final String API_KEY = "sk_live_1234567890abcdef";
                }
                """;

        Path file = tempDir.resolve("ApiClient.java");
        Files.writeString(file, code);

        ScanResult result = scanner.scanForHardcodedCredentials(file.toString());

        assertThat(result.issuesFound()).isEqualTo(1);
        assertThat(result.issues().get(0).type()).isEqualTo("HARDCODED_API_KEY");
    }

    @Test
    void shouldDetectMultipleIssues() throws IOException {
        String code = """
                public class BadConfig {
                    private String password = "hardcoded123";
                    private String apiKey = "sk_test_abcdefghijklmnop";
                }
                """;

        Path file = tempDir.resolve("BadConfig.java");
        Files.writeString(file, code);

        ScanResult result = scanner.scanForHardcodedCredentials(file.toString());

        assertThat(result.issuesFound()).isEqualTo(2);
    }

    @Test
    void shouldReturnEmptyForCleanCode() throws IOException {
        String code = """
                public class GoodConfig {
                    @Value("${api.key}")
                    private String apiKey;
                }
                """;

        Path file = tempDir.resolve("GoodConfig.java");
        Files.writeString(file, code);

        ScanResult result = scanner.scanForHardcodedCredentials(file.toString());

        assertThat(result.issuesFound()).isZero();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void shouldHandleNonExistentFile() {
        ScanResult result = scanner.scanForHardcodedCredentials("/non/existent/file.java");

        assertThat(result.issuesFound()).isZero();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void shouldIncludeLineNumbers() throws IOException {
        String code = """
                line 1
                line 2
                String password = "secret123";
                line 4
                """;

        Path file = tempDir.resolve("Test.java");
        Files.writeString(file, code);

        ScanResult result = scanner.scanForHardcodedCredentials(file.toString());

        assertThat(result.issues().get(0).lineNumber()).isEqualTo(3);
    }
}
