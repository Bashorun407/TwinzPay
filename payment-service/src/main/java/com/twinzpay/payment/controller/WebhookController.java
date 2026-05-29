package com.twinzpay.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinzpay.payment.repository.PaymentRepository;
import com.twinzpay.payment.service.PaystackWebhookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/payments/webhook")
public class WebhookController {
    private final PaystackWebhookService webhookService;
    private final ObjectMapper objectMapper;
    private final String secretKey;

    public WebhookController(
            PaystackWebhookService webhookService,
            ObjectMapper objectMapper,
            @Value("${paystack.secret-key}") String secretKey) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
        this.secretKey = secretKey;
    }

    @PostMapping
    public ResponseEntity<String> handlePaystackWebhook(
            @RequestBody String requestBody,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {

        try {
            // 1. Verify the request actually came from Paystack
            if (signature == null || !isValidSignature(requestBody, signature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            // 2. Parse the JSON body
            JsonNode payload = objectMapper.readTree(requestBody);

            // 3. Delegate the business logic to the Service
            webhookService.processWebhookEvent(payload);

            // 4. Return 200 OK immediately to Paystack
            return ResponseEntity.ok("Webhook Received");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook Error");
        }
    }

    // Standard cryptographic method to verify Paystack's HMAC SHA512 signature
    private boolean isValidSignature(String body, String signature) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        mac.init(secretKeySpec);

        byte[] bytes = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString().equals(signature);
    }
}
