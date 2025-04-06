package com.wiinvent.lotus.checkin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInHistoryDto {
    private boolean isCheckedIn;
    private LocalDate checkInDate;
    private int amount;
    private String reason;
    private long id;
}
