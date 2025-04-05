package com.wiinvent.lotus.checkin.repository;

import com.wiinvent.lotus.checkin.entity.RewardConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RewardConfigRepository extends JpaRepository<RewardConfigEntity, Long> {
    Optional<RewardConfigEntity> findByDayNumber(int dayNumber);
}
