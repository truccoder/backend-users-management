package com.backend.users.mappers;

import java.util.List;

import org.mapstruct.Mapper;

import com.backend.core.web.page.Page;
import com.backend.users.dtos.PendingFriendRequestDto;
import com.backend.users.dtos.ProfileResponseDto;
import com.backend.users.dtos.SentFriendRequestDto;
import com.backend.users.entities.PendingFriendRequestProjection;
import com.backend.users.entities.SentFriendRequestProjection;
import com.backend.users.graph.UserNode;

@Mapper(componentModel = "spring")
public interface FriendMapper {
  PendingFriendRequestDto toPendingFriendRequestDto(PendingFriendRequestProjection projection);

  SentFriendRequestDto toSentFriendRequestDto(SentFriendRequestProjection projection);

  ProfileResponseDto toProfileResponseDto(UserNode userNode);

  default Page<ProfileResponseDto> toUserDtoPage(
      List<ProfileResponseDto> items, Long totalElements) {
    return Page.<ProfileResponseDto>builder().items(items).totalElements(totalElements).build();
  }
}
