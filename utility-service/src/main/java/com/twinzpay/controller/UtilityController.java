package com.twinzpay.controller;

import com.twinzpay.dto.BillPlanResponseDto;
import com.twinzpay.dto.BillerCategoryResponseDto;
import com.twinzpay.dto.BillerResponseDto;
import com.twinzpay.service.UtilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/utilities")
public class UtilityController {
    private final UtilityService utilityService;

    // Constructor injection (Best practice for required dependencies)
    public UtilityController(UtilityService utilityService) {
        this.utilityService = utilityService;
    }

    // Endpoint: GET http://localhost:8080/api/v1/utilities/categories
    @GetMapping("/categories")
    public ResponseEntity<List<BillerCategoryResponseDto>> getCategories() {
        return ResponseEntity.ok(utilityService.getAllCategories());
    }

    // Endpoint: GET http://localhost:8080/api/v1/utilities/categories/1/billers
    @GetMapping("/categories/{id}/billers")
    public ResponseEntity<List<BillerResponseDto>> getBillers(@PathVariable Long id) {
        return ResponseEntity.ok(utilityService.getBillersByCategory(id));
    }

    // Endpoint: GET http://localhost:8080/api/v1/utilities/billers/1/plans
    @GetMapping("/billers/{id}/plans")
    public ResponseEntity<List<BillPlanResponseDto>> getPlans(@PathVariable Long id) {
        return ResponseEntity.ok(utilityService.getPlansByBiller(id));
    }
}
