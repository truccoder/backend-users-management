package com.backend.users.services;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.backend.core.exceptions.ValidationException;
import com.backend.users.dtos.SessionResponseDto;
import com.backend.users.security.KeycloakProperties;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class KeycloakService {
  private final WebClient webClient;
  private final KeycloakProperties properties;

  public KeycloakService(WebClient.Builder webClientBuilder, KeycloakProperties properties) {
    this.webClient = webClientBuilder.build();
    this.properties = properties;
  }

  public Mono<String> createUser(String email, String password, String fullName) {
    Map<String, Object> userRepresentation =
        Map.of(
            "username",
            email,
            "email",
            email,
            "firstName",
            fullName,
            "enabled",
            true,
            "emailVerified",
            true,
            "credentials",
            List.of(Map.of("type", "password", "value", password, "temporary", false)));

    return getAdminAccessToken()
        .flatMap(
            adminToken ->
                webClient
                    .post()
                    .uri(properties.adminUsersEndpoint())
                    .headers(h -> h.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userRepresentation)
                    .retrieve()
                    .toBodilessEntity()
                    .flatMap(
                        response -> {
                          String location = response.getHeaders().getFirst("Location");
                          if (Objects.isNull(location)) {
                            return Mono.error(
                                new ValidationException("Failed to extract user ID from Keycloak"));
                          }
                          return Mono.just(location.substring(location.lastIndexOf("/") + 1));
                        }))
        .onErrorResume(
            WebClientResponseException.Conflict.class,
            e -> Mono.error(new ValidationException("Email already exists")));
  }

  public Mono<KeycloakTokenResponse> login(String email, String password) {
    return webClient
        .post()
        .uri(properties.tokenEndpoint())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(
            BodyInserters.fromFormData("grant_type", "password")
                .with("client_id", properties.clientId())
                .with("client_secret", properties.clientSecret())
                .with("username", email)
                .with("password", password)
                .with("scope", "openid email profile"))
        .retrieve()
        .bodyToMono(KeycloakTokenResponse.class)
        .onErrorResume(
            WebClientResponseException.Unauthorized.class,
            e -> Mono.error(new ValidationException("Invalid email or password")));
  }

  public Mono<KeycloakTokenResponse> refreshToken(String refreshToken) {
    return webClient
        .post()
        .uri(properties.tokenEndpoint())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(
            BodyInserters.fromFormData("grant_type", "refresh_token")
                .with("client_id", properties.clientId())
                .with("client_secret", properties.clientSecret())
                .with("refresh_token", refreshToken))
        .retrieve()
        .bodyToMono(KeycloakTokenResponse.class)
        .onErrorResume(
            WebClientResponseException.BadRequest.class,
            e -> Mono.error(new ValidationException("Invalid or expired refresh token")));
  }

  public Mono<Void> logout(String refreshToken) {
    return webClient
        .post()
        .uri(properties.logoutEndpoint())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(
            BodyInserters.fromFormData("client_id", properties.clientId())
                .with("client_secret", properties.clientSecret())
                .with("refresh_token", refreshToken))
        .retrieve()
        .toBodilessEntity()
        .then();
  }

  public Mono<Void> sendPasswordResetEmail(String email) {
    return getAdminAccessToken()
        .flatMap(adminToken -> findUserByEmail(adminToken, email))
        .flatMap(
            result -> {
              String adminToken = result.adminToken();
              String userId = result.userId();
              return webClient
                  .put()
                  .uri(properties.adminUsersEndpoint() + "/" + userId + "/execute-actions-email")
                  .headers(h -> h.setBearerAuth(adminToken))
                  .contentType(MediaType.APPLICATION_JSON)
                  .bodyValue(List.of("UPDATE_PASSWORD"))
                  .retrieve()
                  .toBodilessEntity()
                  .then();
            });
  }

  public Mono<Void> updatePassword(String keycloakUserId, String newPassword) {
    Map<String, Object> credential =
        Map.of("type", "password", "value", newPassword, "temporary", false);

    return getAdminAccessToken()
        .flatMap(
            adminToken ->
                webClient
                    .put()
                    .uri(properties.adminUsersEndpoint() + "/" + keycloakUserId + "/reset-password")
                    .headers(h -> h.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(credential)
                    .retrieve()
                    .toBodilessEntity()
                    .then());
  }

  public Mono<List<SessionResponseDto>> listSessions(
      String keycloakUserId, String currentSessionId) {
    return getAdminAccessToken()
        .flatMap(
            adminToken ->
                webClient
                    .get()
                    .uri(properties.adminUsersEndpoint() + "/" + keycloakUserId + "/sessions")
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .map(
                        sessions ->
                            sessions.stream()
                                .map(s -> toSessionResponse(s, currentSessionId))
                                .toList()));
  }

  public Mono<Void> revokeSession(String sessionId) {
    return getAdminAccessToken()
        .flatMap(
            adminToken ->
                webClient
                    .delete()
                    .uri(properties.adminSessionsEndpoint() + "/" + sessionId)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .toBodilessEntity()
                    .then());
  }

  public Mono<Void> revokeOtherSessions(String keycloakUserId, String currentSessionId) {
    return getAdminAccessToken()
        .flatMap(
            adminToken ->
                webClient
                    .get()
                    .uri(properties.adminUsersEndpoint() + "/" + keycloakUserId + "/sessions")
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .flatMapMany(Flux::fromIterable)
                    .filter(session -> !currentSessionId.equals(session.get("id")))
                    .flatMap(
                        session -> {
                          String sessionId = session.get("id").toString();
                          return webClient
                              .delete()
                              .uri(properties.adminSessionsEndpoint() + "/" + sessionId)
                              .headers(h -> h.setBearerAuth(adminToken))
                              .retrieve()
                              .toBodilessEntity()
                              .then();
                        })
                    .then());
  }

  private SessionResponseDto toSessionResponse(
      Map<String, Object> session, String currentSessionId) {
    String id = session.get("id").toString();
    return SessionResponseDto.builder()
        .id(id)
        .ipAddress((String) session.get("ipAddress"))
        .start(((Number) session.get("start")).longValue())
        .lastAccess(((Number) session.get("lastAccess")).longValue())
        .current(id.equals(currentSessionId))
        .build();
  }

  private Mono<String> getAdminAccessToken() {
    return webClient
        .post()
        .uri(properties.tokenEndpoint())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(
            BodyInserters.fromFormData("grant_type", "client_credentials")
                .with("client_id", properties.clientId())
                .with("client_secret", properties.clientSecret()))
        .retrieve()
        .bodyToMono(KeycloakTokenResponse.class)
        .map(KeycloakTokenResponse::accessToken);
  }

  private Mono<AdminUserResult> findUserByEmail(String adminToken, String email) {
    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(properties.adminUsersEndpoint())
                    .queryParam("email", email)
                    .queryParam("exact", true)
                    .build())
        .headers(h -> h.setBearerAuth(adminToken))
        .retrieve()
        .bodyToMono(List.class)
        .flatMap(
            users -> {
              if (users.isEmpty()) {
                return Mono.error(new ValidationException("User not found"));
              }
              @SuppressWarnings("unchecked")
              Map<String, Object> user = (Map<String, Object>) users.get(0);
              return Mono.just(new AdminUserResult(adminToken, user.get("id").toString()));
            });
  }

  private record AdminUserResult(String adminToken, String userId) {}

  public record KeycloakTokenResponse(
      @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken,
      @com.fasterxml.jackson.annotation.JsonProperty("refresh_token") String refreshToken,
      @com.fasterxml.jackson.annotation.JsonProperty("expires_in") int expiresIn,
      @com.fasterxml.jackson.annotation.JsonProperty("refresh_expires_in") int refreshExpiresIn,
      @com.fasterxml.jackson.annotation.JsonProperty("token_type") String tokenType) {}
}
