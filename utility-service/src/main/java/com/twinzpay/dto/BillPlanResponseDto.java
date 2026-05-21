package com.twinzpay.dto;

import java.math.BigDecimal;

public record BillPlanResponseDto(Long id, String planName, BigDecimal price) {
}
