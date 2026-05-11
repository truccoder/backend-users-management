package com.backend.users.integration;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SES;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import com.backend.core.cache.settings.LocaldevCacheSettings;
import com.backend.users.neo4j.settings.LocaldevNeo4jSettings;
import com.backend.users.postgresql.settings.LocaldevPostgresqlSettings;
import com.redis.testcontainers.RedisContainer;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {
  private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);
  private static final int STARTUP_ATTEMPTS = 2;

  // Postgresql ----------------------------------------------------
  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
        .withDatabaseName("test")
        .withUsername("admin")
        .withPassword("veryStrongPassword")
        .withStartupTimeout(STARTUP_TIMEOUT)
        .withStartupAttempts(STARTUP_ATTEMPTS);
  }

  @Bean
  @Primary
  public LocaldevPostgresqlSettings localdevPostgresqlSettings(
      PostgreSQLContainer<?> postgresContainer) {
    LocaldevPostgresqlSettings settings = new LocaldevPostgresqlSettings();
    settings.setAuroraHost(postgresContainer.getHost());
    settings.setAuroraPort(String.valueOf(postgresContainer.getFirstMappedPort()));
    settings.setAuroraUsername(postgresContainer.getUsername());
    settings.setAuroraPassword(postgresContainer.getPassword());
    settings.setAuroraDatabase(postgresContainer.getDatabaseName());
    settings.setAuroraSchema("users");
    return settings;
  }

  // Neo4j ----------------------------------------------------
  @Bean
  @ServiceConnection
  public Neo4jContainer<?> neo4jContainer() {
    return new Neo4jContainer<>("neo4j:5")
        .withAdminPassword("veryStrongPassword")
        .withStartupTimeout(STARTUP_TIMEOUT)
        .withStartupAttempts(STARTUP_ATTEMPTS);
  }

  @Bean
  @Primary
  public LocaldevNeo4jSettings localdevNeo4jSettings(Neo4jContainer<?> neo4jContainer) {
    LocaldevNeo4jSettings settings = new LocaldevNeo4jSettings();
    settings.setNeo4jHost(neo4jContainer.getHost());
    settings.setNeo4jPort(String.valueOf(neo4jContainer.getMappedPort(7687)));
    settings.setNeo4jUsername("neo4j");
    settings.setNeo4jPassword(neo4jContainer.getAdminPassword());
    settings.setNeo4jDatabase("neo4j");
    return settings;
  }

  // Redis ----------------------------------------------------
  @Bean
  @ServiceConnection
  public RedisContainer redisContainer() {
    return new RedisContainer(DockerImageName.parse("redis:7-alpine"))
        .withStartupTimeout(STARTUP_TIMEOUT)
        .withStartupAttempts(STARTUP_ATTEMPTS);
  }

  @Bean
  @Primary
  public LocaldevCacheSettings localdevCacheSettings(RedisContainer redisContainer) {
    LocaldevCacheSettings settings = new LocaldevCacheSettings();
    settings.setHost(redisContainer.getHost());
    settings.setPort(String.valueOf(redisContainer.getFirstMappedPort()));
    settings.setUsername("");
    settings.setPassword("");
    settings.setClusterMode(false);
    return settings;
  }

  // AWS ----------------------------------------------------
  @Bean
  public LocalStackContainer localStackContainer() {
    return new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.4"))
        .withServices(SES)
        .withStartupTimeout(STARTUP_TIMEOUT)
        .withStartupAttempts(STARTUP_ATTEMPTS);
  }

  @Bean
  @Primary
  public SesClient sesClient(LocalStackContainer localStackContainer) {
    URI endpoint = localStackContainer.getEndpointOverride(LocalStackContainer.Service.SES);
    return SesClient.builder()
        .endpointOverride(endpoint)
        .region(Region.of(localStackContainer.getRegion()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    localStackContainer.getAccessKey(), localStackContainer.getSecretKey())))
        .build();
  }
}
