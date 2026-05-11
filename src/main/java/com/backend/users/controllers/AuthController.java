package com.backend.users.controllers;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.backend.core.annotations.Anonymous;
import com.backend.core.dtos.ValidateTokenRequestDto;
import com.backend.core.dtos.ValidateTokenResponseDto;
import com.backend.users.dtos.ForgotPasswordRequestDto;
import com.backend.users.dtos.LoginRequestDto;
import com.backend.users.dtos.LoginResponseDto;
import com.backend.users.dtos.LogoutRequestDto;
import com.backend.users.dtos.RefreshTokenRequestDto;
import com.backend.users.dtos.RefreshTokenResponseDto;
import com.backend.users.dtos.RegisterRequestDto;
import com.backend.users.dtos.ResetPasswordRequestDto;
import com.backend.users.services.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Anonymous
@RestController
@RequestMapping("/v1/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  public Mono<Void> register(@Valid @RequestBody RegisterRequestDto request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public Mono<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
    return authService.login(request);
  }

  @PostMapping("/forgot-password")
  public Mono<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
    return authService.forgotPassword(request);
  }

  @PostMapping("/reset-password")
  public Mono<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
    return authService.resetPassword(request);
  }

  @PostMapping("/validate-token")
  public Mono<ValidateTokenResponseDto> validateToken(
      @Valid @RequestBody ValidateTokenRequestDto request) {
    return authService.validateToken(request);
  }

  @PostMapping("/refresh")
  public Mono<RefreshTokenResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
    return authService.refreshAccessToken(request);
  }

  @PostMapping("/logout")
  public Mono<Void> logout(@Valid @RequestBody LogoutRequestDto request) {
    return authService.logout(request);
  }
}
