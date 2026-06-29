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

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void trackAndNotifySchedules() {
        LocalDateTime now = LocalDateTime.now();

        // 1. TRUNCATE SECONDS: Synchronizes the clock
        LocalDateTime truncatedNow = now.withSecond(0).withNano(0);
        int today = truncatedNow.getDayOfMonth();

        // 2. Fetch all ACTIVE schedules for today
        List<PaymentSchedule> todaysSchedules = repository.findByDayOfMonthAndStatus(today, "ACTIVE");

        for (PaymentSchedule schedule : todaysSchedules) {

            // 3. Anti-Duplicate Lock
            if (schedule.getLastExecutedMonth() != null &&
                    YearMonth.from(schedule.getLastExecutedMonth()).equals(YearMonth.from(truncatedNow))) {
                continue;
            }

            // 4. Base the execution time strictly on the truncated clock
            LocalDateTime executionTime = truncatedNow
                    .withHour(schedule.getTargetHour())
                    .withMinute(schedule.getTargetMinute());

            long minutesLeft = Duration.between(truncatedNow, executionTime).toMinutes();

            if (minutesLeft < 0) continue;

            // 5. ABSOLUTE PRIORITY: ZERO HOUR EXECUTION
            if (minutesLeft == 0) {
                System.out.println("ZERO HOUR: Triggering Auto-Charge for " + schedule.getUserEmail());

                boolean paymentSuccess = triggerInternalPayment(schedule);

                if (paymentSuccess) {
                    schedule.setLastExecutedMonth(truncatedNow);
                    schedule.setThreeHourWarningSent(false);
                    schedule.setThirtyMinWarningSent(false);
                    schedule.setFifteenMinWarningSent(false);
                    schedule.setStatus("ACTIVE");

                    sendSuccessReceipt(schedule);
                } else {
                    System.err.println("Auto-Charge Failed for " + schedule.getUserEmail());
                    schedule.setStatus("SUSPENDED");
                    sendFailureNotice(schedule);
                }
            }
            // 6. SECONDARY PRIORITY: WARNING NOTIFICATIONS
            else if (minutesLeft > 0) {

                // Cascade from smallest window to largest.
                // This ensures if a schedule is created 10 mins before execution, it only sends the 15-min warning.
                if (minutesLeft <= 15 && !schedule.isFifteenMinWarningSent()) {
                    System.out.println("CRON DEBUG: Triggering 15-Minute Warning for " + schedule.getUserEmail());
                    sendNotification(schedule, "15 Minutes");

                    // Mark all as sent to prevent spamming older warnings
                    schedule.setFifteenMinWarningSent(true);
                    schedule.setThirtyMinWarningSent(true);
                    schedule.setThreeHourWarningSent(true);
                }
                else if (minutesLeft <= 30 && !schedule.isThirtyMinWarningSent()) {
                    System.out.println("CRON DEBUG: Triggering 30-Minute Warning for " + schedule.getUserEmail());
                    sendNotification(schedule, "30 Minutes");

                    schedule.setThirtyMinWarningSent(true);
                    schedule.setThreeHourWarningSent(true);
                }
                else if (minutesLeft <= 180 && !schedule.isThreeHourWarningSent()) {
                    System.out.println("CRON DEBUG: Triggering 3-Hour Warning for " + schedule.getUserEmail());
                    sendNotification(schedule, "3 Hours");

                    schedule.setThreeHourWarningSent(true);
                }
            }

            repository.save(schedule);
        }
    }

    // --- RESILIENCE4J RETRY BLOCK ---
    @io.github.resilience4j.retry.annotation.Retry(name = "paymentService", fallbackMethod = "paymentFallback")
    private boolean triggerInternalPayment(PaymentSchedule schedule) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userEmail", schedule.getUserEmail());
        requestBody.put("amount", schedule.getAmount());
        requestBody.put("billPlanId", schedule.getBillPlanId());

        ResponseEntity<Void> response = restClient.post()
                .uri("http://PAYMENT-SERVICE/api/v1/internal/payments/auto-charge")
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();

        return response.getStatusCode().is2xxSuccessful();
    }

    private boolean paymentFallback(PaymentSchedule schedule, Exception e) {
        System.err.println("CRITICAL: Payment Service is unresponsive after 3 retries. Error: " + e.getMessage());
        return false;
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
            System.err.println("Failed to send " + timeRemaining + " warning to " + schedule.getUserEmail() + ". Error: " + e.getMessage());
        }
    }

    private void sendSuccessReceipt(PaymentSchedule schedule) {
        System.out.println("Receipt sent to " + schedule.getUserEmail() + " for successful auto-debit.");
    }

    private void sendFailureNotice(PaymentSchedule schedule) {
        System.out.println("Failure notice sent to " + schedule.getUserEmail() + ". Schedule suspended.");
    }
}
