package com.backend.users.dtos;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponseDto {
  private String token;
  private OffsetDateTime expiresAt;
  private OffsetDateTime createdAt;
  private boolean current;
}
