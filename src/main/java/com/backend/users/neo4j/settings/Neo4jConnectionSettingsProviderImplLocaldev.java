package com.backend.users.neo4j.settings;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@Profile({"localdev", "test"})
@ConditionalOnBean(LocaldevNeo4jSettings.class)
@RequiredArgsConstructor
public class Neo4jConnectionSettingsProviderImplLocaldev
    implements Neo4jConnectionSettingsProvider {
  private final LocaldevNeo4jSettings localdevSettings;

  @Override
  public Neo4jConnectionSettings provide() {
    Neo4jConnectionSettings settings = new Neo4jConnectionSettings();

    settings.setHost(localdevSettings.getNeo4jHost());
    settings.setPort(localdevSettings.getNeo4jPort());
    settings.setUsername(localdevSettings.getNeo4jUsername());
    settings.setPassword(localdevSettings.getNeo4jPassword());

    settings.setDatabase(localdevSettings.getNeo4jDatabase());

    return settings;
  }
}
