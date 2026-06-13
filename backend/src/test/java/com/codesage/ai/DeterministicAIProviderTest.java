package com.codesage.ai;

import com.codesage.model.ReviewIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicAIProviderTest {

    @Test
    void reportsSecurityAndQualityFindingsDeterministically() {
        DeterministicAIProvider provider = new DeterministicAIProvider(true);

        AnalysisResult result = provider.analyze("SELECT * FROM users WHERE name = '\" + input; // TODO");

        assertThat(provider.isAvailable()).isTrue();
        assertThat(provider.name()).isEqualTo("Demo");
        assertThat(provider.model()).isEqualTo("deterministic-v1");
        assertThat(result.qualityScore()).isEqualTo(6.5);
        assertThat(result.issues()).extracting(IssueFinding::severity)
                .containsExactly(ReviewIssue.IssueSeverity.HIGH, ReviewIssue.IssueSeverity.MEDIUM);
        assertThat(result.inputTokens()).isPositive();
        assertThat(result.estimatedCostUsd()).isZero();
    }

    @Test
    void returnsCleanResultAndCanBeDisabled() {
        DeterministicAIProvider provider = new DeterministicAIProvider(false);

        AnalysisResult result = provider.analyze("return customerRepository.findById(id);");

        assertThat(provider.isAvailable()).isFalse();
        assertThat(result.qualityScore()).isEqualTo(9.5);
        assertThat(result.issues()).isEmpty();
    }
}
