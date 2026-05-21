package com.twinzpay.repository;

import com.twinzpay.entity.Biller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillerRepository extends JpaRepository<Biller, Long> {

    List<Biller> findByCategoryId(Long categoryId);
}
