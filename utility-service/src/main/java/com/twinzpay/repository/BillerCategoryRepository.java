package com.twinzpay.repository;

import com.twinzpay.entity.BillerCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillerCategoryRepository extends JpaRepository<BillerCategory, Long> {
}
