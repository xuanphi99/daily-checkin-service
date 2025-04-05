package com.wiinvent.lotus.checkin.service;

import com.wiinvent.lotus.checkin.dto.UserDto;
import com.wiinvent.lotus.checkin.entity.UserEntity;
import com.wiinvent.lotus.checkin.mapper.UserMapper;
import com.wiinvent.lotus.checkin.repository.UserRepository;
import com.wiinvent.lotus.checkin.util.LocaleKey;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final MessageSource messageSource;
    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       MessageSource messageSource) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.messageSource = messageSource;
    }

    public UserDto createUser(UserDto userDto) {

        UserEntity user = userMapper.toEntity(userDto);
        UserEntity userEntity = userRepository.save(user);
        return userMapper.toDto(userEntity);
    }

    public ResponseEntity<?> getUserById(long userId, Locale locale) {
        Optional<UserEntity> userEntity = userRepository.findById(userId);
        if (userEntity.isPresent()) {
            return ResponseEntity.ok(userMapper.toDto(userEntity.get()));
        }
        String message = messageSource.getMessage(LocaleKey.USER_NOT_FOUND, null, locale);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
    }
}
