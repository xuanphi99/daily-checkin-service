package com.wiinvent.lotus.checkin.lisener;

import com.wiinvent.lotus.checkin.config.RedissonInstanceHolder;
import com.wiinvent.lotus.checkin.entity.CheckInHistoryEntity;
import com.wiinvent.lotus.checkin.util.ReasonCheckInEnum;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import javax.persistence.PostPersist;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class CheckInHistoryListener {

    @PostPersist
    public void afterAdd(CheckInHistoryEntity entity) {
        RedissonClient redissonClient = RedissonInstanceHolder.get();

        long userId = entity.getUserId();
        LocalDate checkInDate = entity.getCheckInDate();

        String cacheKeyDateRange = String.format("checkInRange:%d:%s:%s:%s",
                userId, ReasonCheckInEnum.check_in.name(),
                checkInDate.withDayOfMonth(1),
                checkInDate.with(TemporalAdjusters.lastDayOfMonth()));

        RBucket<List<CheckInHistoryEntity>> bucket = redissonClient.getBucket(cacheKeyDateRange);

        List<CheckInHistoryEntity> checkInHistoryEntities = Optional.ofNullable(bucket.get()).orElse(new ArrayList<>());

        checkInHistoryEntities.add(entity);

        bucket.set(checkInHistoryEntities);

        bucket.expire(getEndOfDayExpiry());
    }

    private Instant getEndOfDayExpiry() {
        return Date.from(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 0))
                .atZone(ZoneId.systemDefault()).toInstant()).toInstant();
    }

}
