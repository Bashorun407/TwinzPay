package com.twinzpay.payment.repository;

import com.twinzpay.payment.entity.Payment;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository {
    // Custom query to find a transaction by its unique reference
    Optional<Payment> findByReference(String reference);
}
