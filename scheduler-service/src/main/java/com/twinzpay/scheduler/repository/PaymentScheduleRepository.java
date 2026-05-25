package com.twinzpay.scheduler.repository;

import com.twinzpay.scheduler.entity.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {
    // Finds all active schedules meant to run on a specific day (e.g., the 26th)
    List<PaymentSchedule> findByDayOfMonthAndStatus(int dayOfMonth, String status);
}
