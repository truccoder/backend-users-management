package com.backend.users.neo4j.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties
public class LocaldevNeo4jSettings {
  private String neo4jHost;
  private String neo4jPort;
  private String neo4jUsername;
  private String neo4jPassword;
  private String neo4jDatabase;
}
