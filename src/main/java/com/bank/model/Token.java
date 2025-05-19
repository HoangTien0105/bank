package com.bank.model;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "token")
public class Token extends BaseEntity {

    @Column(name = "access_token", length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "access_token_expiry")
    private Date accessTokenExpiry;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "refresh_token_expiry")
    private Date refreshTokenExpiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
