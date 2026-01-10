package com.example.tpdlq.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DlqConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DlqConsumer.class);

    @KafkaListener(topics = "${kafka.topic.dlq}", groupId = "${spring.kafka.consumer.group-id}-dlq")
    public void consumeFromDlq(String message) {
        logger.error("DLQ Consumer - Received error message: {}", message);
        // Monitor and handle error messages from DLQ
        // This could involve alerting, manual review, or custom error handling
    }
}
