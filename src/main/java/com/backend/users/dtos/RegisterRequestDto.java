package com.backend.users.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequestDto {
  @NotBlank @Email private String email;
  @NotBlank private String password;
  @NotBlank private String fullName;
  @NotBlank private String profilePictureUrl;
}
