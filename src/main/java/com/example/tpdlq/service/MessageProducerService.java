package com.example.tpdlq.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageProducerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerService.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.input}")
    private String inputTopic;

    @Value("${kafka.topic.dlq}")
    private String dlqTopic;

    public void sendToInputTopic(String message) {
        logger.info("Sending message to input topic {}: {}", inputTopic, message);
        kafkaTemplate.send(inputTopic, message);
    }

    public void sendToDlqTopic(String message) {
        logger.warn("Sending message to DLQ topic {}: {}", dlqTopic, message);
        kafkaTemplate.send(dlqTopic, message);
    }
}
