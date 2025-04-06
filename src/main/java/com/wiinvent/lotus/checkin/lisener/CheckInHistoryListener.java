package com.wiinvent.lotus.checkin.lisener;

import com.wiinvent.lotus.checkin.config.RedissonInstanceHolder;
import com.wiinvent.lotus.checkin.entity.CheckInHistoryEntity;
import com.wiinvent.lotus.checkin.util.ReasonCheckInEnum;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import javax.persistence.PostPersist;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;

public class CheckInHistoryListener {

    @PostPersist
    public void afterAdd(CheckInHistoryEntity entity) {
        RedissonClient redissonClient = RedissonInstanceHolder.get(); // Lấy tại thời điểm cần

        long userId = entity.getUserId();
        LocalDate checkInDate = entity.getCheckInDate();

        String cacheKeyDateRange = String.format("checkInRange:%d:%s:%s:%s",
                userId, ReasonCheckInEnum.check_in.name(),
                checkInDate.withDayOfMonth(1),
                checkInDate.with(TemporalAdjusters.lastDayOfMonth()));
        RBucket<List<CheckInHistoryEntity>> bucket = redissonClient.getBucket(cacheKeyDateRange);
        if (bucket.isExists()) {
            List<CheckInHistoryEntity> checkInHistoryEntities = bucket.get();
            checkInHistoryEntities.add(entity);
            bucket.set(checkInHistoryEntities);
            bucket.expire( Date.from(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0,0))
                    .atZone(ZoneId.systemDefault()).toInstant()).toInstant());
        }

    }

}
