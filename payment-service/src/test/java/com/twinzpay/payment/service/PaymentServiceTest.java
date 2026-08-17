package com.twinzpay.payment.service;


import com.twinzpay.payment.dto.BillPaymentRequest;
import com.twinzpay.payment.dto.PaystackInitializeRequest;
import com.twinzpay.payment.dto.PaystackInitializeResponse;
import com.twinzpay.payment.dto.PaystackVerifyResponse;
import com.twinzpay.payment.entity.Payment;
import com.twinzpay.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    // WebClient fluent API mocks
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        // We have to mock the builder chain that happens inside the PaymentService constructor
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        // Instantiate the service with mocked dependencies
        paymentService = new PaymentService(
                paymentRepository,
                webClientBuilder,
                "https://api.paystack.co",
                "sk_test_mock_key"
        );
    }

    @Test
    void initializePayment_ShouldSavePendingPaymentAndCallPaystack() {
        // Arrange
        BillPaymentRequest request = new BillPaymentRequest(
                "testuser@gmail.com",
                new BigDecimal("5000.00"),
                1L,
                "08012345678"
        );

        PaystackInitializeResponse mockResponse = new PaystackInitializeResponse();
        // Assume you have setters or a builder to populate the mockResponse if needed.
        // For this test, we just need the WebClient to return it.

        // Mock the WebClient POST chain
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/transaction/initialize")).thenReturn(requestBodySpec);
        // Note: The service sends the original request, so we match on any Object
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(PaystackInitializeResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        PaystackInitializeResponse result = paymentService.initializePayment(request);

        // Assert
        assertNotNull(result);

        // Verify that a Payment was saved to the repository with PENDING status
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();
        assertEquals("testuser@gmail.com", savedPayment.getUserEmail());
        assertEquals(new BigDecimal("5000.00"), savedPayment.getAmount());
        assertEquals("PENDING", savedPayment.getStatus());
        assertEquals(1L, savedPayment.getBillPlanId());
        assertEquals("08012345678", savedPayment.getTargetAccount());
        assertNotNull(savedPayment.getReference()); // UUID should be generated
    }

    @Test
    void verifyPayment_ShouldUpdateStatusToSuccess_WhenPaystackConfirms() {
        // Arrange
        String testReference = "mock-uuid-1234";
        Payment pendingPayment = Payment.builder()
                .reference(testReference)
                .status("PENDING")
                .build();

        PaystackVerifyResponse mockResponse = new PaystackVerifyResponse();
        mockResponse.setStatus(true);
        PaystackVerifyResponse.VerifyData data = new PaystackVerifyResponse.VerifyData();
        data.setStatus("success");
        mockResponse.setData(data);

        // Mock repository retrieval
        when(paymentRepository.findByReference(testReference)).thenReturn(Optional.of(pendingPayment));
        // Mock repository save to just return the object passed to it
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock the WebClient GET chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/transaction/verify/" + testReference)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(PaystackVerifyResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        Payment verifiedPayment = paymentService.verifyPayment(testReference);

        // Assert
        assertEquals("SUCCESS", verifiedPayment.getStatus());
        verify(paymentRepository, times(1)).save(pendingPayment);
    }

    @Test
    void verifyPayment_ShouldUpdateStatusToFailed_WhenPaystackFails() {
        // Arrange
        String testReference = "mock-uuid-5678";
        Payment pendingPayment = Payment.builder()
                .reference(testReference)
                .status("PENDING")
                .build();

        PaystackVerifyResponse mockResponse = new PaystackVerifyResponse();
        mockResponse.setStatus(false); // Paystack says it failed

        when(paymentRepository.findByReference(testReference)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/transaction/verify/" + testReference)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(PaystackVerifyResponse.class)).thenReturn(Mono.just(mockResponse));

        // Act
        Payment verifiedPayment = paymentService.verifyPayment(testReference);

        // Assert
        assertEquals("FAILED", verifiedPayment.getStatus());
        verify(paymentRepository, times(1)).save(pendingPayment);
    }
}
