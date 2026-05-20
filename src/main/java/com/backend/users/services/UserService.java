package com.backend.users.services;

import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.core.dtos.UserDto;
import com.backend.core.minio.MinioService;
import com.backend.users.dtos.ChangePasswordRequestDto;
import com.backend.users.dtos.LoginResponseDto;
import com.backend.users.dtos.UpdateProfileRequestDto;
import com.backend.users.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
  private final UserRepository userRepository;
  private final KeycloakService keycloakService;
  private final MinioService minioService;

  public Mono<Void> changePassword(UserDto currentUser, ChangePasswordRequestDto request) {
    return keycloakService.updatePassword(currentUser.getId(), request.getNewPassword());
  }

  @Transactional
  public Mono<LoginResponseDto> updateProfile(
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
        .flatMap(
            user ->
                keycloakService
                    .updateUserAttributes(
                        currentUser.getId(), request.getFullName(), request.getProfilePictureUrl())
                    .then(refreshTokenPair(request.getRefreshToken())));
  }

  public Mono<LoginResponseDto> uploadProfilePicture(
      UserDto currentUser, FilePart file, String refreshToken) {
    String objectName =
        "profile-pictures/" + currentUser.getId() + "/" + UUID.randomUUID() + "-" + file.filename();

    return DataBufferUtils.join(file.content())
        .map(
            dataBuffer -> {
              byte[] bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              DataBufferUtils.release(dataBuffer);
              return bytes;
            })
        .flatMap(
            bytes ->
                minioService.uploadObject(
                    objectName, bytes, file.headers().getContentType().toString()))
        .flatMap(
            url ->
                userRepository
                    .findById(currentUser.getId())
                    .flatMap(
                        user -> {
                          user.setProfilePictureUrl(url);
                          return userRepository.save(user);
                        })
                    .then(keycloakService.updateUserAttributes(currentUser.getId(), null, url))
                    .then(refreshTokenPair(refreshToken)));
  }

  private Mono<LoginResponseDto> refreshTokenPair(String refreshToken) {
    return keycloakService
        .refreshToken(refreshToken)
        .map(token -> new LoginResponseDto(token.accessToken(), token.refreshToken()));
  }
}
