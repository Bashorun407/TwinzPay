package com.twinzpay.payment.controller;

import com.twinzpay.payment.dto.PaystackInitializeResponse;
import com.twinzpay.payment.entity.Payment;
import com.twinzpay.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

        private final PaymentService paymentService;

        // Injecting our service layer via constructor
        public PaymentController(PaymentService paymentService) {
            this.paymentService = paymentService;
        }

        // Endpoint to initialize a transaction
        @PostMapping("/initialize")
        public ResponseEntity<PaystackInitializeResponse> initializePayment(
                @RequestParam String email,
                @RequestParam BigDecimal amount) {

            PaystackInitializeResponse response = paymentService.initializePayment(email, amount);
            return ResponseEntity.ok(response);
        }

    // Endpoint to verify a transaction
    @GetMapping("/verify")
    public ResponseEntity<Payment> verifyPayment(@RequestParam String reference) {
        Payment verifiedPayment = paymentService.verifyPayment(reference);
        return ResponseEntity.ok(verifiedPayment);
    }
}
