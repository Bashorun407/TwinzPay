package com.twinzpay.payment.service;

import com.twinzpay.payment.dto.BillPaymentRequest;
import com.twinzpay.payment.dto.PaystackInitializeRequest;
import com.twinzpay.payment.dto.PaystackInitializeResponse;
import com.twinzpay.payment.dto.PaystackVerifyResponse;
import com.twinzpay.payment.entity.Payment;
import com.twinzpay.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final WebClient webClient;

    // Constructor Injection is an intermediate-level best practice
    public PaymentService(
            PaymentRepository paymentRepository,
            WebClient.Builder webClientBuilder,
            @Value("${paystack.base-url}") String baseUrl,
            @Value("${paystack.secret-key}") String secretKey) {

        this.paymentRepository = paymentRepository;

        // Build the WebClient once with default headers to keep code clean
        this.webClient = webClientBuilder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    //1. Initialize payment method
    public PaystackInitializeResponse initializePayment(BillPaymentRequest request) {
        // 1. Generate a unique reference for this specific transaction
        String reference = UUID.randomUUID().toString();

        // 2. Save payment to our local database as PENDING
        Payment payment = Payment.builder()
                .userEmail(request.email())
                .amount(request.amount())
                .reference(reference)
                .status("PENDING")
                .billPlanId(request.billPlanId())
                .targetAccount(request.targetAccount())
                .build();
        paymentRepository.save(payment);

        // 3. Prepare the Paystack request
        // Note: Paystack expects the amount in Kobo (so we multiply the Naira amount by 100)
        // OPTIMIZED: Safe scaling and rounding to avoid ArithmeticExceptions
        String amountInKobo = request.amount().multiply(new BigDecimal("100"))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .toPlainString();

        PaystackInitializeRequest payStackRequest = PaystackInitializeRequest.builder()
                .email(request.email())
                .amount(amountInKobo)
                .reference(reference)
                .callback_url("http://localhost:8080/api/v1/payments/verify")
                .build();

        // 4. Call the Paystack API securely
        return webClient.post()
                .uri("/transaction/initialize")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaystackInitializeResponse.class)
                .block(); // .block() safely converts the asynchronous WebFlux call to a synchronous response
    }

    //2. Verify Payment method
    public Payment verifyPayment(String reference) {
        // 1. Call Paystack GET API to verify the transaction status
        PaystackVerifyResponse response = webClient.get()
                .uri("/transaction/verify/" + reference)
                .retrieve()
                .bodyToMono(PaystackVerifyResponse.class)
                .block();

        // 2. Find the payment in our local database
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Payment reference not found: " + reference));

        // 3. If Paystack confirms success, update our database record
        if (response != null && response.isStatus() && "success".equalsIgnoreCase(response.getData().getStatus())) {
            payment.setStatus("SUCCESS");
        } else {
            payment.setStatus("FAILED");
        }

        return paymentRepository.save(payment);
    }
}
