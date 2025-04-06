package com.wiinvent.lotus.checkin.service;

import com.wiinvent.lotus.checkin.dto.CheckInHistoryDto;
import com.wiinvent.lotus.checkin.dto.PaginationRequest;
import com.wiinvent.lotus.checkin.entity.CheckInHistoryEntity;
import com.wiinvent.lotus.checkin.mapper.CheckInHistoryMapper;
import com.wiinvent.lotus.checkin.repository.CheckInHistoryRepository;
import com.wiinvent.lotus.checkin.util.ReasonCheckInEnum;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CheckInHistoryService {
    private final CheckInHistoryRepository checkInHistoryRepository;
    private final CheckInHistoryMapper mapper;
    private final RedissonClient redissonClient;

    public CheckInHistoryService(CheckInHistoryRepository checkInHistoryRepository,
                                 CheckInHistoryMapper mapper, RedissonClient redissonClient) {
        this.checkInHistoryRepository = checkInHistoryRepository;
        this.mapper = mapper;
        this.redissonClient = redissonClient;
    }

    public Page<CheckInHistoryDto> getCheckInHistory(long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1,size));
        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page)
                .size(size)
                .build();

        RBucket<String> keyPage = redissonClient.getBucket(PaginationRequest.buildCacheKey(userId));
        keyPage.set(paginationRequest.toCacheValue());

        String dataCacheKey = PaginationRequest.buildCacheKey(userId) + (paginationRequest.toCacheValue());
        RBucket<Page<CheckInHistoryDto>> pages = redissonClient.getBucket(dataCacheKey);

        if(pages.isExists()){ //upgrade cache in CheckInHistoryListener
            return pages.get();
        }

        Page<CheckInHistoryEntity> entityPage = checkInHistoryRepository
                .findByUserIdAndReason(userId, ReasonCheckInEnum.check_in.name(), pageable);

        List<CheckInHistoryDto> dtoList = entityPage.stream().map(mapper::toDto)
                .collect(Collectors.toList());
        Page<CheckInHistoryDto>  dtoPage = new PageImpl<>(dtoList, pageable, entityPage.getTotalElements());
        pages.set(dtoPage);
        pages.expire(Duration.ofMinutes(15));
        return dtoPage;
    }
}
