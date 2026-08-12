package com.twinzpay.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequestDto(
        @NotBlank(message = "First name and last name is required")
        String fullName,
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        String email,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9]{11}$", message = "Phone number must be exactly 11 digits and contain no alphabets")
        //To accept international numbers with a '+' sign, you would change the regexp to: ^\\+?[0-9]{10,14}$
        String phoneNumber,
        @NotBlank(message = "Password is required")
        String password) {
}
