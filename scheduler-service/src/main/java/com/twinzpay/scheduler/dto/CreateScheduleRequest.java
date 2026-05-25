package com.twinzpay.scheduler.dto;

import java.math.BigDecimal;

public record CreateScheduleRequest(
        String userEmail,
        Long billPlanId,
        String targetAccount,
        BigDecimal amount,
        int dayOfMonth,
        int targetHour,
        int targetMinute
) {
}
