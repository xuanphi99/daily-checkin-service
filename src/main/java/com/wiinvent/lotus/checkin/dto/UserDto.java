package com.wiinvent.lotus.checkin.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private long id;
    private String name;
    private byte[] avatar ;

    @JsonProperty(value = "lotusPoints", access = JsonProperty.Access.READ_ONLY)
    private double lotusPoints;

    private List<CheckInHistoryDto> historyDto;
}
