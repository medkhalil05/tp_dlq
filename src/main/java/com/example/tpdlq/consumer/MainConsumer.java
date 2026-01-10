package com.example.tpdlq.consumer;

import com.example.tpdlq.service.MessageProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MainConsumer {

    private static final Logger logger = LoggerFactory.getLogger(MainConsumer.class);

    @Autowired
    private MessageProducerService messageProducerService;

    @KafkaListener(topics = "${kafka.topic.input}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        logger.info("Received message from input topic: {}", message);
        
        try {
            // Validate message - valid messages contain the keyword "valid"
            if (isValidMessage(message)) {
                processValidMessage(message);
            } else {
                handleInvalidMessage(message);
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
            // Send to DLQ on exception
            messageProducerService.sendToDlqTopic(message);
        }
    }

    private boolean isValidMessage(String message) {
        return message != null && message.toLowerCase().contains("valid");
    }

    private void processValidMessage(String message) {
        logger.info("Processing valid message: {}", message);
        // Business logic for valid messages would go here
    }

    private void handleInvalidMessage(String message) {
        logger.warn("Invalid message detected, sending to DLQ: {}", message);
        messageProducerService.sendToDlqTopic(message);
    }
}
