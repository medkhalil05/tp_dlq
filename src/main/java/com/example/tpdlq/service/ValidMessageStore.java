package com.example.tpdlq.service;

import com.example.tpdlq.model.ValidMessage;
import com.example.tpdlq.model.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ValidMessageStore {
    private final List<ValidMessage> messages = new CopyOnWriteArrayList<>();
    private final int maxSize = 200; // keep last 200 messages

    public void add(Order order, String originalMessage) {
        if (order == null) return;
        ValidMessage vm = new ValidMessage(order.getOrderId(), order.getUserId(), order.getAmount(), originalMessage);
        messages.add(vm);
        // Trim if exceeding max size
        if (messages.size() > maxSize) {
            messages.remove(0);
        }
    }

    public List<ValidMessage> getAll() {
        return new ArrayList<>(messages);
    }

    public void clear() {
        messages.clear();
    }
}
