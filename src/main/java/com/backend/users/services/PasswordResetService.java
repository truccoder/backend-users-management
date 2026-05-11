package com.backend.users.services;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.backend.core.tsid.TsidGenerator;
import com.backend.users.entities.PasswordResetTokenEntity;
import com.backend.users.entities.UserEntity;
import com.backend.users.repositories.PasswordResetTokenRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final TsidGenerator tsidGenerator;

  @Value("${jwt.reset-token-expiration}")
  private Long resetTokenExpiration;

  public Mono<PasswordResetTokenEntity> createPasswordResetToken(UserEntity user) {
    PasswordResetTokenEntity token = new PasswordResetTokenEntity();
    token.setToken(tsidGenerator.generate());
    token.setUserId(user.getId());
    token.setExpiresAt(OffsetDateTime.now().plusSeconds(resetTokenExpiration / 1000));

    return passwordResetTokenRepository.save(token);
  }

  public Mono<PasswordResetTokenEntity> validatePasswordResetToken(String token) {
    return passwordResetTokenRepository
        .findByToken(token)
        .filter(rt -> !rt.getExpiresAt().isBefore(OffsetDateTime.now()));
  }

  public Mono<Void> deletePasswordResetToken(String token) {
    return passwordResetTokenRepository.deleteByToken(token);
  }
}
