package com.example.tpdlq.consumer;

import com.example.tpdlq.model.Order;
import com.example.tpdlq.service.MessageProducerService;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${kafka.topic.input}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        logger.info("Received message from input topic: {}", message);
        
        try {
            // Try to parse as JSON
            Order order = objectMapper.readValue(message, Order.class);
            
            // Validate the order
            String validationError = validateOrder(order);
            if (validationError != null) {
                handleInvalidMessage(message, validationError);
            } else {
                processValidMessage(message, order);
            }
        } catch (Exception e) {
            logger.error("Error parsing message as JSON: {}", message, e);
            // Send malformed JSON to DLQ
            messageProducerService.sendToDlqTopic(message, "Malformed JSON: " + e.getMessage());
        }
    }

    private String validateOrder(Order order) {
        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            return "Missing required field: orderId";
        }
        if (order.getUserId() == null || order.getUserId().trim().isEmpty()) {
            return "Missing required field: userId";
        }
        if (order.getAmount() == null) {
            return "Missing required field: amount";
        }
        if (order.getAmount() <= 0) {
            return "Invalid amount: must be greater than 0";
        }
        return null; // Valid
    }

    private void processValidMessage(String message, Order order) {
        logger.info("Processing valid order: {}", order);
        // Business logic for valid messages would go here
    }

    private void handleInvalidMessage(String message, String reason) {
        logger.warn("Invalid message detected: {}. Reason: {}", message, reason);
        messageProducerService.sendToDlqTopic(message, reason);
    }
}
