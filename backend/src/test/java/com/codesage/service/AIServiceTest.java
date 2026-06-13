package com.codesage.service;

import com.codesage.ai.AIProvider;
import com.codesage.ai.AnalysisResult;
import com.codesage.ai.DeterministicAIProvider;
import com.codesage.exception.AIServiceException;
import com.codesage.model.Review;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AIServiceTest {

    @Test
    void fallsBackAfterProviderFailure() {
        AIProvider failing = mock(AIProvider.class);
        when(failing.isAvailable()).thenReturn(true);
        when(failing.name()).thenReturn("Failing");
        when(failing.analyze("clean diff")).thenThrow(new IllegalStateException("timeout"));
        SimpleMeterRegistry metrics = new SimpleMeterRegistry();
        AIService service = new AIService(List.of(failing, new DeterministicAIProvider(true)), metrics);

        Review review = service.analyzeCode("clean diff", "backend", 42, "Improve review");

        assertThat(review.getStatus()).isEqualTo(Review.ReviewStatus.COMPLETED);
        assertThat(review.getAiProvider()).isEqualTo("Demo");
        assertThat(metrics.get("codesage.ai.provider.fallback").counter().count()).isEqualTo(1);
    }

    @Test
    void failsWhenNoProviderIsAvailable() {
        AIProvider unavailable = mock(AIProvider.class);
        when(unavailable.isAvailable()).thenReturn(false);
        AIService service = new AIService(List.of(unavailable), new SimpleMeterRegistry());

        assertThatThrownBy(() -> service.analyzeCode("diff", "backend", 1, "title"))
                .isInstanceOf(AIServiceException.class)
                .hasMessageContaining("No AI provider");
    }
}
