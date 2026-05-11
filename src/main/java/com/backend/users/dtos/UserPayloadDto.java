package com.backend.users.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// When sending: Jackson adds "@class": "com.yourpackage.BlockPayloadDto" to the JSON
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPayloadDto {
  private String userId;
}
