package com.wiinvent.lotus.checkin.util;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
public class TimeRange {
    private LocalTime start;
    private LocalTime end;
}
