package com.wiinvent.lotus.checkin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInStatus {
    private int day;
    private boolean isCheckedIn;

}
