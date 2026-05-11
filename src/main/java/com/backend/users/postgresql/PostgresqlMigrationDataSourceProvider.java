package com.backend.users.postgresql;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import com.backend.users.postgresql.settings.PostgresqlConnectionSettings;
import com.backend.users.postgresql.settings.PostgresqlConnectionSettingsProvider;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostgresqlMigrationDataSourceProvider {
  private final PostgresqlConnectionSettingsProvider connectionProvider;

  public DataSource getMigrationDataSource() {
    PostgresqlConnectionSettings settings = connectionProvider.provide();

    String jdbcUrl =
        String.format(
            "jdbc:postgresql://%s:%s/%s?currentSchema=%s",
            settings.getWriterHost(),
            settings.getWriterPort(),
            settings.getDatabase(),
            settings.getSchema());

    return DataSourceBuilder.create()
        .url(jdbcUrl)
        .username(settings.getMigrationUsername())
        .password(settings.getMigrationPassword())
        .driverClassName("org.postgresql.Driver")
        .build();
  }
}
