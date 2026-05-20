package com.backend.users.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.backend.users.dtos.ForgotPasswordRequestDto;
import com.backend.users.dtos.LoginRequestDto;
import com.backend.users.dtos.LoginResponseDto;
import com.backend.users.dtos.LogoutRequestDto;
import com.backend.users.dtos.RefreshTokenRequestDto;
import com.backend.users.dtos.RefreshTokenResponseDto;
import com.backend.users.dtos.RegisterRequestDto;
import com.backend.users.entities.UserEntity;
import com.backend.users.services.KeycloakService.KeycloakTokenResponse;

import reactor.core.publisher.Mono;

class AuthControllerTest extends BaseTest {

  @Nested
  class RegisterTests {
    @Test
    void shouldRegisterNewUser() {
      String keycloakUserId = UUID.randomUUID().toString();
      when(keycloakService.createUser(anyString(), anyString(), any(), any()))
          .thenReturn(Mono.just(keycloakUserId));

      RegisterRequestDto request = new RegisterRequestDto();
      request.setEmail("newuser@test.com");
      request.setPassword("securePassword123");
      request.setFullName("New User");
      request.setProfilePictureUrl("https://example.com/pic.jpg");

      webTestClient
          .post()
          .uri("/v1/api/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk();

      UserEntity savedUser = userRepository.findByEmail("newuser@test.com").block();
      assertThat(savedUser).isNotNull();
      assertThat(savedUser.getEmail()).isEqualTo("newuser@test.com");
      assertThat(savedUser.getId()).isEqualTo(keycloakUserId);
    }

    @Test
    void shouldRejectRegistrationWithInvalidEmail() {
      RegisterRequestDto request = new RegisterRequestDto();
      request.setEmail("invalid-email");
      request.setPassword("securePassword123");
      request.setFullName("Test");
      request.setProfilePictureUrl("https://example.com/pic.jpg");

      webTestClient
          .post()
          .uri("/v1/api/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }

    @Test
    void shouldRejectRegistrationWithBlankEmail() {
      RegisterRequestDto request = new RegisterRequestDto();
      request.setEmail("");
      request.setPassword("securePassword123");
      request.setFullName("Test");
      request.setProfilePictureUrl("https://example.com/pic.jpg");

      webTestClient
          .post()
          .uri("/v1/api/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }

    @Test
    void shouldRejectRegistrationWithBlankPassword() {
      RegisterRequestDto request = new RegisterRequestDto();
      request.setEmail("test@test.com");
      request.setPassword("");
      request.setFullName("Test");
      request.setProfilePictureUrl("https://example.com/pic.jpg");

      webTestClient
          .post()
          .uri("/v1/api/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }
  }

  @Nested
  class LoginTests {
    @Test
    void shouldLoginWithValidCredentials() {
      KeycloakTokenResponse tokenResponse =
          new KeycloakTokenResponse("access-token", "refresh-token", 900, 604800, "Bearer");
      when(keycloakService.login(anyString(), anyString())).thenReturn(Mono.just(tokenResponse));

      LoginRequestDto request = new LoginRequestDto();
      request.setEmail("login@test.com");
      request.setPassword("correctPassword");

      webTestClient
          .post()
          .uri("/v1/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(LoginResponseDto.class)
          .consumeWith(
              response -> {
                LoginResponseDto body = response.getResponseBody();
                assertThat(body).isNotNull();
                assertThat(body.getAccessToken()).isEqualTo("access-token");
                assertThat(body.getRefreshToken()).isEqualTo("refresh-token");
              });
    }

    @Test
    void shouldRejectLoginWithInvalidEmail() {
      LoginRequestDto request = new LoginRequestDto();
      request.setEmail("invalid-email");
      request.setPassword("anyPassword");

      webTestClient
          .post()
          .uri("/v1/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }
  }

  @Nested
  class ForgotPasswordTests {
    @Test
    void shouldInitiatePasswordReset() {
      when(keycloakService.sendPasswordResetEmail(anyString())).thenReturn(Mono.empty());

      ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
      request.setEmail("forgot@test.com");

      webTestClient
          .post()
          .uri("/v1/api/auth/forgot-password")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk();
    }

    @Test
    void shouldRejectWithInvalidEmailFormat() {
      ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
      request.setEmail("invalid-email");

      webTestClient
          .post()
          .uri("/v1/api/auth/forgot-password")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }
  }

  @Nested
  class RefreshTokenTests {
    @Test
    void shouldRefreshAccessToken() {
      KeycloakTokenResponse tokenResponse =
          new KeycloakTokenResponse("new-access-token", "new-refresh-token", 900, 604800, "Bearer");
      when(keycloakService.refreshToken(anyString())).thenReturn(Mono.just(tokenResponse));

      RefreshTokenRequestDto request = new RefreshTokenRequestDto();
      request.setRefreshToken("valid-refresh-token");

      webTestClient
          .post()
          .uri("/v1/api/auth/refresh")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(RefreshTokenResponseDto.class)
          .consumeWith(
              response -> {
                RefreshTokenResponseDto body = response.getResponseBody();
                assertThat(body).isNotNull();
                assertThat(body.getAccessToken()).isEqualTo("new-access-token");
              });
    }

    @Test
    void shouldRejectRefreshWithBlankToken() {
      RefreshTokenRequestDto request = new RefreshTokenRequestDto();
      request.setRefreshToken("");

      webTestClient
          .post()
          .uri("/v1/api/auth/refresh")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }
  }

  @Nested
  class LogoutTests {
    @Test
    void shouldLogoutSuccessfully() {
      when(keycloakService.logout(anyString())).thenReturn(Mono.empty());

      LogoutRequestDto request = new LogoutRequestDto();
      request.setRefreshToken("valid-refresh-token");

      webTestClient
          .post()
          .uri("/v1/api/auth/logout")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk();
    }

    @Test
    void shouldRejectLogoutWithBlankToken() {
      LogoutRequestDto request = new LogoutRequestDto();
      request.setRefreshToken("");

      webTestClient
          .post()
          .uri("/v1/api/auth/logout")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }
  }
}
