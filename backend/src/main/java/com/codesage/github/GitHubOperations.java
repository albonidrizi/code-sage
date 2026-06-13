package com.codesage.github;

import com.codesage.model.Review;

public interface GitHubOperations {
    String fetchPullRequestDiff(String owner, String repository, int pullRequestNumber);
    void postReviewComment(String owner, String repository, int pullRequestNumber, String comment);
    String formatReviewComment(Review review);
}
