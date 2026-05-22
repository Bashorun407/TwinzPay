package com.twinzpay.payment.dto;

import java.math.BigDecimal;

public record BillPaymentRequest(
        String email,
        BigDecimal amount,
        Long billPlanId,
        String targetAccount
        ) {
}
