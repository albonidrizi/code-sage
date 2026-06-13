package com.codesage.service;

import com.codesage.model.Review;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class GitHubServiceTest {

    @Test
    void usesSafeDemoOperationsWhenGitHubConfigurationIsBlank() {
        GitHubService service = new GitHubService(WebClient.builder(), new ObjectMapper());
        ReflectionTestUtils.setField(service, "appId", "");
        ReflectionTestUtils.setField(service, "privateKeyPath", "");
        ReflectionTestUtils.setField(service, "installationId", "");

        assertThat(service.fetchPullRequestDiff("codesage", "demo", 1)).contains("PreparedStatement");
        assertThatCode(() -> service.postReviewComment("codesage", "demo", 1, "review")).doesNotThrowAnyException();
    }

    @Test
    void formatsReviewWithoutEncodingSensitiveEmoji() {
        GitHubService service = new GitHubService(WebClient.builder(), new ObjectMapper());
        Review review = Review.builder()
                .repositoryOwner("codesage")
                .repositoryName("demo")
                .prNumber(1)
                .prTitle("Demo")
                .prAuthor("developer")
                .qualityScore(9.5)
                .analysisSummary("Looks good")
                .aiProvider("Demo")
                .aiModel("deterministic-v1")
                .status(Review.ReviewStatus.COMPLETED)
                .build();

        assertThat(service.formatReviewComment(review)).contains("CodeSage Review", "9.5/10").doesNotContain("🤖");
    }
}
