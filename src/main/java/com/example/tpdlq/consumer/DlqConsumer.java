package com.example.tpdlq.consumer;

import com.example.tpdlq.model.DlqMessage;
import com.example.tpdlq.model.ErrorCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DlqConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DlqConsumer.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Thread-safe list to store DLQ messages for display
    private final List<DlqMessage> dlqMessages = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "${kafka.topic.dlq}", groupId = "${spring.kafka.consumer.group-id}-dlq")
    public void consumeFromDlq(String message) {
        try {
            // Try to parse the DLQ message to extract reason, original message, and category
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("reason") && jsonNode.has("originalMessage")) {
                String reason = jsonNode.get("reason").asText();
                String originalMessage = jsonNode.get("originalMessage").toString();
                
                // Extract category if present
                ErrorCategory category = ErrorCategory.UNKNOWN_ERROR;
                if (jsonNode.has("category")) {
                    String categoryStr = jsonNode.get("category").asText();
                    try {
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
            } else {
                // Old format or plain message
                logger.error("DLQ Consumer - Received error message: {}", message);
                DlqMessage dlqMessage = new DlqMessage("Unknown", message, ErrorCategory.UNKNOWN_ERROR);
                dlqMessages.add(dlqMessage);
            }
        } catch (Exception e) {
            // If parsing fails, log as-is
            logger.error("DLQ Consumer - Received error message: {}", message);
            DlqMessage dlqMessage = new DlqMessage("Parse Error", message, ErrorCategory.MALFORMED_ERROR);
            dlqMessages.add(dlqMessage);
        }
        // Monitor and handle error messages from DLQ
        // This could involve alerting, manual review, or custom error handling
    }
    
    // Method to retrieve all DLQ messages for display
    public List<DlqMessage> getDlqMessages() {
        return new ArrayList<>(dlqMessages);
    }
    
    // Method to clear DLQ messages (optional)
    public void clearDlqMessages() {
        dlqMessages.clear();
    }
}
