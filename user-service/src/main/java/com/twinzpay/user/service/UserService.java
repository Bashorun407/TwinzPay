package com.twinzpay.user.service;

import com.twinzpay.user.dto.AuthResponseDto;
import com.twinzpay.user.dto.LoginRequestDto;
import com.twinzpay.user.dto.RegisterRequestDto;
import com.twinzpay.user.dto.UserResponseDto;
import com.twinzpay.user.entity.User;
import com.twinzpay.user.repository.UserRepository;
import com.twinzpay.user.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public UserResponseDto registerUser(RegisterRequestDto request) {
        // 1. Check for duplicates
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email is already registered");
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new RuntimeException("Phone number is already registered");
        }

        // 2. Hash the password
        String hashedPassword = passwordEncoder.encode(request.password());

        // 3. Build and save the user
        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .password(hashedPassword)
                .build();

        User savedUser = userRepository.save(user);

        // 4. Return safe data (never return the password, even hashed!)
        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getPhoneNumber()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponseDto loginUser(LoginRequestDto request) {
        // 1. Find user by email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // 2. Verify the password
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password"); // Keep error message vague for security
        }

        // 3. generate token
        String token = jwtUtil.generateToken(request.email());

        // 4. Return user details
        UserResponseDto userResponseDto = new UserResponseDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber()
        );

        return new AuthResponseDto(token, userResponseDto);
    }
}
