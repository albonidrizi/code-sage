package com.codesage.ai;

import com.codesage.model.ReviewIssue;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteAIProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RemoteAIReviewParser parser = new RemoteAIReviewParser(objectMapper);

    @Test
    void openAiProviderParsesStructuredReviewAndSendsBearerToken() {
        AtomicReference<ClientRequest> requestRef = new AtomicReference<>();
        OpenAIProvider provider = new OpenAIProvider(
                webClient(response("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "{\\"qualityScore\\":8.2,\\"summary\\":\\"Remote review complete\\",\\"issues\\":[{\\"type\\":\\"SECURITY\\",\\"severity\\":\\"HIGH\\",\\"filePath\\":\\"src/App.java\\",\\"lineNumber\\":42,\\"title\\":\\"Unsafe query\\",\\"description\\":\\"Input reaches SQL\\",\\"suggestion\\":\\"Use parameters\\"}]}"
                              }
                            }
                          ],
                          "usage": {"prompt_tokens": 100, "completion_tokens": 20}
                        }
                        """), requestRef),
                parser,
                CircuitBreaker.ofDefaults("test-openai"),
                "openai-test-key",
                "gpt-test",
                Duration.ofSeconds(2),
                500,
                1,
                Duration.ofMillis(1),
                1.0,
                2.0);

        AnalysisResult result = provider.analyze("+ String sql = input;");

        assertThat(provider.isAvailable()).isTrue();
        assertThat(provider.name()).isEqualTo("OpenAI");
        assertThat(result.qualityScore()).isEqualTo(8.2);
        assertThat(result.summary()).isEqualTo("Remote review complete");
        assertThat(result.inputTokens()).isEqualTo(100);
        assertThat(result.outputTokens()).isEqualTo(20);
        assertThat(result.estimatedCostUsd()).isEqualTo(0.00014);
        assertThat(result.issues()).singleElement().satisfies(issue -> {
            assertThat(issue.type()).isEqualTo(ReviewIssue.IssueType.SECURITY);
            assertThat(issue.severity()).isEqualTo(ReviewIssue.IssueSeverity.HIGH);
            assertThat(issue.filePath()).isEqualTo("src/App.java");
        });
        assertThat(requestRef.get().url().getPath()).isEqualTo("/chat/completions");
        assertThat(requestRef.get().headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer openai-test-key");
    }

    @Test
    void claudeProviderParsesStructuredReviewAndSendsAnthropicHeaders() {
        AtomicReference<ClientRequest> requestRef = new AtomicReference<>();
        ClaudeProvider provider = new ClaudeProvider(
                webClient(response("""
                        {
                          "content": [
                            {
                              "type": "text",
                              "text": "```json\\n{\\"qualityScore\\":9.1,\\"summary\\":\\"Claude review complete\\",\\"issues\\":[]}\\n```"
                            }
                          ],
                          "usage": {"input_tokens": 90, "output_tokens": 15}
                        }
                        """), requestRef),
                parser,
                CircuitBreaker.ofDefaults("test-claude"),
                "claude-test-key",
                "claude-test",
                "2023-06-01",
                Duration.ofSeconds(2),
                500,
                1,
                Duration.ofMillis(1),
                3.0,
                15.0);

        AnalysisResult result = provider.analyze("+ return secure;");

        assertThat(provider.isAvailable()).isTrue();
        assertThat(provider.name()).isEqualTo("Claude");
        assertThat(result.qualityScore()).isEqualTo(9.1);
        assertThat(result.issues()).isEmpty();
        assertThat(result.estimatedCostUsd()).isEqualTo(0.000495);
        assertThat(requestRef.get().url().getPath()).isEqualTo("/v1/messages");
        assertThat(requestRef.get().headers().getFirst("x-api-key")).isEqualTo("claude-test-key");
        assertThat(requestRef.get().headers().getFirst("anthropic-version")).isEqualTo("2023-06-01");
    }

    @Test
    void parserDefaultsUnknownEnumValuesAndClampsScore() {
        AnalysisResult result = parser.parse("""
                prefix {"qualityScore":19,"summary":"ok","issues":[{"type":"weird","severity":"loud"}]} suffix
                """, 1, 1, 0.0);

        assertThat(result.qualityScore()).isEqualTo(10.0);
        assertThat(result.issues()).singleElement().satisfies(issue -> {
            assertThat(issue.type()).isEqualTo(ReviewIssue.IssueType.CODE_QUALITY);
            assertThat(issue.severity()).isEqualTo(ReviewIssue.IssueSeverity.MEDIUM);
            assertThat(issue.filePath()).isEqualTo("unknown");
        });
    }

    @Test
    void blankApiKeysMakeRemoteProvidersUnavailable() {
        WebClient webClient = webClient(response("{}"), new AtomicReference<>());

        OpenAIProvider openAI = new OpenAIProvider(webClient, parser, CircuitBreaker.ofDefaults("blank-openai"), "",
                "gpt-test", Duration.ofSeconds(1), 500, 1, Duration.ofMillis(1), 0, 0);
        ClaudeProvider claude = new ClaudeProvider(webClient, parser, CircuitBreaker.ofDefaults("blank-claude"), " ",
                "claude-test", "2023-06-01", Duration.ofSeconds(1), 500, 1, Duration.ofMillis(1), 0, 0);

        assertThat(openAI.isAvailable()).isFalse();
        assertThat(claude.isAvailable()).isFalse();
    }

    @Test
    void parserRejectsNonJsonReviewContent() {
        assertThatThrownBy(() -> parser.parse("plain text", 1, 1, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON object");
    }

    private WebClient webClient(String responseBody) {
        return webClient(responseBody, new AtomicReference<>());
    }

    private WebClient webClient(String responseBody, AtomicReference<ClientRequest> requestRef) {
        return WebClient.builder()
                .exchangeFunction(request -> {
                    requestRef.set(request);
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(responseBody)
                            .build());
                })
                .build();
    }

    private String response(String body) {
        return body;
    }
}
