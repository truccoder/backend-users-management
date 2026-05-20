package com.backend.users.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.core.cache.ReactiveCacheTemplate;
import com.backend.core.dtos.UserDto;
import com.backend.users.dtos.ChangePasswordRequestDto;
import com.backend.users.dtos.ProfileResponseDto;
import com.backend.users.dtos.UpdateProfileRequestDto;
import com.backend.users.mappers.UserMapper;
import com.backend.users.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final KeycloakService keycloakService;
  private final ReactiveCacheTemplate<ProfileResponseDto> userProfileCache;

  public Mono<Void> changePassword(UserDto currentUser, ChangePasswordRequestDto request) {
    return keycloakService.updatePassword(currentUser.getId(), request.getNewPassword());
  }

  @Transactional
  public Mono<ProfileResponseDto> updateProfile(
      UserDto currentUser, UpdateProfileRequestDto request) {
    return userRepository
        .findById(currentUser.getId())
        .flatMap(
            user -> {
              Optional.ofNullable(request.getFullName()).ifPresent(user::setFullName);
              Optional.ofNullable(request.getProfilePictureUrl())
                  .ifPresent(user::setProfilePictureUrl);
              return userRepository.save(user);
            })
        .map(userMapper::toProfileResponseDto)
        .flatMap(profile -> userProfileCache.put(currentUser.getId(), profile).thenReturn(profile));
  }

  public Mono<ProfileResponseDto> getProfile(UserDto currentUser) {
    return userProfileCache.get(currentUser.getId(), this::loadProfileFromDb);
  }

  private Mono<ProfileResponseDto> loadProfileFromDb(String userId) {
    return userRepository.findById(userId).map(userMapper::toProfileResponseDto);
  }
}
