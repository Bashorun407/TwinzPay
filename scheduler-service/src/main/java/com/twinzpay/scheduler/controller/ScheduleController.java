package com.twinzpay.scheduler.controller;

import com.twinzpay.scheduler.dto.CreateScheduleRequest;
import com.twinzpay.scheduler.dto.EmailRequest; // We will create this DTO next
import com.twinzpay.scheduler.entity.PaymentSchedule;
import com.twinzpay.scheduler.service.ScheduleService;
import com.twinzpay.scheduler.service.EmailService; // Injecting your EmailService
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;
    private final EmailService emailService; // 1. Added dependency

    // 2. Updated Constructor
    public ScheduleController(ScheduleService scheduleService, EmailService emailService) {
        this.scheduleService = scheduleService;
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<PaymentSchedule> create(@RequestBody CreateScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(request));
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<String> reactivateSchedule(
            @PathVariable Long id,
            @RequestParam String userEmail) {
        try {
            PaymentSchedule reactivatedSchedule = scheduleService.reactivateSchedule(id, userEmail);
            return ResponseEntity.ok("Schedule successfully reactivated. Status is now: " + reactivatedSchedule.getStatus());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 👉 3. THE NEW ENDPOINT: This listens for the Payment Service Feign Client
    @PostMapping("/email/send")
    public ResponseEntity<Void> triggerEmail(@RequestBody EmailRequest request) {
        // Assuming your EmailService has or will have a generic sendEmail method
        emailService.sendEmail(request.to(), request.subject(), request.body());
        return ResponseEntity.ok().build();
    }
}