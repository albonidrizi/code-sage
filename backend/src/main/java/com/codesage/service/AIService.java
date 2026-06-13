package com.codesage.service;

import com.codesage.ai.AIProvider;
import com.codesage.ai.AnalysisResult;
import com.codesage.exception.AIServiceException;
import com.codesage.model.Review;
import com.codesage.model.ReviewIssue;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AIService {
    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    private final List<AIProvider> providers;
    private final MeterRegistry metrics;
    private final Timer analysisTimer;

    public AIService(List<AIProvider> providers, MeterRegistry metrics) {
        this.providers = providers;
        this.metrics = metrics;
        this.analysisTimer = metrics.timer("codesage.ai.analysis.duration");
    }

    public Review analyzeCode(String codeDiff, String repositoryName, Integer prNumber, String prTitle) {
        return analysisTimer.record(() -> analyze(codeDiff, repositoryName, prNumber, prTitle));
    }

    private Review analyze(String codeDiff, String repositoryName, Integer prNumber, String prTitle) {
        AIServiceException lastFailure = null;
        for (AIProvider provider : providers) {
            if (!provider.isAvailable()) {
                continue;
            }
            try {
                AnalysisResult result = provider.analyze(codeDiff);
                metrics.counter("codesage.ai.provider.success", "provider", provider.name()).increment();
                metrics.counter("codesage.ai.tokens", "provider", provider.name(), "type", "input")
                        .increment(result.inputTokens());
                metrics.counter("codesage.ai.tokens", "provider", provider.name(), "type", "output")
                        .increment(result.outputTokens());
                metrics.counter("codesage.ai.estimated.cost.usd", "provider", provider.name())
                        .increment(result.estimatedCostUsd());
                return toReview(result, provider, repositoryName, prNumber, prTitle);
            } catch (RuntimeException exception) {
                log.warn("AI provider {} failed; trying the next configured provider", provider.name());
                metrics.counter("codesage.ai.provider.failure", "provider", provider.name()).increment();
                metrics.counter("codesage.ai.provider.fallback", "provider", provider.name()).increment();
                lastFailure = new AIServiceException("AI provider failed: " + provider.name(), exception);
            }
        }
        throw lastFailure != null ? lastFailure : new AIServiceException("No AI provider is available");
    }

    private Review toReview(AnalysisResult result, AIProvider provider, String repositoryName, Integer prNumber,
            String prTitle) {
        Review review = Review.builder()
                .repositoryOwner("unknown")
                .repositoryName(repositoryName)
                .prNumber(prNumber)
                .prTitle(prTitle)
                .prAuthor("unknown")
                .qualityScore(result.qualityScore())
                .analysisSummary(result.summary())
                .aiProvider(provider.name())
                .aiModel(provider.model())
                .status(Review.ReviewStatus.COMPLETED)
                .build();
        result.issues().forEach(finding -> review.addIssue(ReviewIssue.builder()
                .type(finding.type())
                .severity(finding.severity())
                .filePath(finding.filePath())
                .lineNumber(finding.lineNumber())
                .title(finding.title())
                .description(finding.description())
                .suggestion(finding.suggestion())
                .build()));
        return review;
    }
}
