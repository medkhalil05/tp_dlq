package com.example.tpdlq.model;

import java.time.LocalDateTime;

public class ValidMessage {
    private final String orderId;
    private final String userId;
    private final double amount;
    private final String originalMessage;
    private final LocalDateTime timestamp;

    public ValidMessage(String orderId, String userId, double amount, String originalMessage) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.originalMessage = originalMessage;
        this.timestamp = LocalDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
