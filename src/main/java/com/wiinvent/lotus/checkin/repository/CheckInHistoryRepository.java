package com.wiinvent.lotus.checkin.repository;

import com.wiinvent.lotus.checkin.entity.CheckInHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface CheckInHistoryRepository extends JpaRepository<CheckInHistoryEntity, Long> {

    Optional<CheckInHistoryEntity> findByUserIdAndCheckInDate(long userId, LocalDate checkInDate);

    List<CheckInHistoryEntity> findByCheckInDateGreaterThanEqualAndCheckInDateLessThanEqual(LocalDate startDate, LocalDate endDate);

}