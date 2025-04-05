package com.wiinvent.lotus.checkin.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "user")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "name", nullable = false, columnDefinition = "VARCHAR(255) COMMENT 'User name'")
    private String name;

    @Column(name = "lotus_points", nullable = false, columnDefinition = "DOUBLE COMMENT 'User lotus points'")
    private double lotusPoints;

    @Column(name = "avatar", columnDefinition = "BLOB COMMENT 'User avatar image in byte array'")
    private byte[] avatar;


}
