package com.twinzpay.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BillPaymentRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        String email,
        @NotBlank(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        //Alternatively use @Digits(integer = 10, fraction = 2) to enforce format
        BigDecimal amount,
        @Positive(message = "Bill Plan ID must be a positive number")
        Long billPlanId,
        @Positive(message = "Target account must be positive digits")
        String targetAccount
        ) {
}
