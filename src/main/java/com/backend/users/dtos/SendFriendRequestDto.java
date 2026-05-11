package com.backend.users.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendFriendRequestDto {
  @NotNull private String addresseeId;
}
