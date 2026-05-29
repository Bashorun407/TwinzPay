package com.twinzpay.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.twinzpay.payment.entity.SavedCard;
import com.twinzpay.payment.repository.PaymentRepository;
import com.twinzpay.payment.repository.SavedCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaystackWebhookService {
    private final PaymentRepository paymentRepository;
    private final SavedCardRepository savedCardRepository;

    public PaystackWebhookService(PaymentRepository paymentRepository, SavedCardRepository savedCardRepository) {
        this.paymentRepository = paymentRepository;
        this.savedCardRepository = savedCardRepository;
    }

    @Transactional
    public void processWebhookEvent(JsonNode payload) {
        String event = payload.path("event").asText();

        if ("charge.success".equals(event)) {
            JsonNode data = payload.path("data");

            // 1. Update the original Payment Status
            String reference = data.path("reference").asText();
            paymentRepository.findByReference(reference).ifPresent(payment -> {
                payment.setStatus("SUCCESS");
                paymentRepository.save(payment);
            });

            // 2. Vault the Card for Future Auto-Debits
            JsonNode customer = data.path("customer");
            JsonNode authorization = data.path("authorization");

            // Verify the authorization object exists and the card is reusable
            if (!authorization.isMissingNode() && authorization.path("reusable").asBoolean()) {
                String email = customer.path("email").asText();
                saveOrUpdateCard(email, authorization);
            }
        }
    }

    private void saveOrUpdateCard(String email, JsonNode authorization) {
        SavedCard card = savedCardRepository.findByUserEmail(email)
                .orElse(new SavedCard());

        card.setUserEmail(email);
        card.setAuthorizationCode(authorization.path("authorization_code").asText());
        card.setCardType(authorization.path("card_type").asText());
        card.setLast4(authorization.path("last4").asText());
        card.setExpMonth(authorization.path("exp_month").asText());
        card.setExpYear(authorization.path("exp_year").asText());

        savedCardRepository.save(card);
        System.out.println("Securely vaulted card for: " + email);
    }
}
