package com.backend.users.postgresql;

import static com.backend.users.postgresql.PostgresqlDataSourceType.READER;
import static com.backend.users.postgresql.PostgresqlDataSourceType.WRITER;
import static java.lang.String.format;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.backend.core.exceptions.ConfigurationException;
import com.backend.users.postgresql.settings.PostgresqlConnectionSettings;
import com.backend.users.postgresql.settings.PostgresqlConnectionSettingsProvider;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class PostgresqlConnectionFactory {
  private final PostgresqlConnectionSettingsProvider connectionProvider;

  public ConnectionFactory getConnectionFactory(String dataSourceType) {
    PostgresqlConnectionSettings settings = connectionProvider.provide();
    PostgresqlPropertiesHolder propertiesHolder =
        initializeDataSourcePropertiesHolder(settings, dataSourceType);

    if (!isValidPropertiesHolder(propertiesHolder)) {
      throw new ConfigurationException(
          format("Failed to initialize required PostgresqlConnectionFactory [%s]", dataSourceType));
    }

    log.info(
        "PostgresqlConnectionFactory [{}] Host: {}", dataSourceType, propertiesHolder.getHost());
    log.info(
        "PostgresqlConnectionFactory [{}] Database: {}",
        dataSourceType,
        propertiesHolder.getDatabase());
    log.info(
        "PostgresqlConnectionFactory [{}] Username: {}",
        dataSourceType,
        propertiesHolder.getUsername());

    ConnectionFactoryOptions options =
        ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "postgresql")
            .option(ConnectionFactoryOptions.HOST, propertiesHolder.getHost())
            .option(ConnectionFactoryOptions.PORT, Integer.parseInt(propertiesHolder.getPort()))
            .option(ConnectionFactoryOptions.DATABASE, propertiesHolder.getDatabase())
            .option(ConnectionFactoryOptions.USER, propertiesHolder.getUsername())
            .option(ConnectionFactoryOptions.PASSWORD, propertiesHolder.getPassword())
            .option(Option.valueOf("schema"), propertiesHolder.getSchema())
            .build();

    return ConnectionFactories.get(options);
  }

  private PostgresqlPropertiesHolder initializeDataSourcePropertiesHolder(
      PostgresqlConnectionSettings settings, String dataSourceType) {

    PostgresqlPropertiesHolder.PostgresqlPropertiesHolderBuilder builder =
        PostgresqlPropertiesHolder.builder()
            .dataSourceType(dataSourceType)
            .database(settings.getDatabase())
            .schema(settings.getSchema());

    return switch (dataSourceType) {
      case READER -> builder
          .host(settings.getReaderHost())
          .port(settings.getReaderPort())
          .username(settings.getReaderUsername())
          .password(settings.getReaderPassword())
          .build();
      case WRITER -> builder
          .host(settings.getWriterHost())
          .port(settings.getWriterPort())
          .username(settings.getWriterUsername())
          .password(settings.getWriterPassword())
          .build();
      default -> throw new ConfigurationException(
          format("Unsupported ConnectionFactory type supplied [%s]", dataSourceType));
    };
  }

  private boolean isValidPropertiesHolder(PostgresqlPropertiesHolder propertiesHolder) {
    String dataSourceType = propertiesHolder.getDataSourceType();

    if (StringUtils.isBlank(propertiesHolder.getHost())) {
      log.error("PostgresqlConnectionFactory[{}] connection[host] is not defined", dataSourceType);
      return false;
    }
    if (StringUtils.isBlank(propertiesHolder.getPort())) {
      log.error("PostgresqlConnectionFactory[{}] connection[port] is not defined", dataSourceType);
      return false;
    }
    if (StringUtils.isBlank(propertiesHolder.getUsername())) {
      log.error("PostgresqlConnectionFactory[{}] connection[user] is not defined", dataSourceType);
      return false;
    }
    if (StringUtils.isBlank(propertiesHolder.getPassword())) {
      log.error(
          "PostgresqlConnectionFactory[{}] connection[password] is not defined", dataSourceType);
      return false;
    }
    if (StringUtils.isBlank(propertiesHolder.getDatabase())) {
      log.error(
          "PostgresqlConnectionFactory[{}] connection[database] is not defined", dataSourceType);
      return false;
    }
    if (StringUtils.isBlank(propertiesHolder.getSchema())) {
      log.error(
          "PostgresqlConnectionFactory[{}] connection[schema] is not defined", dataSourceType);
      return false;
    }
    return true;
  }
}
