package com.backend.users.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.backend.core.dtos.ValidateTokenRequestDto;
import com.backend.users.dtos.ChangePasswordRequestDto;
import com.backend.users.dtos.ForgotPasswordRequestDto;
import com.backend.users.dtos.LoginRequestDto;
import com.backend.users.dtos.LoginResponseDto;
import com.backend.users.dtos.LogoutRequestDto;
import com.backend.users.dtos.RefreshTokenRequestDto;
import com.backend.users.dtos.RefreshTokenResponseDto;
import com.backend.users.dtos.RegisterRequestDto;
import com.backend.users.dtos.ResetPasswordRequestDto;
import com.backend.users.entities.PasswordResetTokenEntity;
import com.backend.users.entities.RefreshTokenEntity;
import com.backend.users.entities.UserEntity;

class AuthControllerTest extends BaseTest {
  @Nested
  class RegisterTests {
    @Test
    void shouldRegisterNewUser() {
      RegisterRequestDto request = new RegisterRequestDto();
      request.setEmail("newuser@test.com");
      request.setPassword("securePassword123");

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
    }

    @Test
    void shouldRejectRegistrationWithExistingEmail() {
      createUser("existing@test.com");

      RegisterRequestDto request = new RegisterRequestDto();
      request.setEmail("existing@test.com");
      request.setPassword("securePassword123");

      webTestClient
          .post()
          .uri("/v1/api/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    void shouldRejectRegistrationWithInvalidEmail() {
      RegisterRequestDto request = new RegisterRequestDto();
      request.setEmail("invalid-email");
      request.setPassword("securePassword123");

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
      createUser("login@test.com", "correctPassword");

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
                assertThat(body.getAccessToken()).isNotBlank();
                assertThat(body.getRefreshToken()).isNotBlank();
              });
    }

    @Test
    void shouldRejectLoginWithWrongPassword() {
      createUser("login@test.com", "correctPassword");

      LoginRequestDto request = new LoginRequestDto();
      request.setEmail("login@test.com");
      request.setPassword("wrongPassword");

      webTestClient
          .post()
          .uri("/v1/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isUnauthorized();
    }

    @Test
    void shouldRejectLoginWithNonExistentEmail() {
      LoginRequestDto request = new LoginRequestDto();
      request.setEmail("nonexistent@test.com");
      request.setPassword("anyPassword");

      webTestClient
          .post()
          .uri("/v1/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isUnauthorized();
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
    void shouldInitiatePasswordResetForExistingUser() {
      createUser("forgot@test.com");

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
    void shouldReturnErrorForNonExistentEmail() {
      ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
      request.setEmail("nonexistent@test.com");

      webTestClient
          .post()
          .uri("/v1/api/auth/forgot-password")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isUnauthorized();
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
  class ResetPasswordTests {
    @Test
    void shouldResetPasswordWithValidToken() {
      UserEntity user = createUser("reset@test.com", "oldPassword");
      PasswordResetTokenEntity token =
          createPasswordResetToken(user, OffsetDateTime.now().plusHours(1));

      ResetPasswordRequestDto request = new ResetPasswordRequestDto();
      request.setToken(token.getToken());
      request.setNewPassword("newSecurePassword123");

      webTestClient
          .post()
          .uri("/v1/api/auth/reset-password")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk();

      UserEntity updatedUser = findUser(user.getId());
      assertThat(passwordEncoder.matches("newSecurePassword123", updatedUser.getPassword()))
          .isTrue();
    }

    @Test
    void shouldRejectResetWithExpiredToken() {
      UserEntity user = createUser("reset@test.com");
      PasswordResetTokenEntity token =
          createPasswordResetToken(user, OffsetDateTime.now().minusHours(1));

      ResetPasswordRequestDto request = new ResetPasswordRequestDto();
      request.setToken(token.getToken());
      request.setNewPassword("newPassword");

      webTestClient
          .post()
          .uri("/v1/api/auth/reset-password")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    void shouldRejectResetWithInvalidToken() {
      ResetPasswordRequestDto request = new ResetPasswordRequestDto();
      request.setToken("invalid-token-12345");
      request.setNewPassword("newPassword");

      webTestClient
          .post()
          .uri("/v1/api/auth/reset-password")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    void shouldDeleteTokenAfterSuccessfulReset() {
      UserEntity user = createUser("reset@test.com");
      PasswordResetTokenEntity token =
          createPasswordResetToken(user, OffsetDateTime.now().plusHours(1));

      ResetPasswordRequestDto request = new ResetPasswordRequestDto();
      request.setToken(token.getToken());
      request.setNewPassword("newPassword");

      webTestClient
          .post()
          .uri("/v1/api/auth/reset-password")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk();

      PasswordResetTokenEntity deletedToken = findPasswordResetToken(token.getToken());
      assertThat(deletedToken).isNull();
    }
  }

  @Nested
  class ValidateTokenTests {
    @Test
    void shouldReturnValidForUnexpiredToken() {
      UserEntity user = createUser("validate@test.com");
      String token = generateToken(user);
      ValidateTokenRequestDto request = new ValidateTokenRequestDto(token);

      webTestClient
          .post()
          .uri("/v1/api/auth/validate-token")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.valid")
          .isEqualTo(true)
          .jsonPath("$.user.email")
          .isEqualTo("validate@test.com");
    }

    @Test
    void shouldRejectValidationWithBlankToken() {
      ValidateTokenRequestDto request = new ValidateTokenRequestDto("");
      webTestClient
          .post()
          .uri("/v1/api/auth/validate-token")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }
  }

  @Nested
  class RefreshTokenTests {
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
      testUser = createUser("testuser@test.com", "originalPassword");
    }

    @Test
    void shouldRefreshAccessToken() {
      RefreshTokenEntity refreshToken =
          createRefreshToken(testUser, OffsetDateTime.now().plusDays(7));

      RefreshTokenRequestDto request = new RefreshTokenRequestDto();
      request.setRefreshToken(refreshToken.getToken());

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
                assertThat(body.getAccessToken()).isNotBlank();
              });
    }

    @Test
    void shouldRejectRefreshWithExpiredToken() {
      RefreshTokenEntity expiredToken =
          createRefreshToken(testUser, OffsetDateTime.now().minusHours(1));

      RefreshTokenRequestDto request = new RefreshTokenRequestDto();
      request.setRefreshToken(expiredToken.getToken());

      webTestClient
          .post()
          .uri("/v1/api/auth/refresh")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isBadRequest();
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

    @Test
    void newAccessTokenShouldBeValid() {
      RefreshTokenEntity refreshToken =
          createRefreshToken(testUser, OffsetDateTime.now().plusDays(7));

      RefreshTokenRequestDto request = new RefreshTokenRequestDto();
      request.setRefreshToken(refreshToken.getToken());

      String newAccessToken =
          webTestClient
              .post()
              .uri("/v1/api/auth/refresh")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(request)
              .exchange()
              .expectStatus()
              .isOk()
              .expectBody(RefreshTokenResponseDto.class)
              .returnResult()
              .getResponseBody()
              .getAccessToken();

      webTestClient
          .post()
          .uri("/v1/api/user/change-password")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken(testUser))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(new ChangePasswordRequestDto("anotherPassword"))
          .exchange()
          .expectStatus()
          .isOk();
    }
  }

  @Nested
  class LogoutTests {
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
      testUser = createUser("testuser@test.com", "originalPassword");
    }

    @Test
    void shouldLogoutAndInvalidateRefreshToken() {
      RefreshTokenEntity refreshToken =
          createRefreshToken(testUser, OffsetDateTime.now().plusDays(7));
      LogoutRequestDto request = new LogoutRequestDto(refreshToken.getToken());

      webTestClient
          .post()
          .uri("/v1/api/auth/logout")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk();

      RefreshTokenEntity deletedToken =
          refreshTokenRepository.findByToken(refreshToken.getToken()).block();
      assertThat(deletedToken).isNull();
    }

    @Test
    void shouldHandleLogoutWithNonExistentToken() {
      LogoutRequestDto request = new LogoutRequestDto("non-existent-token");

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
      LogoutRequestDto request = new LogoutRequestDto("");
      webTestClient
          .post()
          .uri("/v1/api/user/logout")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }

    @Test
    void refreshTokenShouldNotWorkAfterLogout() {
      RefreshTokenEntity refreshToken =
          createRefreshToken(testUser, OffsetDateTime.now().plusDays(7));
      String tokenValue = refreshToken.getToken();

      LogoutRequestDto logoutRequest = new LogoutRequestDto(tokenValue);
      webTestClient
          .post()
          .uri("/v1/api/auth/logout")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(logoutRequest)
          .exchange()
          .expectStatus()
          .isOk();

      RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto();
      refreshRequest.setRefreshToken(tokenValue);

      webTestClient
          .post()
          .uri("/v1/api/auth/refresh")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(refreshRequest)
          .exchange()
          .expectStatus()
          .isBadRequest();
    }
  }
}
