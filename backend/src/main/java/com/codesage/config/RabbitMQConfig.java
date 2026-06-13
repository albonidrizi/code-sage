package com.codesage.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "codesage.review";
    public static final String ANALYSIS_QUEUE = "codesage.analysis.queue";
    public static final String RETRY_QUEUE = "codesage.analysis.retry";
    public static final String DEAD_LETTER_QUEUE = "codesage.analysis.dlq";
    public static final String ANALYSIS_ROUTING_KEY = "analysis";
    public static final String RETRY_ROUTING_KEY = "analysis.retry";
    public static final String DEAD_LETTER_ROUTING_KEY = "analysis.dead";

    @Bean
    DirectExchange reviewExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue analysisQueue() {
        return QueueBuilder.durable(ANALYSIS_QUEUE)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(RETRY_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue retryQueue() {
        return QueueBuilder.durable(RETRY_QUEUE)
                .ttl(10_000)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding analysisBinding(DirectExchange reviewExchange) {
        return BindingBuilder.bind(analysisQueue()).to(reviewExchange).with(ANALYSIS_ROUTING_KEY);
    }

    @Bean
    Binding retryBinding(DirectExchange reviewExchange) {
        return BindingBuilder.bind(retryQueue()).to(reviewExchange).with(RETRY_ROUTING_KEY);
    }

    @Bean
    Binding deadLetterBinding(DirectExchange reviewExchange) {
        return BindingBuilder.bind(deadLetterQueue()).to(reviewExchange).with(DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    RabbitTemplate.ConfirmCallback publisherConfirmCallback() {
        return (correlationData, ack, cause) -> {
            if (!ack) {
                throw new IllegalStateException("RabbitMQ rejected published analysis message");
            }
        };
    }
}
