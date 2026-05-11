package com.backend.users.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Getter
public class RsaKeyProperties {
  private final RSAPublicKey publicKey;
  private final RSAPrivateKey privateKey;

  public RsaKeyProperties(
      @Value("${jwt.rsa.public-key}") String publicKeyPem,
      @Value("${jwt.rsa.private-key}") String privateKeyPem) {
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");

      String publicKeyContent =
          publicKeyPem
              .replace("-----BEGIN PUBLIC KEY-----", "")
              .replace("-----END PUBLIC KEY-----", "")
              .replaceAll("\\s+", "");
      byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyContent);
      this.publicKey =
          (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

      String privateKeyContent =
          privateKeyPem
              .replace("-----BEGIN PRIVATE KEY-----", "")
              .replace("-----END PRIVATE KEY-----", "")
              .replaceAll("\\s+", "");
      byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyContent);
      this.privateKey =
          (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

      log.info("RSA key pair loaded successfully");
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load RSA keys", e);
    }
  }
}
