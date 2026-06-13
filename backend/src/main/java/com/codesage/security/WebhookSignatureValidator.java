package com.codesage.security;

import com.codesage.exception.WebhookValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WebhookSignatureValidator {

    private final String secret;
    private final boolean required;

    public WebhookSignatureValidator(
            @Value("${github.webhook.secret:}") String secret,
            @Value("${github.webhook.signature-required:true}") boolean required) {
        this.secret = secret;
        this.required = required;
    }

    public void validate(String payload, String signature) {
        if (!required && secret.isBlank()) {
            return;
        }
        if (secret.isBlank()) {
            throw new WebhookValidationException("Webhook secret is not configured");
        }
        if (signature == null || !signature.startsWith("sha256=")) {
            throw new WebhookValidationException("Missing or malformed webhook signature");
        }

        byte[] expected = hmac(payload);
        byte[] provided;
        try {
            provided = HexFormat.of().parseHex(signature.substring("sha256=".length()));
        } catch (IllegalArgumentException exception) {
            throw new WebhookValidationException("Malformed webhook signature", exception);
        }
        if (!MessageDigest.isEqual(expected, provided)) {
            throw new WebhookValidationException("Invalid webhook signature");
        }
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new WebhookValidationException("Unable to validate webhook signature", exception);
        }
    }
}
