package com.wiinvent.lotus.checkin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiinvent.lotus.checkin.dto.CheckInHistoryDto;
import com.wiinvent.lotus.checkin.dto.UserDto;
import com.wiinvent.lotus.checkin.service.CheckInHistoryService;
import com.wiinvent.lotus.checkin.service.UserService;
import com.wiinvent.lotus.checkin.util.LocaleKey;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    private final ObjectMapper objectMapper;

    private final CheckInHistoryService checkInHistoryService;

    public UserController(UserService userService, ObjectMapper objectMapper, CheckInHistoryService checkInHistoryService) {
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.checkInHistoryService = checkInHistoryService;
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestPart("user") String userJson,
                                              @RequestPart("avatar") MultipartFile avatar) throws IOException {
        UserDto userDto = objectMapper.readValue(userJson, UserDto.class);

        if (avatar != null && !avatar.isEmpty()) {
            byte[] avatarBytes = avatar.getBytes();
            userDto.setAvatar(avatarBytes);
        }
        UserDto savedUser = userService.createUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@RequestHeader(value = "Accept-Language", defaultValue = "vi") String lang,
                                         @PathVariable long userId) {
        Locale locale = new Locale(lang);
        return userService.getUserById(userId, locale);
    }

    @PostMapping("/check-in/{userId}")
    public ResponseEntity<String> checkIn(@RequestHeader(value = "Accept-Language", defaultValue = "vi") String lang,
                                          @PathVariable long userId) {
        try {
            Locale locale = new Locale(lang);

            userService.checkInByUserId(userId, locale);
            return ResponseEntity.ok(LocaleKey.CHECK_IN_SUCCESS);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/check-in-status/{userId}")
    public List<CheckInHistoryDto> getCheckInStatus(@PathVariable long userId,
                                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return userService.getCheckInStatusById(userId, startDate, endDate);
    }

    @GetMapping("/{userId}/history")
    public Page<CheckInHistoryDto> getCheckInHistory(
            @PathVariable long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return checkInHistoryService.getCheckInHistory(userId, page, size);
    }

    @PostMapping("/points/deduct/{userId}")
    public ResponseEntity<String> deduct(@RequestHeader(value = "Accept-Language", defaultValue = "vi") String lang,
                                          @PathVariable long userId,
                                          @RequestBody CheckInHistoryDto checkInHistoryDto) {
        try {
            Locale locale = new Locale(lang);
            userService.subtractPoints(userId, checkInHistoryDto,locale);
            return ResponseEntity.ok(LocaleKey.SUBTRACT_POINT_SUCCESS);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
