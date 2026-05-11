package com.backend.users.dtos;

import java.time.OffsetDateTime;

import com.backend.users.enums.FriendRequestStatus;

import lombok.Data;

@Data
public class PendingFriendRequestDto {
  private String id;
  private String requesterId;
  private String requesterFullName;
  private String requesterProfilePictureUrl;
  private FriendRequestStatus status;
  private OffsetDateTime createdAt;
}
