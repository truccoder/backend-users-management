package com.backend.users.postgresql.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties
public class LocaldevPostgresqlSettings {
  private String auroraHost;
  private String auroraPort;
  private String auroraUsername;
  private String auroraPassword;

  private String auroraDatabase;
  private String auroraSchema;
}
