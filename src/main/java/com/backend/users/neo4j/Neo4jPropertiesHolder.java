package com.backend.users.neo4j;

import java.text.MessageFormat;
import java.util.Optional;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import io.jsonwebtoken.lang.Strings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Neo4jPropertiesHolder {
  private static final String NEO4J_URI_TEMPLATE = "neo4j://{0}:{1}";

  private String driverType;
  private Config config;
  private String uri;
  private String host;
  private String port;
  private String username;
  private String password;
  private String database;

  public Driver toDriver() {
    Config driverConfig = Optional.ofNullable(config).orElseGet(Config::defaultConfig);
    return GraphDatabase.driver(getUri(), AuthTokens.basic(username, password), driverConfig);
  }

  public String getUri() {
    if (Strings.hasText(uri)) {
      return uri;
    }
    return MessageFormat.format(NEO4J_URI_TEMPLATE, host, port);
  }
}
