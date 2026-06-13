package com.codesage.ai;

import java.util.List;

public record AnalysisResult(
        double qualityScore,
        String summary,
        List<IssueFinding> issues,
        long inputTokens,
        long outputTokens,
        double estimatedCostUsd) {
}
