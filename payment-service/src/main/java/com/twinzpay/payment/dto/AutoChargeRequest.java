package com.twinzpay.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AutoChargeRequest(
//        @NotBlank(message = "Email is required")
//        @Email(message = "Please provide a valid email address")
        String userEmail,
//        @NotBlank(message = "Amount is required")
//        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        //Alternatively use @Digits(integer = 10, fraction = 2) to enforce format
        BigDecimal amount,
//        @Positive(message = "Bill Plan ID must be a positive number")
        Long billPlanId
) {
}
