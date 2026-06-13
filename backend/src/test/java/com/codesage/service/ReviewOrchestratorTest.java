package com.codesage.service;

import com.codesage.ai.DeterministicAIProvider;
import com.codesage.github.GitHubOperations;
import com.codesage.model.Review;
import com.codesage.repository.ReviewRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewOrchestratorTest {

    @Test
    void completesReviewAndPreventsDuplicateWork() {
        ReviewRepository repository = mock(ReviewRepository.class);
        GitHubOperations github = mock(GitHubOperations.class);
        SimpleMeterRegistry metrics = new SimpleMeterRegistry();
        AIService ai = new AIService(java.util.List.of(new DeterministicAIProvider(true)), metrics);
        ReviewOrchestrator orchestrator = new ReviewOrchestrator(ai, github, repository, metrics);
        AtomicReference<Review> stored = new AtomicReference<>();
        when(repository.existsByRepositoryOwnerAndRepositoryNameAndPrNumber("codesage", "backend", 42))
                .thenAnswer(invocation -> stored.get() != null);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(1L);
            stored.set(review);
            return review;
        });
        when(repository.save(any())).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            stored.set(review);
            return review;
        });
        when(repository.findByRepositoryOwnerAndRepositoryNameAndPrNumber("codesage", "backend", 42))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(github.fetchPullRequestDiff("codesage", "backend", 42)).thenReturn("SELECT * + input");
        when(github.formatReviewComment(any())).thenReturn("review");

        Review first = orchestrator.process(payload("opened"));
        Review duplicate = orchestrator.process(payload("opened"));

        assertThat(first.getStatus()).isEqualTo(Review.ReviewStatus.COMPLETED);
        assertThat(first.getIssues()).hasSize(1);
        assertThat(duplicate).isSameAs(first);
        verify(github).postReviewComment("codesage", "backend", 42, "review");
    }

    @Test
    void rejectsUnsupportedActionsBeforeExternalWork() {
        ReviewRepository repository = mock(ReviewRepository.class);
        GitHubOperations github = mock(GitHubOperations.class);
        ReviewOrchestrator orchestrator = new ReviewOrchestrator(
                new AIService(java.util.List.of(new DeterministicAIProvider(true)), new SimpleMeterRegistry()),
                github, repository, new SimpleMeterRegistry());

        assertThatThrownBy(() -> orchestrator.process(payload("closed"))).isInstanceOf(IllegalArgumentException.class);
        verify(github, never()).fetchPullRequestDiff(any(), any(), any(Integer.class));
    }

    private Map<String, Object> payload(String action) {
        return Map.of("action", action, "pull_request", Map.of(
                "number", 42,
                "title", "Improve reviews",
                "html_url", "https://example.invalid/pr/42",
                "user", Map.of("login", "albon"),
                "base", Map.of("repo", Map.of("name", "backend", "owner", Map.of("login", "codesage")))));
    }
}
