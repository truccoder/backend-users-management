package com.backend.users.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevokeSessionRequestDto {
  @NotBlank private String currentRefreshToken;
  @NotBlank private String targetRefreshToken;
}
