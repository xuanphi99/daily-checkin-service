package com.wiinvent.lotus.checkin.util;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class CheckInValidateHelper {
    private static final List<TimeRange> TIME_RANGES = Arrays.asList(
            new TimeRange(LocalTime.of(9, 0), LocalTime.of(11, 0)),
            new TimeRange(LocalTime.of(19, 0), LocalTime.of(21, 0))
    );

    public boolean isTimeInRange() {
        LocalTime now = LocalTime.now();
        return TIME_RANGES.stream()
                .anyMatch(timeRange -> !now.isBefore(timeRange.getStart()) && !now.isAfter(timeRange.getEnd()));
    }

    public Date getExpiryTime() {
        LocalTime now = LocalTime.now();

        return TIME_RANGES.stream()
                .filter(timeRange -> !now.isBefore(timeRange.getStart()) && !now.isAfter(timeRange.getEnd()))
                .map(TimeRange::getEnd)
                .findFirst()
                .map(endTime -> {
                    LocalDateTime localDateTime = LocalDateTime.of(LocalDate.now(), endTime);
                    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                })
                .orElse(Date.from(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0,0))
                        .atZone(ZoneId.systemDefault()).toInstant()));
    }
}
