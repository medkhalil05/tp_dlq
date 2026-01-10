package com.example.tpdlq.model;

import java.time.LocalDateTime;

public class DlqMessage {
    private String reason;
    private String originalMessage;
    private LocalDateTime timestamp;
    private ErrorCategory category;

    public DlqMessage() {
        this.timestamp = LocalDateTime.now();
        this.category = ErrorCategory.UNKNOWN_ERROR;
    }

    public DlqMessage(String reason, String originalMessage) {
        this.reason = reason;
        this.originalMessage = originalMessage;
        this.timestamp = LocalDateTime.now();
        this.category = ErrorCategory.UNKNOWN_ERROR;
    }

    public DlqMessage(String reason, String originalMessage, ErrorCategory category) {
        this.reason = reason;
        this.originalMessage = originalMessage;
        this.timestamp = LocalDateTime.now();
        this.category = category;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    public void setOriginalMessage(String originalMessage) {
        this.originalMessage = originalMessage;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public void setCategory(ErrorCategory category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "DlqMessage{" +
                "reason='" + reason + '\'' +
                ", originalMessage='" + originalMessage + '\'' +
                ", timestamp=" + timestamp +
                ", category=" + category +
                '}';
    }
}
