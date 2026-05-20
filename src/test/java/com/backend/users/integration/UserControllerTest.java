package com.backend.users.integration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.backend.users.dtos.ChangePasswordRequestDto;
import com.backend.users.entities.UserEntity;

import reactor.core.publisher.Mono;

class UserControllerTest extends BaseTest {
  private UserEntity testUser;

  @BeforeEach
  void setUp() {
    testUser = createUser("testuser@test.com");
  }

  @Nested
  class ChangePasswordTests {
    @Test
    void shouldChangePassword() {
      when(keycloakService.updatePassword(anyString(), anyString())).thenReturn(Mono.empty());

      ChangePasswordRequestDto request = new ChangePasswordRequestDto();
      request.setNewPassword("newSecurePassword123");

      webTestClient
          .post()
          .uri("/v1/api/me/change-password")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(testUser))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk();
    }

    @Test
    void shouldRejectChangePasswordWithoutAuth() {
      ChangePasswordRequestDto request = new ChangePasswordRequestDto();
      request.setNewPassword("newPassword");

      webTestClient
          .post()
          .uri("/v1/api/me/change-password")
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
          .uri("/v1/api/me/change-password")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(testUser))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }
  }
}
