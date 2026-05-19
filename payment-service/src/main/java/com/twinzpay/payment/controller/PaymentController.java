package com.twinzpay.payment.controller;

import com.twinzpay.payment.dto.PaystackInitializeResponse;
import com.twinzpay.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
