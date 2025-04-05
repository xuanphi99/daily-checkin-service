package com.wiinvent.lotus.checkin.mapper;

import com.wiinvent.lotus.checkin.dto.CheckInHistoryDto;
import com.wiinvent.lotus.checkin.entity.CheckInHistoryEntity;

public class CheckInHistoryMapper {
    public CheckInHistoryDto toDto(CheckInHistoryEntity entity){
        return CheckInHistoryDto.builder()
                .amount(entity.getAmount())
                .checkInDate(entity.getCheckInDate())
                .reason(entity.getReason())
                .build();
    }
}
