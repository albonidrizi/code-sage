package com.codesage.service;

import com.codesage.exception.GitHubApiException;
import com.codesage.github.GitHubOperations;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * Service for GitHub API integration.
 * Handles authentication, fetching PR diffs, and posting review comments.
 */
@Service
@Slf4j
public class GitHubService implements GitHubOperations {

    private final WebClient githubClient;
    private final ObjectMapper objectMapper;

    @Value("${github.app.id:#{null}}")
    private String appId;

    @Value("${github.app.private-key-path:#{null}}")
    private String privateKeyPath;

    @Value("${github.installation.id:#{null}}")
    private String installationId;

    private String cachedInstallationToken;
    private Instant tokenExpiry;

    public GitHubService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.githubClient = webClientBuilder
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .defaultHeader(HttpHeaders.USER_AGENT, "CodeSage-Bot")
                .build();
    }

    /**
     * Fetch PR diff content from GitHub
     */
    public String fetchPRDiff(String owner, String repo, int prNumber) {
        log.info("Fetching PR diff for {}/{} #{}", owner, repo, prNumber);

        if (appId == null || privateKeyPath == null) {
            log.warn("GitHub App not configured. Returning mock diff.");
            return getMockDiff();
        }

        try {
            String token = getInstallationToken();

            String diff = githubClient.get()
                    .uri("/repos/{owner}/{repo}/pulls/{pr_number}", owner, repo, prNumber)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3.diff")
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                    .timeout(Duration.ofSeconds(15))
                    .block();

            log.info("Successfully fetched diff for PR #{}", prNumber);
            return diff;
        } catch (WebClientResponseException e) {
            log.error("GitHub API error fetching diff: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GitHubApiException("Failed to fetch PR diff", e.getStatusCode().value(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching PR diff", e);
            throw new GitHubApiException("Failed to fetch PR diff: " + e.getMessage(), e);
        }
    }

    @Override
    public String fetchPullRequestDiff(String owner, String repository, int pullRequestNumber) {
        return fetchPRDiff(owner, repository, pullRequestNumber);
    }

    /**
     * Post review comment to GitHub PR
     */
    public void postComment(String owner, String repo, int prNumber, String comment) {
        log.info("Posting comment to PR {}/{} #{}", owner, repo, prNumber);

        if (appId == null || privateKeyPath == null) {
            log.warn("GitHub App not configured. Logging comment instead:");
            log.info("Comment for PR #{}: {}", prNumber, comment);
            return;
        }

        try {
            String token = getInstallationToken();

            Map<String, String> requestBody = Map.of("body", comment);

            githubClient.post()
                    .uri("/repos/{owner}/{repo}/issues/{issue_number}/comments", owner, repo, prNumber)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                    .timeout(Duration.ofSeconds(15))
                    .block();

            log.info("Successfully posted comment to PR #{}", prNumber);
        } catch (WebClientResponseException e) {
            log.error("GitHub API error posting comment: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GitHubApiException("Failed to post comment", e.getStatusCode().value(), e);
        } catch (Exception e) {
            log.error("Unexpected error posting comment", e);
            throw new GitHubApiException("Failed to post comment: " + e.getMessage(), e);
        }
    }

    @Override
    public void postReviewComment(String owner, String repository, int pullRequestNumber, String comment) {
        postComment(owner, repository, pullRequestNumber, comment);
    }

    /**
     * Get GitHub App installation token (with caching)
     */
    private String getInstallationToken() {
        // Return cached token if still valid
        if (cachedInstallationToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            log.debug("Using cached installation token");
            return cachedInstallationToken;
        }

        log.info("Generating new installation token");

        try {
            // Generate JWT for GitHub App authentication
            String jwt = generateJWT();

            // Exchange JWT for installation token
            String response = githubClient.post()
                    .uri("/app/installations/{installation_id}/access_tokens", installationId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            cachedInstallationToken = root.path("token").asText();

            // Tokens expire in 1 hour, cache for 50 minutes to be safe
            tokenExpiry = Instant.now().plus(Duration.ofMinutes(50));

            log.info("Successfully generated installation token");
            return cachedInstallationToken;
        } catch (Exception e) {
            log.error("Failed to get installation token", e);
            throw new GitHubApiException("Failed to authenticate with GitHub", e);
        }
    }

    /**
     * Generate JWT for GitHub App authentication
     */
    private String generateJWT() {
        try {
            // Read private key
            PrivateKey privateKey = loadPrivateKey(privateKeyPath);

            // Generate JWT (valid for 10 minutes)
            Instant now = Instant.now();
            Date issuedAt = Date.from(now);
            Date expiresAt = Date.from(now.plus(Duration.ofMinutes(10)));

            return Jwts.builder()
                    .setIssuer(appId)
                    .setIssuedAt(issuedAt)
                    .setExpiration(expiresAt)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();
        } catch (Exception e) {
            log.error("Failed to generate JWT", e);
            throw new GitHubApiException("Failed to generate JWT for GitHub App", e);
        }
    }

    /**
     * Load private key from PEM file
     */
    private PrivateKey loadPrivateKey(String path) throws Exception {
        String key = new String(Files.readAllBytes(Paths.get(path)));

        // Remove PEM headers and whitespace
        String privateKeyPEM = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Mock diff for development without GitHub App
     */
    private String getMockDiff() {
        return """
                diff --git a/src/main/java/Example.java b/src/main/java/Example.java
                index 1234567..abcdefg 100644
                --- a/src/main/java/Example.java
                +++ b/src/main/java/Example.java
                @@ -10,7 +10,7 @@ public class Example {
                     public void processData(String input) {
                -        String query = "SELECT * FROM users WHERE name = '" + input + "'";
                +        String query = "SELECT * FROM users WHERE name = ?";
                +        PreparedStatement stmt = connection.prepareStatement(query);
                +        stmt.setString(1, input);
                     }
                 }
                """;
    }

    /**
     * Format review as GitHub comment
     */
    @Override
    public String formatReviewComment(com.codesage.model.Review review) {
        StringBuilder comment = new StringBuilder();

        comment.append("## CodeSage Review\n\n");
        comment.append(String.format("**Quality Score:** %.1f/10\n\n", review.getQualityScore()));
        comment.append(String.format("**Summary:** %s\n\n", review.getAnalysisSummary()));

        if (!review.getIssues().isEmpty()) {
            comment.append("### Issues Found\n\n");

            for (com.codesage.model.ReviewIssue issue : review.getIssues()) {
                comment.append(String.format("**%s** - %s\n", issue.getSeverity(), issue.getTitle()));
                comment.append(String.format("- **File:** `%s`", issue.getFilePath()));
                if (issue.getLineNumber() != null) {
                    comment.append(String.format(" (Line %d)", issue.getLineNumber()));
                }
                comment.append("\n");
                comment.append(String.format("- **Description:** %s\n", issue.getDescription()));
                if (issue.getSuggestion() != null && !issue.getSuggestion().isEmpty()) {
                    comment.append(String.format("- **Suggestion:** %s\n", issue.getSuggestion()));
                }
                comment.append("\n");
            }
        }

        comment.append("\n---\n");
        comment.append(String.format("*Powered by %s (%s)*", review.getAiProvider(), review.getAiModel()));

        return comment.toString();
    }
}
