package com.backend.users.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
public class UnblockPayloadDto extends UserPayloadDto {
  private String blockedId;

  public UnblockPayloadDto(String userId, String blockedId) {
    super(userId);
    this.blockedId = blockedId;
  }
}
