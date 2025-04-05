package com.wiinvent.lotus.checkin.mapper;

import com.wiinvent.lotus.checkin.dto.UserDto;
import com.wiinvent.lotus.checkin.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserEntity toEntity(UserDto userDto) {
        UserEntity user = new UserEntity();
        user.setName(userDto.getName());

        return user;
    }

    public UserDto toDto(UserEntity userEntity) {
        UserDto userDto = new UserDto();
        userDto.setName(userEntity.getName());
        userDto.setId(userEntity.getId());
        return userDto;
    }
}
