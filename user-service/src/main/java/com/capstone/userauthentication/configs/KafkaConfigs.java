package com.capstone.userauthentication.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class KafkaConfigs {
    public static final String USER_REGISTERED_TOPIC = "user_registered";
    public static final String USER_LOGGED_TOPIC = "user_logged";
    public static final String PASSWORD_RESET_TOPIC = "password_reset";


    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(USER_REGISTERED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userLoggedTopic() {
        return TopicBuilder.name(USER_LOGGED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic passwordResetTopic() {
        return TopicBuilder.name(PASSWORD_RESET_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
