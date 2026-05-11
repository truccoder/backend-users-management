package com.backend.users.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequestDto {
  @NotBlank private String refreshToken;
}
