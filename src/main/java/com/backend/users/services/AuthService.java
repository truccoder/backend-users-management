package com.backend.users.services;

import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.core.cache.ReactiveCacheTemplate;
import com.backend.core.dtos.UserDto;
import com.backend.core.dtos.ValidateTokenRequestDto;
import com.backend.core.dtos.ValidateTokenResponseDto;
import com.backend.users.dtos.ForgotPasswordRequestDto;
import com.backend.users.dtos.LoginRequestDto;
import com.backend.users.dtos.LoginResponseDto;
import com.backend.users.dtos.LogoutRequestDto;
import com.backend.users.dtos.ProfileResponseDto;
import com.backend.users.dtos.RefreshTokenRequestDto;
import com.backend.users.dtos.RefreshTokenResponseDto;
import com.backend.users.dtos.RegisterRequestDto;
import com.backend.users.entities.UserEntity;
import com.backend.users.mappers.UserMapper;
import com.backend.users.repositories.UserRepository;
import com.backend.users.services.KeycloakService.KeycloakTokenResponse;
import com.backend.users.ses.MailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
  private final KeycloakService keycloakService;
  private final MailService mailService;
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final ReactiveCacheTemplate<ProfileResponseDto> userProfileCache;
  private final ReactiveJwtDecoder jwtDecoder;

  @Transactional
  public Mono<Void> register(RegisterRequestDto request) {
    return keycloakService
        .createUser(request.getEmail(), request.getPassword(), request.getFullName())
        .flatMap(
            keycloakUserId -> {
              UserEntity user = new UserEntity();
              user.setId(keycloakUserId);
              user.setEmail(request.getEmail());
              user.setFullName(request.getFullName());
              user.setProfilePictureUrl(request.getProfilePictureUrl());
              return userRepository
                  .save(user)
                  .doOnSuccess(
                      u -> {
                        cacheUserProfile(u);
                        mailService.sendWelcomeMail(u.getEmail()).subscribe();
                      })
                  .then();
            });
  }

  public Mono<LoginResponseDto> login(LoginRequestDto request) {
    return keycloakService
        .login(request.getEmail(), request.getPassword())
        .map(token -> new LoginResponseDto(token.accessToken(), token.refreshToken()));
  }

  public Mono<Void> forgotPassword(ForgotPasswordRequestDto request) {
    return keycloakService.sendPasswordResetEmail(request.getEmail());
  }

  public Mono<ValidateTokenResponseDto> validateToken(ValidateTokenRequestDto request) {
    return jwtDecoder
        .decode(request.getToken())
        .map(
            jwt -> {
              String id = jwt.getSubject();
              String email = jwt.getClaimAsString("email");
              UserDto user = new UserDto(id, email);
              return ValidateTokenResponseDto.builder()
                  .valid(true)
                  .expiresAt(java.util.Date.from(jwt.getExpiresAt()))
                  .user(user)
                  .build();
            })
        .onErrorResume(e -> Mono.just(ValidateTokenResponseDto.builder().valid(false).build()));
  }

  public Mono<RefreshTokenResponseDto> refreshAccessToken(RefreshTokenRequestDto request) {
    return keycloakService
        .refreshToken(request.getRefreshToken())
        .map(KeycloakTokenResponse::accessToken)
        .map(RefreshTokenResponseDto::new);
  }

  public Mono<Void> logout(LogoutRequestDto request) {
    return keycloakService.logout(request.getRefreshToken());
  }

  private void cacheUserProfile(UserEntity user) {
    userProfileCache.put(user.getId(), userMapper.toProfileResponseDto(user)).subscribe();
  }
}
