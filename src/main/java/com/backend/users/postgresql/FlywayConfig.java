package com.backend.users.postgresql;

import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class FlywayConfig {
  private final PostgresqlMigrationDataSourceProvider migrationProvider;

  @Bean
  @FlywayDataSource
  public DataSource flywayDataSource() {
    return migrationProvider.getMigrationDataSource();
  }
}
