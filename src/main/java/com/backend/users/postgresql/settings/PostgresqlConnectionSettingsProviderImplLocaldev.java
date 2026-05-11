package com.backend.users.postgresql.settings;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@Profile({"localdev", "test"})
@ConditionalOnBean(LocaldevPostgresqlSettings.class)
@RequiredArgsConstructor
public class PostgresqlConnectionSettingsProviderImplLocaldev
    implements PostgresqlConnectionSettingsProvider {
  private final LocaldevPostgresqlSettings localdevSettings;

  @Override
  public PostgresqlConnectionSettings provide() {
    PostgresqlConnectionSettings settings = new PostgresqlConnectionSettings();

    settings.setWriterHost(localdevSettings.getAuroraHost());
    settings.setWriterPort(localdevSettings.getAuroraPort());
    settings.setWriterUsername(localdevSettings.getAuroraUsername());
    settings.setWriterPassword(localdevSettings.getAuroraPassword());

    settings.setReaderHost(localdevSettings.getAuroraHost());
    settings.setReaderPort(localdevSettings.getAuroraPort());
    settings.setReaderUsername(localdevSettings.getAuroraUsername());
    settings.setReaderPassword(localdevSettings.getAuroraPassword());

    settings.setMigrationUsername(localdevSettings.getAuroraUsername());
    settings.setMigrationPassword(localdevSettings.getAuroraPassword());

    settings.setDatabase(localdevSettings.getAuroraDatabase());
    settings.setSchema(localdevSettings.getAuroraSchema());

    return settings;
  }
}
