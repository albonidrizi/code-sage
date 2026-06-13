package com.codesage.queue;

import com.codesage.config.RabbitMQConfig;
import com.codesage.service.ReviewOrchestrator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AnalysisConsumer {
    private final ReviewOrchestrator orchestrator;
    private final Timer processingTimer;

    public AnalysisConsumer(ReviewOrchestrator orchestrator, MeterRegistry metrics) {
        this.orchestrator = orchestrator;
        this.processingTimer = metrics.timer("codesage.queue.processing.duration");
    }

    @RabbitListener(queues = RabbitMQConfig.ANALYSIS_QUEUE)
    public void handleAnalysisRequest(Map<String, Object> payload) {
        processingTimer.record(() -> orchestrator.process(payload));
    }
}
