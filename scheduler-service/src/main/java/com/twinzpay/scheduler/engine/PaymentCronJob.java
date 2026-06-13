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

        // 1. TRUNCATE SECONDS: This synchronizes the clock to prevent the early-firing bug!
        LocalDateTime truncatedNow = now.withSecond(0).withNano(0);
        int today = truncatedNow.getDayOfMonth();

        // 2. Fetch all ACTIVE schedules for today
        List<PaymentSchedule> todaysSchedules = repository.findByDayOfMonthAndStatus(today, "ACTIVE");

        for (PaymentSchedule schedule : todaysSchedules) {

            // 3. Anti-Duplicate Lock: Skip if already paid successfully THIS month
            if (schedule.getLastExecutedMonth() != null &&
                    YearMonth.from(schedule.getLastExecutedMonth()).equals(YearMonth.from(truncatedNow))) {
                continue;
            }

            // 4. Base the execution time strictly on the truncated clock
            LocalDateTime executionTime = truncatedNow
                    .withHour(schedule.getTargetHour())
                    .withMinute(schedule.getTargetMinute());

            // Calculate exact minutes left
            long minutesLeft = Duration.between(truncatedNow, executionTime).toMinutes();

            // Ignore schedules that have already passed for today
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

            repository.save(schedule);
        }
    }

    // --- RESILIENCE4J RETRY BLOCK ---
    @io.github.resilience4j.retry.annotation.Retry(name = "paymentService", fallbackMethod = "paymentFallback")
    private boolean triggerInternalPayment(PaymentSchedule schedule) {
        // Build the payload
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userEmail", schedule.getUserEmail());
        requestBody.put("amount", schedule.getAmount());
        requestBody.put("billPlanId", schedule.getBillPlanId());

        // Fire the request
        ResponseEntity<Void> response = restClient.post()
                .uri("http://PAYMENT-SERVICE/api/v1/internal/payments/auto-charge")
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();

        return response.getStatusCode().is2xxSuccessful();
    }

    // This method is triggered ONLY if the API call fails 3 times in a row
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
            System.err.println("Failed to send " + timeRemaining + " warning to " + schedule.getUserEmail());
        }
    }

    private void sendSuccessReceipt(PaymentSchedule schedule) {
        System.out.println("Receipt sent to " + schedule.getUserEmail() + " for successful auto-debit.");
    }

    private void sendFailureNotice(PaymentSchedule schedule) {
        System.out.println("Failure notice sent to " + schedule.getUserEmail() + ". Schedule suspended.");
    }
}
