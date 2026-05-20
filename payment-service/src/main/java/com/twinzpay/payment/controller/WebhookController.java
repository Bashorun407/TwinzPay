package com.twinzpay.payment.controller;

import com.twinzpay.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/payments/webhook")
public class WebhookController {
    private final PaymentRepository paymentRepository;

    @Value("${paystack.secret-key}")
    private String secretKey;

    public WebhookController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @PostMapping
    public ResponseEntity<String> handlePaystackWebhook(
            @RequestBody String requestBody,
            @RequestHeader("x-paystack-signature") String hmacSignature) {

        try {
            // 1. Cryptographically verify that the event originated from Paystack
            if (!isSignatureValid(requestBody, hmacSignature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            // 2. Simple text checking for the event payload to keep dependencies lightweight
            // An intermediate approach looks for specific sub-strings in the raw JSON payload
            if (requestBody.contains("\"event\":\"charge.success\"")) {
                // Extract the reference code string out of the JSON string text
                String reference = extractFieldFromJson(requestBody, "reference");

                paymentRepository.findByReference(reference).ifPresent(payment -> {
                    payment.setStatus("SUCCESS");
                    paymentRepository.save(payment);
                    System.out.println("Webhook verified & processed payment ref: " + reference);
                });
            }

            // Paystack expects a rapid 200 OK acknowledgment to prevent multiple retries
            return ResponseEntity.ok("Event Processed");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook Error");
        }
    }

    private boolean isSignatureValid(String rawBody, String headerSignature) throws Exception {
        Mac sha512Hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        sha512Hmac.init(secretKeySpec);

        byte[] macData = sha512Hmac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));

        // Convert the byte array into a readable Hex String representation
        StringBuilder hexString = new StringBuilder();
        for (byte b : macData) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        // Constant-time comparison string matching to protect against timing attacks
        return MessageDigest.isEqual(hexString.toString().getBytes(), headerSignature.getBytes());
    }

    private String extractFieldFromJson(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern) + pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
