package com.twinzpay.payment.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.twinzpay.payment.entity.Payment;
import com.twinzpay.payment.entity.SavedCard;
import com.twinzpay.payment.repository.PaymentRepository;
import com.twinzpay.payment.repository.SavedCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaystackWebhookServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SavedCardRepository savedCardRepository;

    @InjectMocks
    private PaystackWebhookService paystackWebhookService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should successfully process charge.success event and vault card")
    void testProcessWebhookEvent_ChargeSuccess_ReusableCard() {
        // 1. Arrange: Build a simulated Paystack webhook JSON payload
        String reference = "trx_123456";
        String email = "student@university.edu.ng";

        ObjectNode authorization = objectMapper.createObjectNode();
        authorization.put("reusable", true);
        authorization.put("authorization_code", "AUTH_99999");
        authorization.put("card_type", "visa");
        authorization.put("last4", "4081");
        authorization.put("exp_month", "12");
        authorization.put("exp_year", "2030");

        ObjectNode customer = objectMapper.createObjectNode();
        customer.put("email", email);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("reference", reference);
        data.set("customer", customer);
        data.set("authorization", authorization);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("event", "charge.success");
        payload.set("data", data);

        // Mock database responses
        Payment mockPayment = new Payment();
        mockPayment.setReference(reference);
        mockPayment.setStatus("PENDING");

        when(paymentRepository.findByReference(reference)).thenReturn(Optional.of(mockPayment));
        when(savedCardRepository.findByUserEmail(email)).thenReturn(Optional.empty()); // Simulate first time saving card

        // 2. Act: Process the webhook
        paystackWebhookService.processWebhookEvent(payload);

        // 3. Assert: Verify the payment status was updated to SUCCESS
        assertEquals("SUCCESS", mockPayment.getStatus());
        verify(paymentRepository, times(1)).save(mockPayment);

        // 4. Assert: Capture and verify the vaulted card details
        ArgumentCaptor<SavedCard> cardCaptor = ArgumentCaptor.forClass(SavedCard.class);
        verify(savedCardRepository, times(1)).save(cardCaptor.capture());

        SavedCard capturedCard = cardCaptor.getValue();
        assertEquals(email, capturedCard.getUserEmail());
        assertEquals("AUTH_99999", capturedCard.getAuthorizationCode());
        assertEquals("visa", capturedCard.getCardType());
        assertEquals("4081", capturedCard.getLast4());
        assertEquals("12", capturedCard.getExpMonth());
        assertEquals("2030", capturedCard.getExpYear());
    }

    @Test
    @DisplayName("Should process charge.success but NOT vault card if reusable is false")
    void testProcessWebhookEvent_ChargeSuccess_NotReusable() {
        // 1. Arrange: Build payload with reusable = false
        ObjectNode authorization = objectMapper.createObjectNode();
        authorization.put("reusable", false); // Card cannot be vaulted!

        ObjectNode data = objectMapper.createObjectNode();
        data.put("reference", "trx_77777");
        data.set("authorization", authorization);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("event", "charge.success");
        payload.set("data", data);

        Payment mockPayment = new Payment();
        when(paymentRepository.findByReference("trx_77777")).thenReturn(Optional.of(mockPayment));

        // 2. Act
        paystackWebhookService.processWebhookEvent(payload);

        // 3. Assert
        verify(paymentRepository, times(1)).save(any(Payment.class)); // Payment is still marked success
        verify(savedCardRepository, never()).save(any()); // But card is NEVER saved
    }

    @Test
    @DisplayName("Should ignore webhook if event is not charge.success")
    void testProcessWebhookEvent_IgnoredEvent() {
        // 1. Arrange: Build payload for a failed charge or completely different event
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("event", "charge.failed");

        // 2. Act
        paystackWebhookService.processWebhookEvent(payload);

        // 3. Assert: Ensure no database operations occurred
        verify(paymentRepository, never()).findByReference(anyString());
        verify(paymentRepository, never()).save(any());
        verify(savedCardRepository, never()).save(any());
    }
}
