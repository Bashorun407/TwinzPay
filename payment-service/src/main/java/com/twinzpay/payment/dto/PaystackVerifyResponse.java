package com.twinzpay.payment.dto;

import lombok.Data;

@Data
public class PaystackVerifyResponse {
    private boolean status; // Status of the API call itself
    private String message;
    private VerifyData data;

    @Data
    public static class VerifyData {
        private Long id;
        private String status; // The actual status of the transaction: "success" or "failed"
        private String reference;
        private Long amount; // Amount returned in Kobo
        private String gateway_response;
    }
}
