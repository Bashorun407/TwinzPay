package com.twinzpay.scheduler.engine;

import com.twinzpay.scheduler.entity.PaymentSchedule;
import com.twinzpay.scheduler.repository.PaymentScheduleRepository;
import com.twinzpay.scheduler.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentCronJob {
    private final PaymentScheduleRepository repository;
    private final EmailService emailService;

    public PaymentCronJob(PaymentScheduleRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    // Cron expression: runs at the 0th second of every minute
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void trackAndNotifySchedules() {
        LocalDateTime now = LocalDateTime.now();
        int today = now.getDayOfMonth();

        // 1. Fetch all ACTIVE schedules for today
        List<PaymentSchedule> todaysSchedules = repository.findByDayOfMonthAndStatus(today, "ACTIVE");

        for (PaymentSchedule schedule : todaysSchedules) {

            // 2. Construct the exact execution time for today
            LocalDateTime executionTime = now
                    .withHour(schedule.getTargetHour())
                    .withMinute(schedule.getTargetMinute())
                    .withSecond(0)
                    .withNano(0);

            // 3. Calculate how many minutes are left
            long minutesLeft = Duration.between(now, executionTime).toMinutes();

            // Ignore schedules that have already passed for today
            if (minutesLeft < 0) continue;

            // 4. State Machine logic for Notifications

            // T-3 Hours (180 minutes)
            if (minutesLeft <= 180 && !schedule.isThreeHourWarningSent()) {
                sendNotification(schedule, "3 Hours");
                schedule.setThreeHourWarningSent(true);
            }

            // T-30 Minutes
            else if (minutesLeft <= 30 && !schedule.isThirtyMinWarningSent()) {
                sendNotification(schedule, "30 Minutes");
                schedule.setThirtyMinWarningSent(true);
            }

            // T-15 Minutes
            else if (minutesLeft <= 15 && !schedule.isFifteenMinWarningSent()) {
                sendNotification(schedule, "15 Minutes");
                schedule.setFifteenMinWarningSent(true);
            }

            // Save the updated boolean flags back to the database
            repository.save(schedule);
        }
    }

    private void sendNotification(PaymentSchedule schedule, String timeRemaining) {
        // Run the email sending in a try-catch so an email failure
        // doesn't crash the entire background loop for other users
        try {
            emailService.sendPaymentWarning(
                    schedule.getUserEmail(),
                    timeRemaining,
                    schedule.getAmount().toPlainString(),
                    schedule.getTargetAccount()
            );
            System.out.println("SUCCESS: " + timeRemaining + " warning sent to " + schedule.getUserEmail());
        } catch (Exception e) {
            System.err.println("FAILED to send email to " + schedule.getUserEmail() + ": " + e.getMessage());
        }
    }
}
