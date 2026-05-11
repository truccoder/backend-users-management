package com.backend.users.cache;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import com.backend.core.cache.ReactiveCacheTemplate;
import com.backend.users.dtos.ProfileResponseDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {
  @Bean
  public ReactiveCacheTemplate<ProfileResponseDto> userProfileCache(
      @Qualifier("reactiveStringRedisTemplate") ReactiveRedisTemplate<String, String> redis,
      ObjectMapper mapper,
      CacheProperties props) {
    CacheProperties.CacheConfig config = props.getUser();
    return new ReactiveCacheTemplate<>(
        redis, mapper, config.getKeyPrefix(), config.getTtl(), new TypeReference<>() {});
  }

  @Bean
  public ReactiveCacheTemplate<List<ProfileResponseDto>> friendsCache(
      @Qualifier("reactiveStringRedisTemplate") ReactiveRedisTemplate<String, String> redis,
      ObjectMapper mapper,
      CacheProperties props) {
    CacheProperties.CacheConfig config = props.getFriends();
    return new ReactiveCacheTemplate<>(
        redis, mapper, config.getKeyPrefix(), config.getTtl(), new TypeReference<>() {});
  }

  @Bean
  public ReactiveCacheTemplate<List<ProfileResponseDto>> suggestionsCache(
      @Qualifier("reactiveStringRedisTemplate") ReactiveRedisTemplate<String, String> redis,
      ObjectMapper mapper,
      CacheProperties props) {
    CacheProperties.CacheConfig config = props.getSuggestions();
    return new ReactiveCacheTemplate<>(
        redis, mapper, config.getKeyPrefix(), config.getTtl(), new TypeReference<>() {});
  }
}
