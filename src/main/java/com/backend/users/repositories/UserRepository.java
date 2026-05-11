package com.backend.users.repositories;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.backend.users.entities.UserEntity;

import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<UserEntity, String> {
  Mono<UserEntity> findByEmail(String email);

  Mono<Boolean> existsByEmail(String email);
}
