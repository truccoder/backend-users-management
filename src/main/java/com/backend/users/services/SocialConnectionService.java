package com.backend.users.services;

import org.springframework.stereotype.Service;

import com.backend.core.dtos.UserDto;
import com.backend.core.web.page.Page;
import com.backend.users.dtos.BlockPayloadDto;
import com.backend.users.dtos.FollowPayloadDto;
import com.backend.users.dtos.ProfileResponseDto;
import com.backend.users.dtos.UnblockPayloadDto;
import com.backend.users.dtos.UnfollowPayloadDto;
import com.backend.users.kafka.KafkaPublisher;
import com.backend.users.mappers.FriendMapper;
import com.backend.users.repositories.UserNodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialConnectionService {
  private final UserNodeRepository userNodeRepository;
  private final FriendMapper friendMapper;
  private final KafkaPublisher kafkaPublisher;

  public Mono<Void> follow(UserDto currentUser, String followedId) {
    FollowPayloadDto payload = new FollowPayloadDto(currentUser.getId(), followedId);
    return kafkaPublisher.sendFollowEvent(payload);
  }

  public Mono<Void> unfollow(UserDto currentUser, String followedId) {
    UnfollowPayloadDto payload = new UnfollowPayloadDto(currentUser.getId(), followedId);
    return kafkaPublisher.sendUnfollowEvent(payload);
  }

  public Mono<Page<ProfileResponseDto>> getFollowing(
      UserDto currentUser, Long offset, Integer pageSize) {
    String userId = currentUser.getId();

    return userNodeRepository
        .findFollowingPaginated(userId, offset, pageSize)
        .map(friendMapper::toProfileResponseDto)
        .collectList()
        .zipWith(userNodeRepository.countFollowing(userId))
        .map(tuple -> friendMapper.toUserDtoPage(tuple.getT1(), tuple.getT2()));
  }

  public Mono<Page<ProfileResponseDto>> getFollowers(String userId, Long offset, Integer pageSize) {
    return userNodeRepository
        .findFollowersPaginated(userId, offset, pageSize)
        .map(friendMapper::toProfileResponseDto)
        .collectList()
        .zipWith(userNodeRepository.countFollowers(userId))
        .map(tuple -> friendMapper.toUserDtoPage(tuple.getT1(), tuple.getT2()));
  }

  // TODO: enhance this by updating t_friend_requests to status BLOCK
  // so later they sent/accept friend request, we allow it
  // consider: let cdc capture status BLOCK and handle in neo4j
  public Mono<Void> block(UserDto currentUser, String blockedId) {
    BlockPayloadDto payload = new BlockPayloadDto(currentUser.getId(), blockedId);
    return kafkaPublisher.sendBlockEvent(payload);
  }

  public Mono<Void> unblock(UserDto currentUser, String blockedId) {
    UnblockPayloadDto payload = new UnblockPayloadDto(currentUser.getId(), blockedId);
    return kafkaPublisher.sendUnblockEvent(payload);
  }

  public Mono<Page<ProfileResponseDto>> getBlockedUsers(
      UserDto currentUser, Long offset, Integer pageSize) {
    String userId = currentUser.getId();
    return userNodeRepository
        .findBlockedUsersPaginated(userId, offset, pageSize)
        .map(friendMapper::toProfileResponseDto)
        .collectList()
        .zipWith(userNodeRepository.countBlockedUsers(userId))
        .map(tuple -> friendMapper.toUserDtoPage(tuple.getT1(), tuple.getT2()));
  }
}
