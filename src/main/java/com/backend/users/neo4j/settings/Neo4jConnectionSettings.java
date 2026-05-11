package com.backend.users.neo4j.settings;

import lombok.Data;

@Data
public class Neo4jConnectionSettings {
  private String uri;
  private String host;
  private String port;
  private String username;
  private String password;

  private String database;
}
