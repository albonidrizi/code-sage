package com.codesage.service;

import com.codesage.model.WebhookDelivery;
import com.codesage.repository.WebhookDeliveryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookIdempotencyService {
    private final WebhookDeliveryRepository deliveries;

    public WebhookIdempotencyService(WebhookDeliveryRepository deliveries) {
        this.deliveries = deliveries;
    }

    @Transactional
    public boolean claim(String deliveryId) {
        if (deliveryId == null || deliveryId.isBlank()) {
            return false;
        }
        if (deliveries.existsById(deliveryId)) {
            return false;
        }
        try {
            deliveries.saveAndFlush(new WebhookDelivery(deliveryId));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }
}
