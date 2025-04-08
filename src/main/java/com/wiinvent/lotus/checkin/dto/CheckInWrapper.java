package com.wiinvent.lotus.checkin.dto;

import com.wiinvent.lotus.checkin.entity.CheckInHistoryEntity;
import com.wiinvent.lotus.checkin.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CheckInWrapper {
    private UserEntity userEntity;
    private CheckInHistoryEntity checkInHistoryEntity;
}
