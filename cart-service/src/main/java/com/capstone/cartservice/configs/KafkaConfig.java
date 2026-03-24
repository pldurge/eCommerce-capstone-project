package com.capstone.cartservice.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic cartUpdatedTopic() {
        return TopicBuilder.name("cart-updated").partitions(3).replicas(1).build();
    }
}
