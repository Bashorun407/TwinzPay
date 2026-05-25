package com.twinzpay.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Long billPlanId;

    @Column(nullable = false)
    private String targetAccount; // e.g., Meter Number

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private int dayOfMonth; // e.g., 26

    @Column(nullable = false)
    private String status; // ACTIVE, PAUSED, CANCELLED

    // State machine trackers for the current execution cycle
    private LocalDateTime lastExecutedMonth; // Tracks if paid for May 2026, June 2026, etc.

    private boolean threeHourWarningSent;
    private boolean thirtyMinWarningSent;
    private boolean fifteenMinWarningSent;
}
