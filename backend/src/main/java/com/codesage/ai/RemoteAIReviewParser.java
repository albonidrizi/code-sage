package com.codesage.ai;

import com.codesage.model.ReviewIssue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class RemoteAIReviewParser {

    private final ObjectMapper objectMapper;

    RemoteAIReviewParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    AnalysisResult parse(String content, long inputTokens, long outputTokens, double estimatedCostUsd) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(content));
            List<IssueFinding> findings = new ArrayList<>();
            JsonNode issues = root.path("issues");
            if (issues.isArray()) {
                for (JsonNode issue : issues) {
                    findings.add(new IssueFinding(
                            enumValue(issue.path("type").asText(), ReviewIssue.IssueType.CODE_QUALITY),
                            enumValue(issue.path("severity").asText(), ReviewIssue.IssueSeverity.MEDIUM),
                            textOrDefault(issue.path("filePath"), "unknown"),
                            issue.hasNonNull("lineNumber") ? issue.path("lineNumber").asInt() : null,
                            textOrDefault(issue.path("title"), "Review finding"),
                            textOrDefault(issue.path("description"), "No description provided."),
                            textOrDefault(issue.path("suggestion"), "")));
                }
            }

            double score = Math.max(0.0, Math.min(10.0, root.path("qualityScore").asDouble(7.0)));
            String summary = textOrDefault(root.path("summary"), "Remote provider returned a structured review.");
            return new AnalysisResult(score, summary, List.copyOf(findings), inputTokens, outputTokens,
                    estimatedCostUsd);
        } catch (IOException exception) {
            throw new IllegalArgumentException("AI provider returned invalid review JSON", exception);
        }
    }

    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("AI provider returned empty review content");
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int closingFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && closingFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, closingFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("AI provider response did not contain a JSON object");
        }
        return trimmed.substring(start, end + 1);
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return defaultValue;
        }
        return node.asText();
    }

    private <T extends Enum<T>> T enumValue(String value, T defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(defaultValue.getDeclaringClass(), value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return defaultValue;
        }
    }
}
