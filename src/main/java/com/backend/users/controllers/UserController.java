package com.backend.users.controllers;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.core.dtos.UserDto;
import com.backend.core.security.KeycloakAuthenticationToken;
import com.backend.users.dtos.ChangePasswordRequestDto;
import com.backend.users.dtos.ProfileResponseDto;
import com.backend.users.dtos.SessionResponseDto;
import com.backend.users.dtos.UpdateProfileRequestDto;
import com.backend.users.services.KeycloakService;
import com.backend.users.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/api/me")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final KeycloakService keycloakService;

  @GetMapping("/profile")
  public Mono<ProfileResponseDto> getProfile(@AuthenticationPrincipal UserDto currentUser) {
    return userService.getProfile(currentUser);
  }

  @PutMapping("/profile")
  public Mono<ProfileResponseDto> updateProfile(
      @AuthenticationPrincipal UserDto currentUser,
      @Valid @RequestBody UpdateProfileRequestDto request) {
    return userService.updateProfile(currentUser, request);
  }

  @PostMapping("/change-password")
  public Mono<Void> changePassword(
      @AuthenticationPrincipal UserDto currentUser,
      @Valid @RequestBody ChangePasswordRequestDto request) {
    return userService.changePassword(currentUser, request);
  }

  @GetMapping("/sessions")
  public Mono<List<SessionResponseDto>> listSessions(KeycloakAuthenticationToken authentication) {
    UserDto user = authentication.getPrincipal();
    String sessionId = extractSessionId(authentication);
    return keycloakService.listSessions(user.getId(), sessionId);
  }

  @DeleteMapping("/sessions/{sessionId}")
  public Mono<Void> revokeSession(@PathVariable String sessionId) {
    return keycloakService.revokeSession(sessionId);
  }

  @DeleteMapping("/sessions")
  public Mono<Void> revokeOtherSessions(KeycloakAuthenticationToken authentication) {
    UserDto user = authentication.getPrincipal();
    String sessionId = extractSessionId(authentication);
    return keycloakService.revokeOtherSessions(user.getId(), sessionId);
  }

  private String extractSessionId(KeycloakAuthenticationToken authentication) {
    Jwt jwt = (Jwt) authentication.getCredentials();
    return jwt.getClaimAsString("sid");
  }
}
