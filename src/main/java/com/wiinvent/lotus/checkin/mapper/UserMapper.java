package com.wiinvent.lotus.checkin.mapper;

import com.wiinvent.lotus.checkin.dto.UserDto;
import com.wiinvent.lotus.checkin.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserEntity toEntity(UserDto userDto) {
        return UserEntity.builder()
                .name(userDto.getName())
                .avatar(userDto.getAvatar())
                .lotusPoints(userDto.getLotusPoints())
                .build();
    }

    public UserDto toDto(UserEntity userEntity) {
        return UserDto.builder()
                .id(userEntity.getId())
                .name(userEntity.getName())
                .avatar(userEntity.getAvatar())
                .lotusPoints(userEntity.getLotusPoints())
                .build();
    }
}
