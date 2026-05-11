package com.backend.users.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.backend.users.dtos.BaseEvent;
import com.backend.users.kafka.settings.KafkaBootstrapServersProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {
  private final KafkaProperties kafkaProperties;
  private final KafkaBootstrapServersProvider bootstrapServersProvider;

  @Bean
  public KafkaSender<String, BaseEvent> kafkaSender() {
    Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServersProvider.provide());

    SenderOptions<String, BaseEvent> senderOptions =
        SenderOptions.<String, BaseEvent>create(props).maxInFlight(1024);
    return KafkaSender.create(senderOptions);
  }
}
