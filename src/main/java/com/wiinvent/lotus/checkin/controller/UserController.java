package com.wiinvent.lotus.checkin.controller;

import com.wiinvent.lotus.checkin.dto.UserDto;
import com.wiinvent.lotus.checkin.mapper.UserMapper;
import com.wiinvent.lotus.checkin.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        UserDto savedUser = userService.createUser(userDto);
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping("/{userId}")
    public  ResponseEntity<?> getUserById(@RequestHeader(value = "Accept-Language", defaultValue = "vi") String lang,
                                   @PathVariable long userId) {
        Locale locale = new Locale(lang);
        return userService.getUserById(userId,locale);
    }
}
