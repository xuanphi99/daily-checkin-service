package com.wiinvent.lotus.checkin.entity;

import com.wiinvent.lotus.checkin.lisener.CheckInHistoryListener;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@EntityListeners(CheckInHistoryListener.class)
@Table(name = "check_in_history", indexes = {
        @Index(name = "idx_user_id_check_in_date", columnList = "user_id, check_in_date"),
        @Index(name = "idx_user_id_check_in_date_reason", columnList = "user_id, check_in_date,reason"),
})
@Getter
@Setter
public class CheckInHistoryEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "user_id", columnDefinition = "BIGINT COMMENT 'User ID (foreign key handled at service layer)'")
    private long userId;

    @Column(name = "check_in_date" , columnDefinition = "DATE COMMENT 'Check-in date '")
    private LocalDate checkInDate;

    @Column(name = "amount" , columnDefinition = "INT COMMENT 'amount added or deducted'")
    private int amount;

    @Column(name = "reason" , columnDefinition = "VARCHAR(255) COMMENT 'Reason for point change'")
    private String reason;

}
