package com.github.birulazena.PaymentService.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class KafkaConfig {

    @Value("${app.kafka.topics.payment-events.name}")
    private String topicName;

    @Value("${app.kafka.topics.payment-events.partitions}")
    private int partitions;

    @Value("${app.kafka.topics.payment-events.replicas}")
    private int replicas;

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

}
