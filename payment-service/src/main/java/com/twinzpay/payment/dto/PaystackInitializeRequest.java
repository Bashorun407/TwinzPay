package com.twinzpay.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaystackInitializeRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    @NotBlank(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    //Alternatively use @Digits(integer = 10, fraction = 2) to enforce format
    private String amount; // Note: Paystack expects this as a String representation of Kobo (e.g., "50000" for 500 Naira)
    private String reference;
    private String callback_url;
}
