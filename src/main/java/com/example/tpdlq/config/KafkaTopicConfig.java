package com.example.tpdlq.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topic.input}")
    private String inputTopic;

    @Value("${kafka.topic.dlq}")
    private String dlqTopic;

    @Bean
    public NewTopic inputTopic() {
        return TopicBuilder.name(inputTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(dlqTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
