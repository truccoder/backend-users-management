package com.backend.users.postgresql.settings;

import lombok.Data;

@Data
public class PostgresqlConnectionSettings {
  private String writerHost;
  private String writerPort;
  private String writerUsername;
  private String writerPassword;

  private String readerHost;
  private String readerPort;
  private String readerUsername;
  private String readerPassword;

  private String migrationUsername;
  private String migrationPassword;

  private String database;
  private String schema;
}
