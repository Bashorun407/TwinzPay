package com.twinzpay.payment.repository;

import com.twinzpay.payment.entity.SavedCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedCardRepository extends JpaRepository<SavedCard, Long> {
    Optional<SavedCard> findByUserEmail(String userEmail);
}
