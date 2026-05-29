package com.twinzpay.payment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "saved_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userEmail;

    @Column(nullable = false)
    private String authorizationCode; // e.g., AUTH_8dfx2ye

    private String cardType; // visa, mastercard
    private String last4;    // 1234
    private String expMonth;
    private String expYear;
}
