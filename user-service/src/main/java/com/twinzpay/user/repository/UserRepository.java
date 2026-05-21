package com.twinzpay.user.repository;

import com.twinzpay.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used for login
    Optional<User> findByEmail(String email);

    // Used for registration validation
    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
