package com.twinzpay.payment.client;

import com.twinzpay.payment.dto.EmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Tell Eureka to route this exactly to your Scheduler Service
@FeignClient(name = "SCHEDULER-SERVICE")
public interface SchedulerClient {
    // The path now perfectly matches the new endpoint in the ScheduleController
    @PostMapping("/api/v1/schedules/email/send")
    void sendNotification(@RequestBody EmailRequest request);
}
