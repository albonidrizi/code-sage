package com.codesage.security;

import com.codesage.exception.WebhookValidationException;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookSignatureValidatorTest {
    private static final String SECRET = "test-secret";
    private static final String PAYLOAD = "{\"action\":\"opened\"}";

    @Test
    void acceptsValidSignature() throws Exception {
        WebhookSignatureValidator validator = new WebhookSignatureValidator(SECRET, true);

        assertThatCode(() -> validator.validate(PAYLOAD, signature(PAYLOAD))).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingMalformedAndInvalidSignatures() {
        WebhookSignatureValidator validator = new WebhookSignatureValidator(SECRET, true);

        assertThatThrownBy(() -> validator.validate(PAYLOAD, null)).isInstanceOf(WebhookValidationException.class);
        assertThatThrownBy(() -> validator.validate(PAYLOAD, "sha256=not-hex"))
                .isInstanceOf(WebhookValidationException.class);
        assertThatThrownBy(() -> validator.validate(PAYLOAD, "sha256=00"))
                .isInstanceOf(WebhookValidationException.class);
    }

    @Test
    void allowsUnsignedPayloadOnlyWhenExplicitlyOptional() {
        WebhookSignatureValidator optional = new WebhookSignatureValidator("", false);
        WebhookSignatureValidator required = new WebhookSignatureValidator("", true);

        assertThatCode(() -> optional.validate(PAYLOAD, null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> required.validate(PAYLOAD, null)).isInstanceOf(WebhookValidationException.class);
    }

    private String signature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
