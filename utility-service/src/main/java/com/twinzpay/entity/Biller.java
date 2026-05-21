package com.twinzpay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "billers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Biller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "MTN", "Airtel", "IKEDC", "DSTV"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private BillerCategory category;

    @OneToMany(mappedBy = "biller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BillPlan> plans;
}
