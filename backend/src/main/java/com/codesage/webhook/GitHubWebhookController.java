package com.codesage.webhook;

import com.codesage.queue.AnalysisProducer;
import com.codesage.security.WebhookSignatureValidator;
import com.codesage.service.WebhookIdempotencyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class GitHubWebhookController {
    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final AnalysisProducer analysisProducer;
    private final WebhookSignatureValidator signatureValidator;
    private final ObjectMapper objectMapper;
    private final WebhookIdempotencyService idempotencyService;

    public GitHubWebhookController(AnalysisProducer analysisProducer, WebhookSignatureValidator signatureValidator,
            ObjectMapper objectMapper, WebhookIdempotencyService idempotencyService) {
        this.analysisProducer = analysisProducer;
        this.signatureValidator = signatureValidator;
        this.objectMapper = objectMapper;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/github")
    public ResponseEntity<Map<String, String>> handleGitHubEvent(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload) throws Exception {
        signatureValidator.validate(rawPayload, signature);
        if (!idempotencyService.claim(deliveryId)) {
            return ResponseEntity.ok(Map.of("status", "duplicate", "event", eventType));
        }
        if (!"pull_request".equals(eventType) && !"ping".equals(eventType)) {
            return ResponseEntity.accepted().body(Map.of("status", "ignored", "event", eventType));
        }
        Map<String, Object> payload = objectMapper.readValue(rawPayload, new TypeReference<>() {
        });
        log.info("Accepted GitHub event type={}", eventType);
        if ("pull_request".equals(eventType)) {
            analysisProducer.sendToQueue(payload);
        }
        return ResponseEntity.accepted().body(Map.of("status", "accepted", "event", eventType));
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of("status", "running", "service", "CodeSage Webhook Handler", "timestamp", Instant.now());
    }
}
