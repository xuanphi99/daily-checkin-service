package com.wiinvent.lotus.checkin.mapper;

import com.wiinvent.lotus.checkin.dto.CheckInHistoryDto;
import com.wiinvent.lotus.checkin.entity.CheckInHistoryEntity;
import org.springframework.stereotype.Component;

@Component
public class CheckInHistoryMapper {
    public CheckInHistoryDto toDto(CheckInHistoryEntity entity){
        return CheckInHistoryDto.builder()
                .id(entity.getId())
                .amount(entity.getAmount())
                .checkInDate(entity.getCheckInDate())
                .reason(entity.getReason())
                .build();
    }
}
