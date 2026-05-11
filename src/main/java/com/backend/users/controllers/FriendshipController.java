package com.backend.users.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.backend.core.annotations.Anonymous;
import com.backend.core.dtos.UserDto;
import com.backend.users.dtos.PendingFriendRequestDto;
import com.backend.users.dtos.ProfileResponseDto;
import com.backend.users.dtos.SendFriendRequestDto;
import com.backend.users.dtos.SentFriendRequestDto;
import com.backend.users.services.FriendshipService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/api/friendships")
@RequiredArgsConstructor
public class FriendshipController {
  private final FriendshipService friendshipService;

  @PostMapping("/requests")
  public Mono<Void> sendFriendRequest(
      @AuthenticationPrincipal UserDto currentUser,
      @Valid @RequestBody SendFriendRequestDto request) {
    return friendshipService.sendFriendRequest(currentUser, request);
  }

  @PostMapping("/requests/{requestId}/accept")
  public Mono<Void> acceptFriendRequest(
      @AuthenticationPrincipal UserDto currentUser, @PathVariable String requestId) {
    return friendshipService.acceptFriendRequest(currentUser, requestId);
  }

  @PostMapping("/requests/{requestId}/reject")
  public Mono<Void> rejectFriendRequest(
      @AuthenticationPrincipal UserDto currentUser, @PathVariable String requestId) {
    return friendshipService.rejectFriendRequest(currentUser, requestId);
  }

  @PostMapping("/requests/{requestId}/cancel")
  public Mono<Void> cancelFriendRequest(
      @AuthenticationPrincipal UserDto currentUser, @PathVariable String requestId) {
    return friendshipService.cancelFriendRequest(currentUser, requestId);
  }

  @GetMapping("/requests/pending")
  public Flux<PendingFriendRequestDto> getPendingFriendRequests(
      @AuthenticationPrincipal UserDto currentUser) {
    return friendshipService.getPendingFriendRequests(currentUser.getId());
  }

  @GetMapping("/requests/sent")
  public Flux<SentFriendRequestDto> getSentFriendRequests(
      @AuthenticationPrincipal UserDto currentUser) {
    return friendshipService.getSentFriendRequests(currentUser.getId());
  }

  @GetMapping("/friends")
  public Flux<ProfileResponseDto> getFriends(@AuthenticationPrincipal UserDto currentUser) {
    return friendshipService.getFriends(currentUser.getId());
  }

  @GetMapping("/suggestions")
  public Flux<ProfileResponseDto> getFriendSuggestions(
      @AuthenticationPrincipal UserDto currentUser) {
    return friendshipService.getFriendSuggestions(currentUser.getId());
  }

  @Anonymous // only internal pod can reach
  @GetMapping("/{userId}/friends")
  public Flux<String> getFriendIds(@PathVariable String userId) {
    return friendshipService.getFriendIds(userId);
  }
}
