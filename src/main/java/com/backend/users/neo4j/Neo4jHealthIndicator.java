package com.backend.users.neo4j;

import org.neo4j.driver.Driver;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component("neo4jHealthIndicator")
@RequiredArgsConstructor
public class Neo4jHealthIndicator implements ReactiveHealthIndicator {
  private final Driver neo4jDriver;

  @Override
  public Mono<Health> health() {
    return Mono.fromCallable(
            () -> {
              neo4jDriver.verifyConnectivity();
              return true;
            })
        .map(connected -> Health.up().build())
        .onErrorResume(ex -> Mono.just(Health.down().withException(ex).build()));
  }
}
