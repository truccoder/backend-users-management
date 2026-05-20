package com.backend.users.integration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import com.backend.users.entities.UserEntity;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@TestConfiguration
public class TestSecurityConfig {
  private static final KeyPair KEY_PAIR = generateKeyPair();

  @Bean
  @Primary
  public ReactiveJwtDecoder reactiveJwtDecoder() {
    return NimbusReactiveJwtDecoder.withPublicKey((RSAPublicKey) KEY_PAIR.getPublic()).build();
  }

  public static String generateToken(UserEntity user) {
    try {
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .subject(user.getId())
              .claim("email", user.getEmail())
              .issueTime(new Date())
              .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
              .jwtID(UUID.randomUUID().toString())
              .build();

      SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
      signedJWT.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
      return signedJWT.serialize();
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate test JWT", e);
    }
  }

  private static KeyPair generateKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate RSA key pair for tests", e);
    }
  }
}
