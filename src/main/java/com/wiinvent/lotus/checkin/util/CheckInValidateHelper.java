package com.wiinvent.lotus.checkin.util;

import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Service
public class CheckInValidateHelper {
    private static final List<TimeRange> TIME_RANGES = Arrays.asList(
            new TimeRange(LocalTime.of(9, 0), LocalTime.of(11, 0)),  // 9h-11h
            new TimeRange(LocalTime.of(19, 0), LocalTime.of(21, 0))  // 19h-21h
    );

    public boolean isTimeInRange() {
        LocalTime now = LocalTime.now();
        return TIME_RANGES.stream()
                .anyMatch(timeRange -> !now.isBefore(timeRange.getStart()) && !now.isAfter(timeRange.getEnd()));
    }
}
