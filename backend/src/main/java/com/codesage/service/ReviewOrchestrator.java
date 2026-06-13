package com.codesage.service;

import com.codesage.github.GitHubOperations;
import com.codesage.model.Review;
import com.codesage.repository.ReviewRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ReviewOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(ReviewOrchestrator.class);

    private final AIService aiService;
    private final GitHubOperations github;
    private final ReviewRepository reviews;
    private final MeterRegistry metrics;
    private final Timer reviewTimer;

    public ReviewOrchestrator(AIService aiService, GitHubOperations github, ReviewRepository reviews,
            MeterRegistry metrics) {
        this.aiService = aiService;
        this.github = github;
        this.reviews = reviews;
        this.metrics = metrics;
        this.reviewTimer = metrics.timer("codesage.review.duration");
    }

    @Transactional
    public Review process(Map<String, Object> payload) {
        return reviewTimer.record(() -> processReview(PullRequestEvent.from(payload)));
    }

    private Review processReview(PullRequestEvent event) {
        if (!event.isReviewable()) {
            throw new IllegalArgumentException("Unsupported pull request action: " + event.action());
        }
        if (reviews.existsByRepositoryOwnerAndRepositoryNameAndPrNumber(event.owner(), event.repository(),
                event.number())) {
            metrics.counter("codesage.review.duplicate").increment();
            return reviews.findByRepositoryOwnerAndRepositoryNameAndPrNumber(event.owner(), event.repository(),
                    event.number()).orElseThrow();
        }

        Review review = Review.builder()
                .repositoryOwner(event.owner())
                .repositoryName(event.repository())
                .prNumber(event.number())
                .prTitle(event.title())
                .prAuthor(event.author())
                .prUrl(event.url())
                .status(Review.ReviewStatus.PENDING)
                .qualityScore(0.0)
                .aiProvider("Pending")
                .aiModel("Pending")
                .build();
        try {
            review = reviews.saveAndFlush(review);
        } catch (DataIntegrityViolationException duplicate) {
            metrics.counter("codesage.review.duplicate").increment();
            return reviews.findByRepositoryOwnerAndRepositoryNameAndPrNumber(event.owner(), event.repository(),
                    event.number()).orElseThrow(() -> duplicate);
        }

        try {
            String diff = github.fetchPullRequestDiff(event.owner(), event.repository(), event.number());
            Review analyzed = aiService.analyzeCode(diff, event.repository(), event.number(), event.title());
            review.setQualityScore(analyzed.getQualityScore());
            review.setAnalysisSummary(analyzed.getAnalysisSummary());
            review.setAiProvider(analyzed.getAiProvider());
            review.setAiModel(analyzed.getAiModel());
            review.setStatus(Review.ReviewStatus.COMPLETED);
            analyzed.getIssues().forEach(review::addIssue);
            Review completed = reviews.save(review);
            github.postReviewComment(event.owner(), event.repository(), event.number(),
                    github.formatReviewComment(completed));
            metrics.counter("codesage.review.completed").increment();
            return completed;
        } catch (RuntimeException failure) {
            log.error("Review processing failed for {}/{} #{}", event.owner(), event.repository(), event.number());
            review.setStatus(Review.ReviewStatus.FAILED);
            review.setErrorMessage("Review processing failed");
            metrics.counter("codesage.review.failed").increment();
            reviews.save(review);
            throw failure;
        }
    }

    record PullRequestEvent(String action, int number, String title, String author, String owner, String repository,
            String url) {
        boolean isReviewable() {
            return "opened".equals(action) || "synchronize".equals(action);
        }

        static PullRequestEvent from(Map<String, Object> payload) {
            Map<String, Object> pr = requiredMap(payload, "pull_request");
            Map<String, Object> user = requiredMap(pr, "user");
            Map<String, Object> base = requiredMap(pr, "base");
            Map<String, Object> repo = requiredMap(base, "repo");
            Map<String, Object> owner = requiredMap(repo, "owner");
            return new PullRequestEvent(
                    String.valueOf(payload.get("action")),
                    ((Number) pr.get("number")).intValue(),
                    String.valueOf(pr.get("title")),
                    String.valueOf(user.get("login")),
                    String.valueOf(owner.get("login")),
                    String.valueOf(repo.get("name")),
                    String.valueOf(pr.get("html_url")));
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> requiredMap(Map<String, Object> source, String key) {
            Object value = source.get(key);
            if (!(value instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("Missing required webhook field: " + key);
            }
            return (Map<String, Object>) value;
        }
    }
}
