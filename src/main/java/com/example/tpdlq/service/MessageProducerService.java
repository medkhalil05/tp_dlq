package com.example.tpdlq.service;

import com.example.tpdlq.model.ErrorCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageProducerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerService.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.input}")
    private String inputTopic;

    @Value("${kafka.topic.dlq}")
    private String dlqTopic;

    public void sendToInputTopic(String message) {
        logger.info("Sending message to input topic {}: {}", inputTopic, message);
        kafkaTemplate.send(inputTopic, message);
    }

    public void sendToDlqTopic(String message) {
        logger.warn("Sending message to DLQ topic {}: {}", dlqTopic, message);
        kafkaTemplate.send(dlqTopic, message);
    }

    public void sendToDlqTopic(String message, String reason) {
        sendToDlqTopic(message, reason, ErrorCategory.UNKNOWN_ERROR);
    }

    public void sendToDlqTopic(String message, String reason, ErrorCategory category) {
        String dlqMessage = String.format("{\"reason\":\"%s\",\"originalMessage\":%s,\"category\":\"%s\"}", 
                reason, message, category.getDisplayName());
        logger.warn("Sending message to DLQ topic {} with reason: {} (Category: {})", dlqTopic, reason, category);
        kafkaTemplate.send(dlqTopic, dlqMessage);
    }
}
