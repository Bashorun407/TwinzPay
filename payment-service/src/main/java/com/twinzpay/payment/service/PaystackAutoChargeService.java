package com.twinzpay.payment.service;

import com.twinzpay.payment.dto.AutoChargeRequest;
import com.twinzpay.payment.entity.SavedCard;
import com.twinzpay.payment.repository.SavedCardRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaystackAutoChargeService {

    private final SavedCardRepository savedCardRepository;
    private final RestClient restClient;
    private final String secretKey;

    public PaystackAutoChargeService(
            SavedCardRepository savedCardRepository,
            RestClient restClient,
            @Value("${paystack.secret-key}") String secretKey) {
        this.savedCardRepository = savedCardRepository;
        this.restClient = restClient;
        this.secretKey = secretKey;
    }

    public boolean executeAutoCharge(AutoChargeRequest request) {
        // 1. Look up the vaulted card token
        SavedCard card = savedCardRepository.findByUserEmail(request.userEmail())
                .orElseThrow(() -> new RuntimeException("No saved card found for user: " + request.userEmail()));

        // 2. Prepare the payload for Paystack
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", request.userEmail());
        payload.put("amount", request.amount().multiply(BigDecimal.valueOf(100)).longValue()); // Convert NGN to Kobo
        payload.put("authorization_code", card.getAuthorizationCode());

        // 3. Fire the server-to-server request using fluent RestClient
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("https://api.paystack.co/transaction/charge_authorization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + secretKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity(); // Highly efficient: drops the response body, keeping only the HTTP status

            // If Paystack returns 200 OK, the card was successfully charged
            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            System.err.println("Auto-Charge failed for " + request.userEmail() + ": " + e.getMessage());
            return false;
        }
    }
}
