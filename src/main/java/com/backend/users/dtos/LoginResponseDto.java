package com.backend.users.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDto {
  private String accessToken;
  private String refreshToken;
}
