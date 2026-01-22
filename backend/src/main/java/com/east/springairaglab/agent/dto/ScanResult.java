package com.east.springairaglab.agent.dto;

import java.util.List;

/**
 * Result of security scan
 */
public record ScanResult(
        String filePath,
        int issuesFound,
        List<SecurityIssue> issues) {
    public record SecurityIssue(
            String type,
            int lineNumber,
            String description,
            String snippet) {
    }
}
