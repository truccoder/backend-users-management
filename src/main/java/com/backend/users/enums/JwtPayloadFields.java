package com.backend.users.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JwtPayloadFields {
  EMAIL("email"),
  ID("id");

  private final String name;
}
