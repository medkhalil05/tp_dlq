package com.example.tpdlq.controller;

import com.example.tpdlq.service.MessageProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageProducerService messageProducerService;

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody String message) {
        messageProducerService.sendToInputTopic(message);
        return ResponseEntity.ok("Message sent to input topic: " + message);
    }

    @PostMapping("/send-valid")
    public ResponseEntity<String> sendValidMessage() {
        String validMessage = "This is a valid message for processing";
        messageProducerService.sendToInputTopic(validMessage);
        return ResponseEntity.ok("Valid message sent: " + validMessage);
    }

    @PostMapping("/send-invalid")
    public ResponseEntity<String> sendInvalidMessage() {
        String invalidMessage = "This is an invalid message";
        messageProducerService.sendToInputTopic(invalidMessage);
        return ResponseEntity.ok("Invalid message sent: " + invalidMessage);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("DLQ Service is running");
    }
}
