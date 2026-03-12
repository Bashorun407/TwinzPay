package com.twinzpay.payment.dto;

import lombok.Data;

@Data
public class PaystackInitializeResponse {
    private boolean status;
    private String message;
    private ResponseData data;

    @Data
    public static class ResponseData {
        private String authorization_url; // The link where the user enters their card details
        private String access_code;
        private String reference;
    }
}
