package com.backend.users.postgresql;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component("dbHealthIndicator")
@RequiredArgsConstructor
@Slf4j
public class PostgresHealthIndicator implements ReactiveHealthIndicator {
  private final DatabaseClient databaseClient;

  @Override
  public Mono<Health> health() {
    return databaseClient
        .sql("SELECT 1")
        .fetch()
        .first()
        .map(result -> Health.up().build())
        .onErrorResume(ex -> Mono.just(Health.down().withException(ex).build()));
  }
}
