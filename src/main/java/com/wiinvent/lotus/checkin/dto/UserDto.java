package com.wiinvent.lotus.checkin.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    public UserDto(String name) {
        this.name = name;
    }

    private long id;
    private String name;

    @JsonProperty("avatar")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private byte[] avatar ;

    @JsonProperty(value = "lotusPoints", access = JsonProperty.Access.READ_ONLY)
    private double lotusPoints;

    private List<CheckInHistoryDto> historyDto;
}
