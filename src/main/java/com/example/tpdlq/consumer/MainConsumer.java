package com.example.tpdlq.consumer;

import com.example.tpdlq.model.ErrorCategory;
import com.example.tpdlq.model.Order;
import com.example.tpdlq.service.MessageProducerService;
import com.example.tpdlq.service.OrderValidator;
import com.example.tpdlq.service.ValidMessageStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class MainConsumer {

    private static final Logger logger = LoggerFactory.getLogger(MainConsumer.class);

    private final MessageProducerService messageProducerService;
    private final OrderValidator orderValidator;
    private final ValidMessageStore validMessageStore;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Counter processedCounter;
    private final Counter validCounter;
    private final Counter invalidCounter;
    private final Counter malformedCounter;

    public MainConsumer(MessageProducerService messageProducerService,
                        OrderValidator orderValidator,
                        ValidMessageStore validMessageStore,
                        MeterRegistry meterRegistry) {
        this.messageProducerService = messageProducerService;
        this.orderValidator = orderValidator;
        this.validMessageStore = validMessageStore;
        this.processedCounter = meterRegistry.counter("tpdlq_messages_processed_total");
        this.validCounter = meterRegistry.counter("tpdlq_messages_valid_total");
        this.invalidCounter = meterRegistry.counter("tpdlq_messages_invalid_total");
        this.malformedCounter = meterRegistry.counter("tpdlq_messages_malformed_total");
    }

    @KafkaListener(topics = "${kafka.topic.input}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        logger.info("Received message from input topic: {}", message);
        processedCounter.increment();
        
        try {
            // Try to parse as JSON
            Order order = objectMapper.readValue(message, Order.class);
            
            // Validate the order
            String validationError = orderValidator.validateOrder(order);
            if (validationError != null) {
                // Check if it's a malformed error (extra fields)
                ErrorCategory category = validationError.contains("Malformed JSON: unexpected fields") 
                    ? ErrorCategory.MALFORMED_ERROR 
                    : ErrorCategory.VALIDATION_ERROR;
                handleInvalidMessage(message, validationError, category);
            } else {
                processValidMessage(message, order);
                validCounter.increment();
            }
        } catch (IllegalArgumentException e) {
            logger.error("Validation error for message: {}", message, e);
            messageProducerService.sendToDlqTopic(message, "Validation error: " + e.getMessage(), ErrorCategory.VALIDATION_ERROR);
            invalidCounter.increment();
        } catch (Exception e) {
            logger.error("Error parsing message as JSON: {}", message, e);
            // Send malformed JSON to DLQ
            messageProducerService.sendToDlqTopic(message, "Malformed JSON: " + e.getMessage(), ErrorCategory.MALFORMED_ERROR);
            malformedCounter.increment();
        }
    }

    private void processValidMessage(String message, Order order) {
        logger.info("Processing valid order: {}", order);
        // Business logic for valid messages would go here
        validMessageStore.add(order, message);
    }

    private void handleInvalidMessage(String message, String reason, ErrorCategory category) {
        logger.warn("Invalid message detected: {}. Reason: {} (Category: {})", message, reason, category);
        messageProducerService.sendToDlqTopic(message, reason, category);
        invalidCounter.increment();
    }
}
