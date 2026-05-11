package com.backend.users.services;

import static com.backend.users.utils.Constants.DEFAULT_PAGINATION;
import static com.backend.users.utils.Constants.FRIEND_REQUEST_RESOURCE_NAME;
import static com.backend.users.utils.Constants.USER_RESOURCE_NAME;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.core.cache.ReactiveCacheTemplate;
import com.backend.core.dtos.UserDto;
import com.backend.core.exceptions.ForbiddenException;
import com.backend.core.exceptions.ResourceNotFoundException;
import com.backend.core.exceptions.ValidationException;
import com.backend.core.tsid.TsidGenerator;
import com.backend.users.dtos.PendingFriendRequestDto;
import com.backend.users.dtos.ProfileResponseDto;
import com.backend.users.dtos.SendFriendRequestDto;
import com.backend.users.dtos.SentFriendRequestDto;
import com.backend.users.entities.FriendRequestEntity;
import com.backend.users.enums.FriendRequestStatus;
import com.backend.users.mappers.FriendMapper;
import com.backend.users.repositories.FriendRequestRepository;
import com.backend.users.repositories.UserNodeRepository;
import com.backend.users.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class FriendshipService {
  private final FriendRequestRepository friendRequestRepository;
  private final UserRepository userRepository;
  private final UserNodeRepository userNodeRepository;
  private final FriendMapper friendMapper;
  private final TsidGenerator tsidGenerator;

  private final ReactiveCacheTemplate<List<ProfileResponseDto>> friendsCache;
  private final ReactiveCacheTemplate<List<ProfileResponseDto>> suggestionsCache;

  @Transactional
  public Mono<Void> sendFriendRequest(UserDto currentUser, SendFriendRequestDto request) {
    String requesterId = currentUser.getId();
    String addresseeId = request.getAddresseeId();

    return userRepository
        .findById(addresseeId)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException(addresseeId, USER_RESOURCE_NAME)))
        .flatMap(
            addressee ->
                validateSending(requesterId, addresseeId)
                    .then(
                        Mono.defer(
                            () -> {
                              FriendRequestEntity friendRequest = new FriendRequestEntity();
                              friendRequest.setId(tsidGenerator.generate());
                              friendRequest.setRequesterId(requesterId);
                              friendRequest.setAddresseeId(addresseeId);
                              friendRequest.setStatus(FriendRequestStatus.PENDING);
                              return friendRequestRepository.save(friendRequest);
                            })))
        .then();
  }

  @Transactional
  public Mono<Void> acceptFriendRequest(UserDto currentUser, String requestId) {
    String currentUserId = currentUser.getId();

    return findFriendRequestById(requestId)
        .flatMap(
            fr -> {
              validateDefaultOperations(fr, currentUserId);
              fr.setStatus(FriendRequestStatus.ACCEPTED);
              return friendRequestRepository
                  .save(fr)
                  .then(
                      Mono.when(
                          evictFriendsCaches(fr.getRequesterId(), currentUserId),
                          evictSuggestionsCaches(fr.getRequesterId(), currentUserId)));
            });
  }

  @Transactional
  public Mono<Void> rejectFriendRequest(UserDto currentUser, String requestId) {
    return findFriendRequestById(requestId)
        .flatMap(
            fr -> {
              validateDefaultOperations(fr, currentUser.getId());
              fr.setStatus(FriendRequestStatus.REJECTED);
              return friendRequestRepository.save(fr);
            })
        .then();
  }

  @Transactional
  public Mono<Void> cancelFriendRequest(UserDto currentUser, String requestId) {
    return findFriendRequestById(requestId)
        .flatMap(
            fr -> {
              validateCanceling(fr, currentUser.getId());
              fr.setStatus(FriendRequestStatus.CANCELLED);
              return friendRequestRepository.save(fr);
            })
        .then();
  }

  public Flux<PendingFriendRequestDto> getPendingFriendRequests(String userId) {
    return friendRequestRepository
        .findPendingFriendRequests(userId, FriendRequestStatus.PENDING)
        .map(friendMapper::toPendingFriendRequestDto);
  }

  public Flux<SentFriendRequestDto> getSentFriendRequests(String userId) {
    return friendRequestRepository
        .findSentFriendRequests(userId, FriendRequestStatus.PENDING)
        .map(friendMapper::toSentFriendRequestDto);
  }

  public Flux<ProfileResponseDto> getFriends(String userId) {
    return friendsCache.get(userId, this::loadFriendsFromNeo4j).flatMapMany(Flux::fromIterable);
  }

  public Flux<ProfileResponseDto> getFriendSuggestions(String userId) {
    return suggestionsCache
        .get(userId, this::loadSuggestionsFromNeo4j)
        .flatMapMany(Flux::fromIterable);
  }

  private Mono<FriendRequestEntity> findFriendRequestById(String id) {
    return friendRequestRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException(id, FRIEND_REQUEST_RESOURCE_NAME)));
  }

  private Mono<Void> validateSending(String requesterId, String addresseeId) {
    if (requesterId.equals(addresseeId)) {
      return Mono.error(new ValidationException("Cannot send friend request to yourself"));
    }

    return friendRequestRepository
        .areFriends(requesterId, addresseeId)
        .flatMap(
            areFriends -> {
              if (areFriends) {
                return Mono.error(new ValidationException("Users are already friends"));
              }
              return friendRequestRepository
                  .findByIdAndStatus(requesterId, addresseeId, FriendRequestStatus.PENDING.name())
                  .flatMap(
                      fr ->
                          Mono.<Void>error(new ValidationException("Friend request already sent")))
                  .switchIfEmpty(Mono.empty());
            });
  }

  private void validateDefaultOperations(FriendRequestEntity fr, String addresseeId) {
    if (!fr.getAddresseeId().equals(addresseeId)) {
      throw new ForbiddenException("You are not authorized to operate this request");
    }
    validateFriendRequestPending(fr);
  }

  private void validateCanceling(FriendRequestEntity fr, String requesterId) {
    if (!fr.getRequesterId().equals(requesterId)) {
      throw new ForbiddenException("You are not authorized to cancel this request");
    }
    validateFriendRequestPending(fr);
  }

  private void validateFriendRequestPending(FriendRequestEntity friendRequest) {
    if (!FriendRequestStatus.PENDING.equals(friendRequest.getStatus())) {
      throw new ValidationException("Friend request is not pending");
    }
  }

  private Mono<List<ProfileResponseDto>> loadFriendsFromNeo4j(String id) {
    return userNodeRepository
        .findFriendsByUserId(id)
        .map(friendMapper::toProfileResponseDto)
        .collectList();
  }

  private Mono<List<ProfileResponseDto>> loadSuggestionsFromNeo4j(String id) {
    return userNodeRepository
        .findFriendsOfFriends(id, DEFAULT_PAGINATION)
        .map(friendMapper::toProfileResponseDto)
        .collectList();
  }

  private Mono<Void> evictFriendsCaches(String userId1, String userId2) {
    return Mono.when(friendsCache.evict(userId1), friendsCache.evict(userId2));
  }

  private Mono<Void> evictSuggestionsCaches(String userId1, String userId2) {
    return Mono.when(suggestionsCache.evict(userId1), suggestionsCache.evict(userId2));
  }

  public Flux<String> getFriendIds(String userId) {
    return getFriends(userId).map(ProfileResponseDto::getId);
  }
}
