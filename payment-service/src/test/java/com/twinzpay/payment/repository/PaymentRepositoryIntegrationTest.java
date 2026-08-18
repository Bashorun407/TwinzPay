package com.twinzpay.payment.repository;

import com.twinzpay.payment.entity.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest // This annotation tells Spring to boot up a real database context (H2) for integration testing
public class PaymentRepositoryIntegrationTest {
    @Autowired
    private PaymentRepository paymentRepository; // We are using the REAL repository here!

    @Test
    @DisplayName("Integration Test: Save Payment to Database")
    void testSavePayment() {
        // 1. Arrange: Create a real Payment entity
        Payment payment = Payment.builder()
                .userEmail("test.user@university.edu.ng")
                .amount(new BigDecimal("5000.00"))
                .reference("paystack_ref_99999")
                .status("PENDING")
                .billPlanId(101L)
                .targetAccount("08012345678")
                .build();

        // 2. Act: Save it to the H2 database
        Payment savedPayment = paymentRepository.save(payment);

        // 3. Assert: Verify the database assigned an ID and saved the data correctly
        assertNotNull(savedPayment.getId(), "Database should auto-generate an ID");
        assertEquals("PENDING", savedPayment.getStatus());
        assertEquals("test.user@university.edu.ng", savedPayment.getUserEmail());
    }

    @Test
    @DisplayName("Integration Test: Find Payment by Reference")
    void testFindByReference() {
        // 1. Arrange: Save a payment to the database first
        Payment payment = Payment.builder()
                .userEmail("student@miva.edu.ng")
                .amount(new BigDecimal("1500.00"))
                .reference("paystack_ref_12345")
                .status("SUCCESS")
                .billPlanId(202L)
                .targetAccount("09087654321")
                .build();

        paymentRepository.save(payment);

        // 2. Act: Ask the repository to find it using your custom method
        Optional<Payment> foundPayment = paymentRepository.findByReference("paystack_ref_12345");

        // 3. Assert: Verify the database successfully retrieved the exact record
        assertTrue(foundPayment.isPresent(), "Payment should be found in the database");
        assertEquals("SUCCESS", foundPayment.get().getStatus());
        assertEquals(new BigDecimal("1500.00"), foundPayment.get().getAmount());
    }
}
