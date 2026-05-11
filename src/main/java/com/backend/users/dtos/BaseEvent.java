package com.backend.users.dtos;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent {
  private String timestamp;
  private String environment;
  private UserPayloadDto payload;

  public BaseEvent(String environment, UserPayloadDto payload) {
    this.timestamp = String.valueOf(OffsetDateTime.now().toInstant().toEpochMilli());
    this.environment = environment;
    this.payload = payload;
  }
}
