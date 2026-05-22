package com.twinzpay.user.dto;

public record RegisterRequestDto(String fullName,
                                 String email,
                                 String phoneNumber,
                                 String password) {
}
