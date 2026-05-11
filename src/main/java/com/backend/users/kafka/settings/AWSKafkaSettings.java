package com.backend.users.kafka.settings;

import org.neo4j.cypherdsl.core.utils.Strings;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AWSKafkaSettings {
  @JsonProperty("bootstrap_brokers_tls")
  private String bootstrapBrokersTls;

  @JsonProperty("bootstrap_brokers")
  private String bootstrapBrokers;

  public String getFirstTlsBroker() {
    if (Strings.hasText(bootstrapBrokersTls)) {
      return bootstrapBrokersTls.split(",")[0];
    }
    return null;
  }
}
