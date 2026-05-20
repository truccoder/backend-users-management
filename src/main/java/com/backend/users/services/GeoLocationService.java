package com.backend.users.services;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class GeoLocationService {
  private static final String IP_API_URL = "http://ip-api.com/json/";

  private final WebClient webClient;

  public GeoLocationService(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.baseUrl(IP_API_URL).build();
  }

  @SuppressWarnings("unchecked")
  public Mono<String> resolveCity(String ipAddress) {
    if (ipAddress == null || ipAddress.isBlank() || isLocalAddress(ipAddress)) {
      return Mono.just("Unknown");
    }

    return webClient
        .get()
        .uri(ipAddress)
        .retrieve()
        .bodyToMono(Map.class)
        .map(
            response -> {
              String status = (String) response.get("status");
              if ("success".equals(status)) {
                return (String) response.getOrDefault("city", "Unknown");
              }
              return "Unknown";
            })
        .onErrorReturn("Unknown");
  }

  private boolean isLocalAddress(String ip) {
    return ip.startsWith("127.")
        || ip.startsWith("0.")
        || ip.equals("::1")
        || ip.startsWith("10.")
        || ip.startsWith("172.")
        || ip.startsWith("192.168.");
  }
}
