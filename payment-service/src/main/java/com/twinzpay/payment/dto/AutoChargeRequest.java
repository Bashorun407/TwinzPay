package com.twinzpay.payment.dto;

import java.math.BigDecimal;

public record AutoChargeRequest(
        String userEmail,
        BigDecimal amount,
        Long billPlanId
) {
}
