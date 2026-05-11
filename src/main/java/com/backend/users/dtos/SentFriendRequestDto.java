package com.backend.users.dtos;

import java.time.OffsetDateTime;

import com.backend.users.enums.FriendRequestStatus;

import lombok.Data;

@Data
public class SentFriendRequestDto {
  private String id;
  private String addresseeId;
  private String addresseeFullName;
  private String addresseeProfilePictureUrl;
  private FriendRequestStatus status;
  private OffsetDateTime createdAt;
}
