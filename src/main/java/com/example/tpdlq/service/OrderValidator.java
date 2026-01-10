package com.example.tpdlq.service;

import com.example.tpdlq.model.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderValidator {

    /**
     * Validates an order and returns an error message if validation fails.
     * 
     * @param order the order to validate
     * @return error message if validation fails, null if order is valid
     */
    public String validateOrder(Order order) {
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
}
