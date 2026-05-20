package com.backend.users.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
    String serverUrl, String realm, String clientId, String clientSecret) {

  public String tokenEndpoint() {
    return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
  }

  public String logoutEndpoint() {
    return serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
  }

  public String adminUsersEndpoint() {
    return serverUrl + "/admin/realms/" + realm + "/users";
  }

  public String adminSessionsEndpoint() {
    return serverUrl + "/admin/realms/" + realm + "/sessions";
  }
}
