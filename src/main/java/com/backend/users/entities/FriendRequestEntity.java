package com.backend.users.entities;

import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import com.backend.users.enums.FriendRequestStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("t_friend_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestEntity implements Persistable<String> {
  @Id private String id;
  private String requesterId;
  private String addresseeId;
  private FriendRequestStatus status;

  @CreatedDate private OffsetDateTime createdAt;

  @LastModifiedDate private OffsetDateTime updatedAt;

  @Transient private boolean isNew = true;

  @Override
  public boolean isNew() {
    return Objects.isNull(createdAt);
  }
}
