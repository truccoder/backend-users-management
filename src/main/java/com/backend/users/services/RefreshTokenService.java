package com.backend.users.services;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.backend.core.exceptions.ValidationException;
import com.backend.core.tsid.TsidGenerator;
import com.backend.users.dtos.ListSessionsRequestDto;
import com.backend.users.dtos.SessionResponseDto;
import com.backend.users.entities.RefreshTokenEntity;
import com.backend.users.entities.UserEntity;
import com.backend.users.repositories.RefreshTokenRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RefreshTokenService {
  private final RefreshTokenRepository refreshTokenRepository;
  private final Long refreshTokenExpiration;
  private final TsidGenerator tsidGenerator;

  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository,
      @Value("${jwt.refresh-token-expiration}") Long refreshTokenExpiration,
      TsidGenerator tsidGenerator) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.refreshTokenExpiration = refreshTokenExpiration;
    this.tsidGenerator = tsidGenerator;
  }

  public Mono<RefreshTokenEntity> createRefreshToken(UserEntity user) {
    RefreshTokenEntity refreshToken = new RefreshTokenEntity();
    refreshToken.setToken(tsidGenerator.generate());
    refreshToken.setUserId(user.getId());
    refreshToken.setExpiresAt(OffsetDateTime.now().plusSeconds(refreshTokenExpiration / 1000));

    return refreshTokenRepository.save(refreshToken);
  }

  public Mono<RefreshTokenEntity> validateRefreshToken(String token) {
    return refreshTokenRepository
        .findByToken(token)
        .flatMap(
            rt -> {
              if (rt.getExpiresAt().isBefore(OffsetDateTime.now())) {
                return refreshTokenRepository.delete(rt).then(Mono.empty());
              }
              return Mono.just(rt);
            });
  }

  public Mono<Void> deleteRefreshToken(String token) {
    return refreshTokenRepository.deleteByToken(token);
  }

  public Mono<Void> revokeOtherSessions(String userId, String currentRefreshToken) {
    return validateOwnership(userId, currentRefreshToken)
        .then(refreshTokenRepository.deleteAllByUserIdExcludingToken(userId, currentRefreshToken));
  }

  public Mono<Void> revokeSession(String userId, String currentRefreshToken, String targetToken) {
    if (currentRefreshToken.equals(targetToken)) {
      return Mono.error(
          new ValidationException("Cannot revoke current session, use logout instead"));
    }
    return validateOwnership(userId, targetToken)
        .then(refreshTokenRepository.deleteByToken(targetToken));
  }

  public Flux<SessionResponseDto> listSessions(String userId, ListSessionsRequestDto request) {
    String currentRefreshToken = request.getCurrentRefreshToken();
    return validateOwnership(userId, currentRefreshToken)
        .thenMany(
            refreshTokenRepository
                .findAllByUserId(userId)
                .filter(rt -> rt.getExpiresAt().isAfter(OffsetDateTime.now()))
                .map(
                    rt ->
                        new SessionResponseDto(
                            rt.getToken(),
                            rt.getExpiresAt(),
                            tsidGenerator.extractCreatedAt(rt.getToken()),
                            rt.getToken().equals(currentRefreshToken))));
  }

  private Mono<Void> validateOwnership(String userId, String refreshToken) {
    return refreshTokenRepository
        .findByToken(refreshToken)
        .switchIfEmpty(Mono.error(new ValidationException("Invalid refresh token")))
        .flatMap(
            rt -> {
              if (!rt.getUserId().equals(userId)) {
                return Mono.error(new ValidationException("Refresh token does not belong to user"));
              }
              if (rt.getExpiresAt().isBefore(OffsetDateTime.now())) {
                return Mono.error(new ValidationException("Refresh token has expired"));
              }
              return Mono.empty();
            });
  }
}
