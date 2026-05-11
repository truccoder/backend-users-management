package com.backend.users.controllers;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.core.annotations.Anonymous;
import com.backend.users.security.RsaKeyProperties;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Anonymous
@RestController
@RequiredArgsConstructor
public class JwksController {
  private final RsaKeyProperties rsaKeyProperties;

  @GetMapping("/.well-known/jwks.json")
  public Mono<Map<String, Object>> jwks() {
    RSAPublicKey publicKey = rsaKeyProperties.getPublicKey();

    Map<String, Object> jwk =
        Map.of(
            "kty", "RSA",
            "use", "sig",
            "alg", "RS256",
            "kid", "default",
            "n", base64UrlEncode(publicKey.getModulus().toByteArray()),
            "e", base64UrlEncode(publicKey.getPublicExponent().toByteArray()));

    return Mono.just(Map.of("keys", List.of(jwk)));
  }

  private String base64UrlEncode(byte[] bytes) {
    // Strip leading zero byte if present (BigInteger sign bit)
    if (bytes.length > 0 && bytes[0] == 0) {
      byte[] stripped = new byte[bytes.length - 1];
      System.arraycopy(bytes, 1, stripped, 0, stripped.length);
      bytes = stripped;
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
