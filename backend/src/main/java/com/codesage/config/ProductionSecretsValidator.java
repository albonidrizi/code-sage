package com.codesage.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("production")
public class ProductionSecretsValidator {
    private final List<String> requiredSecrets;

    public ProductionSecretsValidator(
            @Value("${github.webhook.secret:}") String webhookSecret,
            @Value("${spring.datasource.password:}") String databasePassword,
            @Value("${spring.rabbitmq.password:}") String rabbitPassword) {
        this.requiredSecrets = List.of(webhookSecret, databasePassword, rabbitPassword);
    }

    @PostConstruct
    void validate() {
        if (requiredSecrets.stream().anyMatch(value -> value == null || value.isBlank()
                || "postgres".equals(value) || "guest".equals(value))) {
            throw new IllegalStateException("Production profile requires non-default webhook, database, and queue secrets");
        }
    }
}
