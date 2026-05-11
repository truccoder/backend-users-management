package com.backend.users.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDto {
  @Size(max = 255)
  private String fullName;

  @Size(max = 1024)
  private String profilePictureUrl;
}
