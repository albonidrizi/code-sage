package com.codesage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery {
    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false, updatable = false)
    private Instant receivedAt;

    protected WebhookDelivery() {
    }

    public WebhookDelivery(String id) {
        this.id = id;
    }

    @PrePersist
    void onCreate() {
        receivedAt = Instant.now();
    }
}
