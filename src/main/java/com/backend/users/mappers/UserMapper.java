package com.backend.users.mappers;

import org.mapstruct.Mapper;

import com.backend.users.dtos.ProfileResponseDto;
import com.backend.users.entities.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {
  ProfileResponseDto toProfileResponseDto(UserEntity user);
}
