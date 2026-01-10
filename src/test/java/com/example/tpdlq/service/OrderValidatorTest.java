package com.example.tpdlq.service;

import com.example.tpdlq.model.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderValidatorTest {

    private final OrderValidator validator = new OrderValidator();

    @Test
    void testValidOrder() {
        Order order = new Order("ord-123", "user-456", 100.0);
        String error = validator.validateOrder(order);
        assertNull(error, "Valid order should not return error");
    }

    @Test
    void testMissingOrderId() {
        Order order = new Order(null, "user-456", 100.0);
        String error = validator.validateOrder(order);
        assertNotNull(error, "Missing orderId should return error");
        assertTrue(error.contains("orderId"), "Error message should mention orderId");
    }

    @Test
    void testEmptyOrderId() {
        Order order = new Order("  ", "user-456", 100.0);
        String error = validator.validateOrder(order);
        assertNotNull(error, "Empty orderId should return error");
        assertTrue(error.contains("orderId"), "Error message should mention orderId");
    }

    @Test
    void testMissingUserId() {
        Order order = new Order("ord-123", null, 100.0);
        String error = validator.validateOrder(order);
        assertNotNull(error, "Missing userId should return error");
        assertTrue(error.contains("userId"), "Error message should mention userId");
    }

    @Test
    void testEmptyUserId() {
        Order order = new Order("ord-123", "  ", 100.0);
        String error = validator.validateOrder(order);
        assertNotNull(error, "Empty userId should return error");
        assertTrue(error.contains("userId"), "Error message should mention userId");
    }

    @Test
    void testMissingAmount() {
        Order order = new Order("ord-123", "user-456", null);
        String error = validator.validateOrder(order);
        assertNotNull(error, "Missing amount should return error");
        assertTrue(error.contains("amount"), "Error message should mention amount");
    }

    @Test
    void testZeroAmount() {
        Order order = new Order("ord-123", "user-456", 0.0);
        String error = validator.validateOrder(order);
        assertNotNull(error, "Zero amount should return error");
        assertTrue(error.contains("greater than 0"), "Error message should mention amount must be greater than 0");
    }

    @Test
    void testNegativeAmount() {
        Order order = new Order("ord-123", "user-456", -50.0);
        String error = validator.validateOrder(order);
        assertNotNull(error, "Negative amount should return error");
        assertTrue(error.contains("greater than 0"), "Error message should mention amount must be greater than 0");
    }
}
