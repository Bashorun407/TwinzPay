package com.twinzpay.payment.controller;

import com.twinzpay.payment.dto.AutoChargeRequest;
import com.twinzpay.payment.service.PaystackAutoChargeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/payments")
public class InternalPaymentController {
    private final PaystackAutoChargeService autoChargeService;

    public InternalPaymentController(PaystackAutoChargeService autoChargeService) {
        this.autoChargeService = autoChargeService;
    }

    @PostMapping("/auto-charge")
    public ResponseEntity<String> processAutoCharge(@RequestBody AutoChargeRequest request) {
        boolean success = autoChargeService.executeAutoCharge(request);
        if (success) {
            return ResponseEntity.ok("Charge Successful");
        }
        return ResponseEntity.badRequest().body("Charge Failed");
    }
}
