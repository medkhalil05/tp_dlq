package com.example.tpdlq.model;

import java.time.LocalDateTime;

public class DlqMessage {
    private String reason;
    private String originalMessage;
    private LocalDateTime timestamp;

    public DlqMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public DlqMessage(String reason, String originalMessage) {
        this.reason = reason;
        this.originalMessage = originalMessage;
        this.timestamp = LocalDateTime.now();
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

    @Override
    public String toString() {
        return "DlqMessage{" +
                "reason='" + reason + '\'' +
                ", originalMessage='" + originalMessage + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
