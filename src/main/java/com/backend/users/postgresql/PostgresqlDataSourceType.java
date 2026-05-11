package com.backend.users.postgresql;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class PostgresqlDataSourceType {
  public static final String READER = "readerDataSource";
  public static final String WRITER = "writerDataSource";
  public static final String MIGRATION = "migrationDataSource";
}
