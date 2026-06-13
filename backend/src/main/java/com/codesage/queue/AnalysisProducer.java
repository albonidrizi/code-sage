package com.codesage.queue;

import com.codesage.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class AnalysisProducer {

    private final RabbitTemplate rabbitTemplate;

    public AnalysisProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMandatory(true);
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("RabbitMQ rejected analysis message: {}", cause);
            }
        });
    }

    public void sendToQueue(Map<String, Object> payload) {
        log.info("Pushing event to queue: {}", payload.get("action"));
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ANALYSIS_ROUTING_KEY, payload);
    }
}
