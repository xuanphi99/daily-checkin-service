package com.wiinvent.lotus.checkin.repository;

import com.wiinvent.lotus.checkin.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

}

