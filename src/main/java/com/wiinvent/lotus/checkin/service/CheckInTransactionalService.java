package com.wiinvent.lotus.checkin.service;

import com.wiinvent.lotus.checkin.dto.CheckInWrapper;
import com.wiinvent.lotus.checkin.entity.CheckInHistoryEntity;
import com.wiinvent.lotus.checkin.entity.UserEntity;
import com.wiinvent.lotus.checkin.repository.CheckInHistoryRepository;
import com.wiinvent.lotus.checkin.repository.UserRepository;
import com.wiinvent.lotus.checkin.util.ReasonCheckInEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@Service
public class CheckInTransactionalService {
    private final UserRepository userRepository;
    private final CheckInHistoryRepository checkInHistoryRepository;

    public CheckInTransactionalService(UserRepository userRepository,
                                       CheckInHistoryRepository checkInHistoryRepository) {
        this.userRepository = userRepository;
        this.checkInHistoryRepository = checkInHistoryRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CheckInWrapper upgradeUserCheckIn(long userId, UserEntity userEntity,
                                   HashMap<Integer, Integer> rewardConfigs,
                                   List<CheckInHistoryEntity> turnInMonth,
                                   LocalDate dateCheckIn) {
        userEntity.setLotusPoints(userEntity.getLotusPoints() +
                rewardConfigs.getOrDefault(turnInMonth.size() + 1, 0));
        UserEntity user =  userRepository.save(userEntity);

        CheckInHistoryEntity checkInHistoryEntity = new CheckInHistoryEntity();
        checkInHistoryEntity.setUserId(userId);
        checkInHistoryEntity.setAmount(rewardConfigs.getOrDefault(turnInMonth.size() + 1, 0));
        checkInHistoryEntity.setCheckInDate(dateCheckIn);
        checkInHistoryEntity.setReason(ReasonCheckInEnum.check_in.name());
        checkInHistoryRepository.save(checkInHistoryEntity);

        return CheckInWrapper.builder()
                .checkInHistoryEntity(checkInHistoryEntity)
                .userEntity(user)
                .build();
    }
}
