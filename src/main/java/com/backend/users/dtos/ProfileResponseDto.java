package com.backend.users.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponseDto {
  private String id;
  private String fullName;
  private String profilePictureUrl;
}
