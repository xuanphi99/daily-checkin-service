package com.wiinvent.lotus.checkin.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "check_in_history")
@Getter
@Setter
public class CheckInHistoryEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "user_id", columnDefinition = "BIGINT COMMENT 'User ID (foreign key handled at service layer)'")
    private long userId;

    @Column(name = "check_in_date" , columnDefinition = "DATE COMMENT 'Check-in date (yyyy-MM-dd)'")
    private LocalDate checkInDate;

    @Column(name = "amount" , columnDefinition = "INT COMMENT 'amount added or deducted'")
    private int amount;

    @Column(name = "reason" , columnDefinition = "VARCHAR(255) COMMENT 'Reason for point change'")
    private String reason;

}
