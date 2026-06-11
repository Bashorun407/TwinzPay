package com.twinzpay.scheduler.controller;

import com.twinzpay.scheduler.dto.CreateScheduleRequest;
import com.twinzpay.scheduler.entity.PaymentSchedule;
import com.twinzpay.scheduler.service.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<PaymentSchedule> create(@RequestBody CreateScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(request));
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<String> reactivateSchedule(
            @PathVariable Long id,
            @RequestParam String userEmail) { // In a real production app, extract this email from the JWT token instead!

        try {
            PaymentSchedule reactivatedSchedule = scheduleService.reactivateSchedule(id, userEmail);
            return ResponseEntity.ok("Schedule successfully reactivated. Status is now: " + reactivatedSchedule.getStatus());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
