package com.backend.users.entities;

import java.time.OffsetDateTime;

import com.backend.users.enums.FriendRequestStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SentFriendRequestProjection {
  private String id;
  private String addresseeId;
  private String addresseeFullName;
  private String addresseeProfilePictureUrl;
  private FriendRequestStatus status;
  private OffsetDateTime createdAt;
}
