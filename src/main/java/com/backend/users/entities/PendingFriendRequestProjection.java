package com.backend.users.entities;

import java.time.OffsetDateTime;

import com.backend.users.enums.FriendRequestStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingFriendRequestProjection {
  private String id;
  private String requesterId;
  private String requesterFullName;
  private String requesterProfilePictureUrl;
  private FriendRequestStatus status;
  private OffsetDateTime createdAt;
}
