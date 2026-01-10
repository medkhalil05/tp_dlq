package com.example.tpdlq.consumer;

import com.example.tpdlq.model.DlqMessage;
import com.example.tpdlq.model.ErrorCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DlqConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DlqConsumer.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Thread-safe list to store DLQ messages for display
    private final List<DlqMessage> dlqMessages = new CopyOnWriteArrayList<>();

    private final Counter dlqCounter;
    private final Counter validationCounter;
    private final Counter malformedCounter;
    private final Counter unknownCounter;

    public DlqConsumer(MeterRegistry meterRegistry) {
        this.dlqCounter = meterRegistry.counter("tpdlq_dlq_total");
        this.validationCounter = meterRegistry.counter("tpdlq_dlq_category_total", "category", "VALIDATION_ERROR");
        this.malformedCounter = meterRegistry.counter("tpdlq_dlq_category_total", "category", "MALFORMED_ERROR");
        this.unknownCounter = meterRegistry.counter("tpdlq_dlq_category_total", "category", "UNKNOWN_ERROR");
        meterRegistry.gaugeCollectionSize("tpdlq_dlq_backlog", List.of(), dlqMessages);
    }

    @KafkaListener(topics = "${kafka.topic.dlq}", groupId = "${spring.kafka.consumer.group-id}-dlq")
    public void consumeFromDlq(String message) {
        try {
            // Try to parse the DLQ message to extract reason, original message, and category
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("reason") && jsonNode.has("originalMessage")) {
                String reason = jsonNode.get("reason").asText();
                String originalMessage = jsonNode.get("originalMessage").asText();
                
                // Extract category if present
                ErrorCategory category = ErrorCategory.UNKNOWN_ERROR;
                if (jsonNode.has("category")) {
                    String categoryStr = jsonNode.get("category").asText();
                    try {
                        // Expect enum name (e.g., VALIDATION_ERROR); remain tolerant to formats
                        category = ErrorCategory.valueOf(categoryStr.toUpperCase().replace(" ", "_"));
                    } catch (IllegalArgumentException e) {
                        // If category doesn't match enum, use UNKNOWN_ERROR
                        category = ErrorCategory.UNKNOWN_ERROR;
                    }
                }
                
                logger.error("DLQ Consumer - Category: {} | Reason: {} | Original Message: {}", 
                        category, reason, originalMessage);
                
                // Store message for display
                DlqMessage dlqMessage = new DlqMessage(reason, originalMessage, category);
                dlqMessages.add(dlqMessage);
                countCategory(category);
            } else {
                // Old format or plain message
                logger.error("DLQ Consumer - Received error message: {}", message);
                DlqMessage dlqMessage = new DlqMessage("Unknown", message, ErrorCategory.UNKNOWN_ERROR);
                dlqMessages.add(dlqMessage);
                countCategory(ErrorCategory.UNKNOWN_ERROR);
            }
        } catch (Exception e) {
            // If parsing fails, log as-is
            logger.error("DLQ Consumer - Received error message: {}", message);
            DlqMessage dlqMessage = new DlqMessage("Parse Error", message, ErrorCategory.MALFORMED_ERROR);
            dlqMessages.add(dlqMessage);
            countCategory(ErrorCategory.MALFORMED_ERROR);
        }
        // Monitor and handle error messages from DLQ
        // This could involve alerting, manual review, or custom error handling
    }
    
    // Method to retrieve all DLQ messages for display
    public List<DlqMessage> getDlqMessages() {
        return new ArrayList<>(dlqMessages);
    }

    public Optional<DlqMessage> findById(String id) {
        return dlqMessages.stream().filter(m -> m.getId().equals(id)).findFirst();
    }

    public boolean removeById(String id) {
        return dlqMessages.removeIf(m -> m.getId().equals(id));
    }
    
    // Method to clear DLQ messages (optional)
    public void clearDlqMessages() {
        dlqMessages.clear();
    }

    private void countCategory(ErrorCategory category) {
        dlqCounter.increment();
        switch (category) {
            case VALIDATION_ERROR -> validationCounter.increment();
            case MALFORMED_ERROR -> malformedCounter.increment();
            default -> unknownCounter.increment();
        }
    }
}
