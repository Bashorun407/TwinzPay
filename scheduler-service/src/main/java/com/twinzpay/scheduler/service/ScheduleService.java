package com.twinzpay.scheduler.service;

import com.twinzpay.scheduler.dto.CreateScheduleRequest;
import com.twinzpay.scheduler.entity.PaymentSchedule;
import com.twinzpay.scheduler.repository.PaymentScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ScheduleService {
    private final PaymentScheduleRepository repository;

    public ScheduleService(PaymentScheduleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PaymentSchedule createSchedule(CreateScheduleRequest request) {
        // Business Validation: A month only has up to 31 days
        if (request.dayOfMonth() < 1 || request.dayOfMonth() > 31) {
            throw new IllegalArgumentException("Invalid day of the month. Must be between 1 and 31.");
        }

        PaymentSchedule schedule = PaymentSchedule.builder()
                .userEmail(request.userEmail())
                .billPlanId(request.billPlanId())
                .targetAccount(request.targetAccount())
                .amount(request.amount())
                .dayOfMonth(request.dayOfMonth())
                .status("ACTIVE")
                .threeHourWarningSent(false)
                .thirtyMinWarningSent(false)
                .fifteenMinWarningSent(false)
                .build();

        return repository.save(schedule);
    }

    public List<PaymentSchedule> getActiveSchedulesForDay(int day) {
        return repository.findByDayOfMonthAndStatus(day, "ACTIVE");
    }
}
