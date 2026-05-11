package com.backend.users.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.core.dtos.UserDto;
import com.backend.users.dtos.ChangePasswordRequestDto;
import com.backend.users.dtos.ListSessionsRequestDto;
import com.backend.users.dtos.ProfileResponseDto;
import com.backend.users.dtos.RevokeSessionRequestDto;
import com.backend.users.dtos.RevokeSessionsRequestDto;
import com.backend.users.dtos.SessionResponseDto;
import com.backend.users.dtos.UpdateProfileRequestDto;
import com.backend.users.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/api/me")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

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

  @PostMapping("/sessions")
  public Flux<SessionResponseDto> listSessions(
      @AuthenticationPrincipal UserDto currentUser,
      @Valid @RequestBody ListSessionsRequestDto request) {
    return userService.listSessions(currentUser, request);
  }

  @PostMapping("/sessions/revoke-others")
  public Mono<Void> revokeOtherSessions(
      @AuthenticationPrincipal UserDto currentUser,
      @Valid @RequestBody RevokeSessionsRequestDto request) {
    return userService.revokeOtherSessions(currentUser, request);
  }

  @PostMapping("/sessions/revoke")
  public Mono<Void> revokeSession(
      @AuthenticationPrincipal UserDto currentUser,
      @Valid @RequestBody RevokeSessionRequestDto request) {
    return userService.revokeSession(currentUser, request);
  }
}
