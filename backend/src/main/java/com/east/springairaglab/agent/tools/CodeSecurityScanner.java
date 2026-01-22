package com.east.springairaglab.agent.tools;

import com.east.springairaglab.agent.dto.ScanResult;
import com.east.springairaglab.agent.dto.ScanResult.SecurityIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Code security scanner for detecting hardcoded credentials and vulnerabilities
 */
@Slf4j
@Component
public class CodeSecurityScanner {

    // Patterns for detecting hardcoded credentials
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(?i)(password|passwd|pwd)\\s*[=:]\\s*['\"]([^'\"]{8,})['\"]",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|token|secret)\\s*[=:]\\s*['\"]([a-zA-Z0-9_-]{16,})['\"]",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DATABASE_URL_PATTERN = Pattern.compile(
            "(?i)(jdbc|mongodb|mysql|postgresql)://[^:]+:([^@]+)@",
            Pattern.CASE_INSENSITIVE);

    /**
     * Scan a file for hardcoded credentials
     * 
     * @param filePath Path to file to scan
     * @return Scan results with detected issues
     */
    public ScanResult scanForHardcodedCredentials(String filePath) {
        log.info("Scanning file for hardcoded credentials: {}", filePath);

        try {
            Path path = Path.of(filePath);

            if (!Files.exists(path)) {
                return new ScanResult(filePath, 0, List.of());
            }

            List<String> lines = Files.readAllLines(path);
            List<SecurityIssue> issues = new ArrayList<>();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int lineNumber = i + 1;

                // Check for passwords
                Matcher passwordMatcher = PASSWORD_PATTERN.matcher(line);
                if (passwordMatcher.find()) {
                    issues.add(new SecurityIssue(
                            "HARDCODED_PASSWORD",
                            lineNumber,
                            "Hardcoded password detected",
                            line.trim()));
                }

                // Check for API keys
                Matcher apiKeyMatcher = API_KEY_PATTERN.matcher(line);
                if (apiKeyMatcher.find()) {
                    issues.add(new SecurityIssue(
                            "HARDCODED_API_KEY",
                            lineNumber,
                            "Hardcoded API key or token detected",
                            line.trim()));
                }

                // Check for database URLs with credentials
                Matcher dbUrlMatcher = DATABASE_URL_PATTERN.matcher(line);
                if (dbUrlMatcher.find()) {
                    issues.add(new SecurityIssue(
                            "HARDCODED_DB_CREDENTIALS",
                            lineNumber,
                            "Database URL with embedded credentials detected",
                            line.trim()));
                }
            }

            log.info("Scan complete: {} issues found in {}", issues.size(), filePath);
            return new ScanResult(filePath, issues.size(), issues);

        } catch (IOException e) {
            log.error("Error scanning file: {}", filePath, e);
            return new ScanResult(filePath, 0, List.of());
        }
    }
}
