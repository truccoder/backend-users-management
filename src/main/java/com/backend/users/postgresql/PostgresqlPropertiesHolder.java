package com.backend.users.postgresql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostgresqlPropertiesHolder {
  private static final String R2DBC_URL_TEMPLATE = "r2dbc:postgresql://{0}:{1}/{2}?schema={3}";

  private String dataSourceType;
  private String database;
  private String schema;
  private String host;
  private String port;
  private String username;
  private String password;
}
