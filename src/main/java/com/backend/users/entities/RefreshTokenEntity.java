package com.backend.users.entities;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("t_refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity implements Persistable<String> {
  @Id private String token;
  private String userId;
  private OffsetDateTime expiresAt;

  @Override
  public String getId() {
    return token;
  }

  @Override
  public boolean isNew() {
    // Refresh token never updated
    return true;
  }
}
