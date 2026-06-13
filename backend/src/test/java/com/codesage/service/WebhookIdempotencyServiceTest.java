package com.codesage.service;

import com.codesage.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebhookIdempotencyServiceTest {

    @Test
    void claimsNewDeliveryAndRejectsMissingOrExistingDelivery() {
        WebhookDeliveryRepository repository = mock(WebhookDeliveryRepository.class);
        WebhookIdempotencyService service = new WebhookIdempotencyService(repository);

        when(repository.existsById("new")).thenReturn(false);
        assertThat(service.claim("new")).isTrue();
        when(repository.existsById("existing")).thenReturn(true);
        assertThat(service.claim("existing")).isFalse();
        assertThat(service.claim("")).isFalse();
    }

    @Test
    void treatsConcurrentConstraintViolationAsDuplicate() {
        WebhookDeliveryRepository repository = mock(WebhookDeliveryRepository.class);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThat(new WebhookIdempotencyService(repository).claim("racing-delivery")).isFalse();
    }
}
