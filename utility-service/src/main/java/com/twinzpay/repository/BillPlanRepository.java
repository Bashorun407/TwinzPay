package com.twinzpay.repository;

import com.twinzpay.entity.BillPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillPlanRepository extends JpaRepository<BillPlan, Long> {

    List<BillPlan> findByBillerId(Long BillerId);
}
