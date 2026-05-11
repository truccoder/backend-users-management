package com.backend.users.cache;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "app.caches")
public class CacheProperties {
  private CacheConfig user;
  private CacheConfig friends;
  private CacheConfig suggestions;

  @Data
  public static class CacheConfig {
    private String keyPrefix;
    private Duration ttl;

    public CacheConfig(String keyPrefix, Duration ttl) {
      this.keyPrefix = keyPrefix;
      this.ttl = ttl;
    }
  }
}
