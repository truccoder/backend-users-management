package com.backend.users.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.backend.users.entities.UserEntity;
import com.backend.users.enums.JwtPayloadFields;
import com.backend.users.security.RsaKeyProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Component
public class JwtUtil {
  private final RsaKeyProperties rsaKeyProperties;

  @Value("${jwt.access-token-expiration}")
  private Long expiration;

  public JwtUtil(RsaKeyProperties rsaKeyProperties) {
    this.rsaKeyProperties = rsaKeyProperties;
  }

  public String generateToken(UserEntity user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(JwtPayloadFields.EMAIL.getName(), user.getEmail());
    claims.put(JwtPayloadFields.ID.getName(), user.getId());
    return createToken(claims, user.getUsername());
  }

  public Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private String createToken(Map<String, Object> claims, String subject) {
    Date now = new Date();
    Date expirationDate = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(now)
        .expiration(expirationDate)
        .signWith(rsaKeyProperties.getPrivateKey())
        .compact();
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(rsaKeyProperties.getPublicKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public Map<String, Object> extractPayload(String token) {
    Claims claims = extractAllClaims(token);
    Map<String, Object> customClaims = new HashMap<>();

    for (JwtPayloadFields field : JwtPayloadFields.values()) {
      Object value = claims.get(field.getName());
      if (Objects.nonNull(value)) {
        customClaims.put(field.getName(), value);
      }
    }
    return customClaims;
  }
}
