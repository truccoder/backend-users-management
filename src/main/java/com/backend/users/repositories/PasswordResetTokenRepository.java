package com.backend.users.repositories;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import com.backend.users.entities.PasswordResetTokenEntity;

import reactor.core.publisher.Mono;

@Repository
public interface PasswordResetTokenRepository
    extends R2dbcRepository<PasswordResetTokenEntity, String> {
  Mono<PasswordResetTokenEntity> findByToken(String token);

  Mono<Void> deleteByToken(String token);
}
