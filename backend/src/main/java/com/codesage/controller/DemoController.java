package com.codesage.controller;

import com.codesage.model.Review;
import com.codesage.service.ReviewOrchestrator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/demo")
public class DemoController {
    private final ReviewOrchestrator orchestrator;
    private final boolean enabled;
    private final AtomicInteger sequence = new AtomicInteger(1000);

    public DemoController(ReviewOrchestrator orchestrator, @Value("${codesage.demo.enabled:true}") boolean enabled) {
        this.orchestrator = orchestrator;
        this.enabled = enabled;
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createDemoReview() {
        if (!enabled) {
            throw new IllegalStateException("Demo mode is disabled");
        }
        int number = sequence.incrementAndGet();
        Review review = orchestrator.process(Map.of(
                "action", "opened",
                "pull_request", Map.of(
                        "number", number,
                        "title", "Demo: parameterize customer lookup",
                        "html_url", "https://example.invalid/codesage/demo/pull/" + number,
                        "user", Map.of("login", "demo-developer"),
                        "base", Map.of("repo", Map.of(
                                "name", "demo-service",
                                "owner", Map.of("login", "codesage"))))));
        return Map.of("id", review.getId(), "status", review.getStatus(), "qualityScore", review.getQualityScore(),
                "issuesFound", review.getIssues().size());
    }
}
