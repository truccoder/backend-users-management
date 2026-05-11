package com.backend.users.neo4j;

import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.springframework.stereotype.Component;

import com.backend.users.neo4j.settings.Neo4jConnectionSettings;
import com.backend.users.neo4j.settings.Neo4jConnectionSettingsProvider;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@AllArgsConstructor
public class Neo4jDriverFactory {
  private final Neo4jConnectionSettingsProvider connectionProvider;

  public Driver getDriver(Config commonConfig) {
    Neo4jConnectionSettings settings = connectionProvider.provide();

    Neo4jPropertiesHolder properties =
        Neo4jPropertiesHolder.builder()
            .config(commonConfig)
            .uri(settings.getUri())
            .host(settings.getHost())
            .port(settings.getPort())
            .username(settings.getUsername())
            .password(settings.getPassword())
            .database(settings.getDatabase())
            .build();

    log.info("Neo4j URI: {}", properties.getUri());
    log.info("Neo4j Username: {}", properties.getUsername());
    log.info("Neo4j Database: {}", properties.getDatabase());

    return properties.toDriver();
  }
}
