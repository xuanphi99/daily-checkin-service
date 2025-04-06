package com.wiinvent.lotus.checkin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiinvent.lotus.checkin.dto.CheckInHistoryDto;
import com.wiinvent.lotus.checkin.dto.UserDto;
import com.wiinvent.lotus.checkin.service.CheckInHistoryService;
import com.wiinvent.lotus.checkin.service.UserService;
import com.wiinvent.lotus.checkin.util.LocaleKey;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    private final ObjectMapper objectMapper;

    private final CheckInHistoryService checkInHistoryService;

    private final MessageSource messageSource;

    public UserController(UserService userService,
                          ObjectMapper objectMapper,
                          CheckInHistoryService checkInHistoryService,
                          MessageSource messageSource) {
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.checkInHistoryService = checkInHistoryService;
        this.messageSource = messageSource;
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestPart("user") String userJson,
                                              @RequestPart(value = "avatar" ,required = false) MultipartFile avatar,
                                              @RequestHeader(value = "Accept-Language", defaultValue = "vi") String lang) throws Exception {
        UserDto userDto = objectMapper.readValue(userJson, UserDto.class);
        Locale locale = new Locale(lang);

        if (avatar != null && !avatar.isEmpty()) {
            String contentType = avatar.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new Exception(messageSource.getMessage(LocaleKey.FILE_AVATAR_INVALID,null,locale));
            }
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
            return ResponseEntity.ok(messageSource.getMessage(LocaleKey.CHECK_IN_SUCCESS,null,locale));
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
            return ResponseEntity.ok(messageSource.getMessage(LocaleKey.SUBTRACT_POINT_SUCCESS,null,locale));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
