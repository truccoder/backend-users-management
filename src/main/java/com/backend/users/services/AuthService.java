package com.backend.users.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.users.dtos.ForgotPasswordRequestDto;
import com.backend.users.dtos.LoginRequestDto;
import com.backend.users.dtos.LoginResponseDto;
import com.backend.users.dtos.LogoutRequestDto;
import com.backend.users.dtos.RefreshTokenRequestDto;
import com.backend.users.dtos.RefreshTokenResponseDto;
import com.backend.users.dtos.RegisterRequestDto;
import com.backend.users.entities.UserEntity;
import com.backend.users.repositories.UserRepository;
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

  @Transactional
  public Mono<Void> register(RegisterRequestDto request) {
    return keycloakService
        .createUser(
            request.getEmail(),
            request.getPassword(),
            request.getFullName(),
            request.getProfilePictureUrl())
        .flatMap(
            keycloakUserId -> {
              UserEntity user = new UserEntity();
              user.setId(keycloakUserId);
              user.setEmail(request.getEmail());
              user.setFullName(request.getFullName());
              return userRepository
                  .save(user)
                  .doOnSuccess(u -> mailService.sendWelcomeMail(u.getEmail()).subscribe())
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

  public Mono<RefreshTokenResponseDto> refreshAccessToken(RefreshTokenRequestDto request) {
    return keycloakService
        .refreshToken(request.getRefreshToken())
        .map(token -> new RefreshTokenResponseDto(token.accessToken(), token.refreshToken()));
  }

  public Mono<Void> logout(LogoutRequestDto request) {
    return keycloakService.logout(request.getRefreshToken());
  }
}
