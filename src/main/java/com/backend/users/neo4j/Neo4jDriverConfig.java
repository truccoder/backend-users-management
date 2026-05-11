package com.backend.users.neo4j;

import java.util.concurrent.TimeUnit;

import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnClass({Driver.class})
@EnableReactiveNeo4jRepositories(
    basePackages = "com.backend.users.repositories",
    transactionManagerRef = "neo4jTransactionManager")
@EnableTransactionManagement
public class Neo4jDriverConfig {
  @Bean
  protected Config neo4jCommonConfig() {
    return Config.builder()
        .withMaxConnectionPoolSize(50)
        // If pool is exhausted, request waits up to 60 seconds
        .withConnectionAcquisitionTimeout(60, TimeUnit.SECONDS)
        // How long to wait when opening a NEW connection to Neo4j
        .withConnectionTimeout(30, TimeUnit.SECONDS)
        // Maximum lifetime of a connection before it's CLOSED and recreated
        .withMaxConnectionLifetime(1, TimeUnit.HOURS)
        // Timeout for checking if an IDLE connection is still alive before using it
        .withConnectionLivenessCheckTimeout(5, TimeUnit.SECONDS)
        .build();
  }

  @Bean
  @Primary
  protected Driver neo4jDriver(Neo4jDriverFactory driverFactory) {
    return driverFactory.getDriver(neo4jCommonConfig());
  }

  @Bean("neo4jTransactionManager")
  public ReactiveTransactionManager neo4jTransactionManager(
      Driver routingNeo4jDriver, ReactiveDatabaseSelectionProvider databaseSelectionProvider) {
    return new ReactiveNeo4jTransactionManager(routingNeo4jDriver, databaseSelectionProvider);
  }
}
