package com.twinzpay.scheduler.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateScheduleRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        String userEmail,
        @Positive(message = "Bill Plan ID must be a positive number")
        Long billPlanId,
        @Positive(message = "Target account must be positive digits")
        String targetAccount,
        @NotBlank(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        //Alternatively use @Digits(integer = 10, fraction = 2) to enforce format
        BigDecimal amount,
        @NotNull(message = "Day of the month is required")
        @Min(value = 1, message = "Day of the month must be at least 1")
        @Max(value = 31, message = "Day of the month cannot exceed 31")
        int dayOfMonth,
        @NotNull(message = "Target hour is required")
        @Min(value = 0, message = "Target hour cannot be less than 0 (Midnight)")
        @Max(value = 23, message = "Target hour cannot exceed 23 (11PM)")
        int targetHour,
        @NotNull(message = "Target minute is required")
        @Min(value = 1, message = "Target minute cannot be less than 0")
        @Max(value = 31, message = "Target minute cannot exceed 59")
        int targetMinute
) {
}
