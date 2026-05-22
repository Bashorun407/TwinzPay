package com.twinzpay.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "bill_plan_id")
    private Long billPlanId;

    @Column(name = "target_account")
    private String targetAccount; // The phone number, smart card, or meter number

    // This is the unique code Paystack uses to identify the transaction
    @Column(unique = true, nullable = false)
    private String reference;

    @Column(nullable = false)
    private String status; // We will use strings like: PENDING, SUCCESS, FAILED

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
