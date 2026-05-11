package com.backend.users.repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;

import com.backend.users.entities.RefreshTokenEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepository extends R2dbcRepository<RefreshTokenEntity, String> {
  Mono<RefreshTokenEntity> findByToken(String token);

  Mono<Void> deleteByToken(String token);

  @Query("DELETE FROM t_refresh_tokens WHERE user_id = :userId AND token != :excludeToken")
  Mono<Void> deleteAllByUserIdExcludingToken(
      @Param("userId") String userId, @Param("excludeToken") String excludeToken);

  Flux<RefreshTokenEntity> findAllByUserId(String userId);
}
