package com.backend.users.entities;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("t_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity implements UserDetails, Persistable<String> {
  @Id private String id;
  private String email;
  private String password;
  private String fullName;
  private String profilePictureUrl;

  @CreatedDate private OffsetDateTime createdAt;

  @LastModifiedDate private OffsetDateTime updatedAt;

  @Transient private boolean isNew = true;

  @Override
  public boolean isNew() {
    return Objects.isNull(createdAt);
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
