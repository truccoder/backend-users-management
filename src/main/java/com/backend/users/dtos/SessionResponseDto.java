package com.backend.users.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponseDto {
  private String id;
  private String ipAddress;
  private long start;
  private long lastAccess;
  private boolean current;
}
