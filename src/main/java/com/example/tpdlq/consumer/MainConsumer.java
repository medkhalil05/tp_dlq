package com.example.tpdlq.consumer;

import com.example.tpdlq.model.ErrorCategory;
import com.example.tpdlq.model.Order;
import com.example.tpdlq.service.MessageProducerService;
import com.example.tpdlq.service.OrderValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private OrderValidator orderValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${kafka.topic.input}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        logger.info("Received message from input topic: {}", message);
        
        try {
            // Try to parse as JSON
            Order order = objectMapper.readValue(message, Order.class);
            
            // Validate the order
            String validationError = orderValidator.validateOrder(order);
            if (validationError != null) {
                handleInvalidMessage(message, validationError, ErrorCategory.VALIDATION_ERROR);
            } else {
                processValidMessage(message, order);
            }
        } catch (IllegalArgumentException e) {
            logger.error("Validation error for message: {}", message, e);
            messageProducerService.sendToDlqTopic(message, "Validation error: " + e.getMessage(), ErrorCategory.VALIDATION_ERROR);
        } catch (Exception e) {
            logger.error("Error parsing message as JSON: {}", message, e);
            // Send malformed JSON to DLQ
            messageProducerService.sendToDlqTopic(message, "Malformed JSON: " + e.getMessage(), ErrorCategory.MALFORMED_ERROR);
        }
    }

    private void processValidMessage(String message, Order order) {
        logger.info("Processing valid order: {}", order);
        // Business logic for valid messages would go here
    }

    private void handleInvalidMessage(String message, String reason, ErrorCategory category) {
        logger.warn("Invalid message detected: {}. Reason: {} (Category: {})", message, reason, category);
        messageProducerService.sendToDlqTopic(message, reason, category);
    }
}
