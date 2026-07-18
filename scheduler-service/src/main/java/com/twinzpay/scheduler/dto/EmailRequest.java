package com.twinzpay.scheduler.dto;

public record EmailRequest(String to, String subject, String body) {
}
