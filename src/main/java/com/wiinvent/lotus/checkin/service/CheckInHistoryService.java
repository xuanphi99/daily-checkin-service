package com.wiinvent.lotus.checkin.service;

import com.wiinvent.lotus.checkin.dto.CheckInHistoryDto;
import com.wiinvent.lotus.checkin.entity.CheckInHistoryEntity;
import com.wiinvent.lotus.checkin.mapper.CheckInHistoryMapper;
import com.wiinvent.lotus.checkin.repository.CheckInHistoryRepository;
import com.wiinvent.lotus.checkin.util.ReasonCheckInEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CheckInHistoryService {
    private final CheckInHistoryRepository checkInHistoryRepository;
    private final CheckInHistoryMapper mapper;

    public CheckInHistoryService(CheckInHistoryRepository checkInHistoryRepository,
                                 CheckInHistoryMapper mapper) {
        this.checkInHistoryRepository = checkInHistoryRepository;
        this.mapper = mapper;
    }

    public Page<CheckInHistoryDto> getCheckInHistory(long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1,size));

        Page<CheckInHistoryEntity> entityPage = checkInHistoryRepository
                .findByUserIdAndReason(userId, ReasonCheckInEnum.check_in.name(), pageable);

        List<CheckInHistoryDto> dtoList = entityPage.stream().map(mapper::toDto)
                .collect(Collectors.toList());
            return new PageImpl<>(dtoList, pageable, entityPage.getTotalElements());
    }
}
