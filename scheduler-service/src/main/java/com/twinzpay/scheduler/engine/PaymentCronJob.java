package com.twinzpay.scheduler.engine;

import com.twinzpay.scheduler.entity.PaymentSchedule;
import com.twinzpay.scheduler.repository.PaymentScheduleRepository;
import com.twinzpay.scheduler.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentCronJob {
    private final PaymentScheduleRepository repository;
    private final EmailService emailService;
    private final RestClient restClient;

    public PaymentCronJob(PaymentScheduleRepository repository, EmailService emailService, RestClient restClient) {
        this.repository = repository;
        this.emailService = emailService;
        this.restClient = restClient;
    }

    // Cron expression: runs at the 0th second of every single minute
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void trackAndNotifySchedules() {
        LocalDateTime now = LocalDateTime.now();
        int today = now.getDayOfMonth();

        // 1. Fetch all ACTIVE schedules for today
        List<PaymentSchedule> todaysSchedules = repository.findByDayOfMonthAndStatus(today, "ACTIVE");

        for (PaymentSchedule schedule : todaysSchedules) {

            // 2. Anti-Duplicate Lock: Skip if already paid successfully THIS month
            if (schedule.getLastExecutedMonth() != null &&
                    YearMonth.from(schedule.getLastExecutedMonth()).equals(YearMonth.from(now))) {
                continue;
            }

            // 3. Construct the exact execution time for today
            LocalDateTime executionTime = now
                    .withHour(schedule.getTargetHour())
                    .withMinute(schedule.getTargetMinute())
                    .withSecond(0)
                    .withNano(0);

            // 4. Calculate how many minutes are left
            long minutesLeft = Duration.between(now, executionTime).toMinutes();

            // Ignore schedules that have already passed for today
            if (minutesLeft < 0) continue;

            // 5. ABSOLUTE PRIORITY: ZERO HOUR EXECUTION
            if (minutesLeft == 0) {
                System.out.println("ZERO HOUR: Triggering Auto-Charge for " + schedule.getUserEmail());

                boolean paymentSuccess = triggerInternalPayment(schedule);

                if (paymentSuccess) {
                    // Mark as executed for this specific month and year
                    schedule.setLastExecutedMonth(now);

                    // Reset all warning flags so they can trigger again NEXT month
                    schedule.setThreeHourWarningSent(false);
                    schedule.setThirtyMinWarningSent(false);
                    schedule.setFifteenMinWarningSent(false);
                    schedule.setStatus("ACTIVE"); // Ensure it remains active for next month

                    sendSuccessReceipt(schedule);
                } else {
                    System.err.println("Auto-Charge Failed for " + schedule.getUserEmail());

                    // Suspend the schedule to prevent infinite retry loops on a dead card
                    schedule.setStatus("SUSPENDED");
                    sendFailureNotice(schedule);
                }
            }
            // 6. SECONDARY PRIORITY: WARNING NOTIFICATIONS
            else if (minutesLeft > 0) {
                // Independent 'if' statements ensure that if a schedule is created late,
                // it can catch up and send multiple delayed warnings in a single minute.
                if (minutesLeft <= 180 && !schedule.isThreeHourWarningSent()) {
                    sendNotification(schedule, "3 Hours");
                    schedule.setThreeHourWarningSent(true);
                }
                if (minutesLeft <= 30 && !schedule.isThirtyMinWarningSent()) {
                    sendNotification(schedule, "30 Minutes");
                    schedule.setThirtyMinWarningSent(true);
                }
                if (minutesLeft <= 15 && !schedule.isFifteenMinWarningSent()) {
                    sendNotification(schedule, "15 Minutes");
                    schedule.setFifteenMinWarningSent(true);
                }
            }

            // 7. Save the updated states (flags and execution dates) back to the database
            repository.save(schedule);
        }
    }

    private boolean triggerInternalPayment(PaymentSchedule schedule) {
        try {
            // Build the payload matching the Payment Service's AutoChargeRequest DTO
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userEmail", schedule.getUserEmail());
            requestBody.put("amount", schedule.getAmount());
            requestBody.put("billPlanId", schedule.getBillPlanId());

            // Fire the request using Eureka routing and the modern RestClient
            ResponseEntity<Void> response = restClient.post()
                    .uri("http://PAYMENT-SERVICE/api/v1/internal/payments/auto-charge")
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity(); // Efficiently checks status without expecting a JSON response body

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("Internal Payment Call Failed: " + e.getMessage());
            return false;
        }
    }

    private void sendNotification(PaymentSchedule schedule, String timeRemaining) {
        try {
            emailService.sendPaymentWarning(
                    schedule.getUserEmail(),
                    timeRemaining,
                    schedule.getAmount().toPlainString(),
                    schedule.getTargetAccount()
            );
        } catch (Exception e) {
            System.err.println("Failed to send " + timeRemaining + " warning to " + schedule.getUserEmail());
        }
    }

    private void sendSuccessReceipt(PaymentSchedule schedule) {
        System.out.println("Receipt sent to " + schedule.getUserEmail() + " for successful auto-debit.");
        // Implement actual email template sending here if desired
    }

    private void sendFailureNotice(PaymentSchedule schedule) {
        System.out.println("Failure notice sent to " + schedule.getUserEmail() + ". Schedule suspended.");
        // Implement actual email template sending here to notify user to update their card
    }
}
