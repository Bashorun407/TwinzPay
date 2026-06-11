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
        if (request.targetHour() < 0 || request.targetHour() > 23) {
            throw new IllegalArgumentException("Invalid hour. Must be between 0 and 23.");
        }
        if (request.targetMinute() < 0 || request.targetMinute() > 59) {
            throw new IllegalArgumentException("Invalid minute. Must be between 0 and 59.");
        }

        PaymentSchedule schedule = PaymentSchedule.builder()
                .userEmail(request.userEmail())
                .billPlanId(request.billPlanId())
                .targetAccount(request.targetAccount())
                .amount(request.amount())
                .dayOfMonth(request.dayOfMonth())
                .targetHour(request.targetHour())
                .targetMinute(request.targetMinute())
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

    public PaymentSchedule reactivateSchedule(Long scheduleId, String userEmail) {
        // 1. Find the schedule by ID
        PaymentSchedule schedule = repository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        // 2. Security Check: Ensure the user actually owns this schedule
        if (!schedule.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized: You cannot modify this schedule");
        }

        // 3. Prevent unnecessary database calls if it is already active
        if ("ACTIVE".equals(schedule.getStatus())) {
            return schedule;
        }

        // 4. Flip the status back to ACTIVE
        schedule.setStatus("ACTIVE");

        // 5. Save and return the updated schedule
        return repository.save(schedule);
    }
}
