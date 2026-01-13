package com.example.tpdlq.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class Order {
    private String orderId;
    private String userId;
    private Double amount;
    
    // Store any additional fields that aren't explicitly defined
    private Map<String, Object> additionalProperties = new HashMap<>();

    public Order() {
    }

    public Order(String orderId, String userId, Double amount) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String key, Object value) {
        additionalProperties.put(key, value);
    }

    public boolean hasAdditionalFields() {
        return !additionalProperties.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Order{");
        sb.append("orderId='").append(orderId).append('\'')
          .append(", userId='").append(userId).append('\'')
          .append(", amount=").append(amount);
        if (!additionalProperties.isEmpty()) {
            sb.append(", additionalFields=").append(additionalProperties);
        }
        sb.append('}');
        return sb.toString();
    }
}
