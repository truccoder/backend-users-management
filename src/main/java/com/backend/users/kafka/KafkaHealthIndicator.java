package com.backend.users.kafka;

import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component("kafkaHealthIndicator")
public class KafkaHealthIndicator implements ReactiveHealthIndicator {
  private final String bootstrapServers;

  public KafkaHealthIndicator(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
  }

  @Override
  public Mono<Health> health() {
    return Mono.fromCallable(
        () -> {
          try (AdminClient admin =
              AdminClient.create(
                  Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            admin.listTopics().names().get();
            return Health.up().build();
          } catch (Exception ex) {
            return Health.down().withException(ex).build();
          }
        });
  }
}
