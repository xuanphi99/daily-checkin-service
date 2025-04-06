package com.wiinvent.lotus.checkin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginationRequest {
    private int page;
    private int size;

    public String toCacheValue() {
        return String.format("page=%d&size=%d", this.page, this.size);
    }

    public static String buildCacheKey(long userId) {
        return "userId:" + userId + ":pagination";
    }
}
