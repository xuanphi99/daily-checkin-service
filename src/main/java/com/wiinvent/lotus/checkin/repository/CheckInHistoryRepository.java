package com.wiinvent.lotus.checkin.repository;

import com.wiinvent.lotus.checkin.entity.CheckInHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CheckInHistoryRepository extends JpaRepository<CheckInHistoryEntity, Long> {

    Optional<CheckInHistoryEntity> findByUserIdAndCheckInDate(long userId, LocalDate checkInDate);
}