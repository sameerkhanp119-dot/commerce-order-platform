package com.skp.commerce.order.config;

import com.skp.commerce.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(Topics.ORDER_EVENTS).partitions(6).replicas(1).build();
    }

    @Bean
    NewTopic paymentEventsTopic() {
        return TopicBuilder.name(Topics.PAYMENT_EVENTS).partitions(6).replicas(1).build();
    }

    /** Retry with exponential backoff, then park the record on <topic>.DLT instead of blocking the partition. */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        ExponentialBackOff backOff = new ExponentialBackOff(500L, 2.0);
        backOff.setMaxElapsedTime(10_000L);
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), backOff);
    }
}
