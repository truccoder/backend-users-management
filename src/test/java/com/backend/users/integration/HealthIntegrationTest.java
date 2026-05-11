package com.backend.users.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

public class HealthIntegrationTest extends BaseTest {
  @Autowired private WebTestClient webTestClient;

  @Test
  void healthAll() {
    webTestClient
        .get()
        .uri("/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("UP")
        .jsonPath("$.components.kafka.status")
        .isEqualTo("UP")
        .jsonPath("$.components.neo4j.status")
        .isEqualTo("UP")
        .jsonPath("$.components.db.status")
        .isEqualTo("UP");
  }
}
