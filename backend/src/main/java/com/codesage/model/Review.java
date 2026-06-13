package com.codesage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a code review performed on a Pull Request.
 * Stores the analysis results, score, and metadata.
 */
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_repo_pr", columnNames = {"repository_owner", "repository_name", "pr_number"})
}, indexes = {
        @Index(name = "idx_repo_pr", columnList = "repository_owner,repository_name,pr_number"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String repositoryOwner;

    @Column(nullable = false)
    private String repositoryName;

    @Column(nullable = false)
    private Integer prNumber;

    @Column(nullable = false, length = 500)
    private String prTitle;

    @Column(nullable = false)
    private String prAuthor;

    @Column(length = 2000)
    private String prUrl;

    /**
     * Code quality score from 0.0 to 10.0
     */
    @Column(nullable = false)
    private Double qualityScore;

    /**
     * Overall analysis summary
     */
    @Column(columnDefinition = "TEXT")
    private String analysisSummary;

    /**
     * AI provider used (OpenAI, Claude, etc.)
     */
    @Column(nullable = false)
    private String aiProvider;

    /**
     * Model used (gpt-4, claude-3-opus, etc.)
     */
    @Column(nullable = false)
    private String aiModel;

    /**
     * Review status: PENDING, COMPLETED, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    /**
     * Issues found during the review
     */
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReviewIssue> issues = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Error message if review failed
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Helper method to add an issue to the review
     */
    public void addIssue(ReviewIssue issue) {
        issues.add(issue);
        issue.setReview(this);
    }

    /**
     * Get full repository name (owner/name)
     */
    public String getFullRepositoryName() {
        return repositoryOwner + "/" + repositoryName;
    }

    public enum ReviewStatus {
        PENDING,
        COMPLETED,
        FAILED
    }
}
