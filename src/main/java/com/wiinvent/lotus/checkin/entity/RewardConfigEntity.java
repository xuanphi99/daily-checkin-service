package com.wiinvent.lotus.checkin.entity;

import lombok.Getter;

import javax.persistence.*;

@Entity
@Getter
@Table(name = "reward_config", indexes = {
        @Index(name = "idx_day_number", columnList = "day_number")
})
public class RewardConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "points", nullable = false)
    private int points;
}
