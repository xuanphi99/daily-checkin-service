package com.wiinvent.lotus.checkin.repository;

import com.wiinvent.lotus.checkin.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

}

