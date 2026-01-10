package com.example.tpdlq.service;

import com.example.tpdlq.model.ErrorCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class MessageProducerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerService.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.input}")
    private String inputTopic;

    @Value("${kafka.topic.dlq}")
    private String dlqTopic;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("reason", reason);
            // Store original message as a JSON string to guarantee valid DLQ payload
            root.put("originalMessage", message);
            // Use enum name to align with consumer parsing logic
            root.put("category", category.name());

            String dlqMessage = objectMapper.writeValueAsString(root);
            logger.warn("Sending message to DLQ topic {} with reason: {} (Category: {})", dlqTopic, reason, category.name());
            kafkaTemplate.send(dlqTopic, dlqMessage);
        } catch (Exception e) {
            logger.error("Failed to build DLQ message JSON. Falling back to raw. Error: {}", e.getMessage());
            kafkaTemplate.send(dlqTopic, String.format("{\"reason\":\"%s\",\"originalMessage\":\"%s\",\"category\":\"%s\"}",
                    reason, message.replace("\"", "\\\""), category.name()));
        }
    }
}
