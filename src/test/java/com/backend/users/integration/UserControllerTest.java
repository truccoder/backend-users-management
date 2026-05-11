package com.backend.users.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.backend.users.dtos.ChangePasswordRequestDto;
import com.backend.users.entities.UserEntity;

class UserControllerTest extends BaseTest {
  private UserEntity testUser;

  @BeforeEach
  void setUp() {
    testUser = createUser("testuser@test.com", "originalPassword");
  }

  @Nested
  class ChangePasswordTests {
    @Test
    void shouldChangePassword() {
      ChangePasswordRequestDto request = new ChangePasswordRequestDto();
      request.setNewPassword("newSecurePassword123");

      webTestClient
          .post()
          .uri("/v1/api/user/change-password")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(testUser))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk();

      UserEntity updatedUser =
          transactionalOperator.transactional(userRepository.findById(testUser.getId())).block();
      assertThat(passwordEncoder.matches("newSecurePassword123", updatedUser.getPassword()))
          .isTrue();
    }

    @Test
    void shouldRejectChangePasswordWithoutAuth() {
      ChangePasswordRequestDto request = new ChangePasswordRequestDto();
      request.setNewPassword("newPassword");

      webTestClient
          .post()
          .uri("/v1/api/user/change-password")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isUnauthorized();
    }

    @Test
    void shouldRejectChangePasswordWithBlankNewPassword() {
      ChangePasswordRequestDto request = new ChangePasswordRequestDto();
      request.setNewPassword("");

      webTestClient
          .post()
          .uri("/v1/api/user/change-password")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(testUser))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }
  }
}
