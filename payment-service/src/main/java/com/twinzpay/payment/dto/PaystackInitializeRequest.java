package com.twinzpay.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaystackInitializeRequest {
    private String email;
    private String amount; // Note: Paystack expects this as a String representation of Kobo (e.g., "50000" for 500 Naira)
    private String reference;
    private String callback_url;
}
