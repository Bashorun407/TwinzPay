package com.twinzpay.service;

import com.twinzpay.dto.BillPlanResponseDto;
import com.twinzpay.dto.BillerCategoryResponseDto;
import com.twinzpay.dto.BillerResponseDto;
import com.twinzpay.entity.BillPlan;
import com.twinzpay.entity.Biller;
import com.twinzpay.entity.BillerCategory;
import com.twinzpay.repository.BillPlanRepository;
import com.twinzpay.repository.BillerCategoryRepository;
import com.twinzpay.repository.BillerRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UtilityService {
    private final BillerCategoryRepository categoryRepository;
    private final BillerRepository billerRepository;
    private final BillPlanRepository planRepository;

    public UtilityService(BillerCategoryRepository categoryRepository,
                          BillerRepository billerRepository,
                          BillPlanRepository planRepository) {
        this.categoryRepository = categoryRepository;
        this.billerRepository = billerRepository;
        this.planRepository = planRepository;
    }

    @Transactional // OPTIMIZED: Explicitly open a write-transaction only where needed
    @PostConstruct
    public void seedDatabase() {
        if (categoryRepository.count() == 0) {
            // 1. Create Airtime Category
            BillerCategory airtime = categoryRepository.save(BillerCategory.builder().name("Airtime & Data").build());
            Biller mtn = billerRepository.save(Biller.builder().name("MTN").category(airtime).build());
            planRepository.save(BillPlan.builder().planName("MTN 10GB Monthly Data").price(BigDecimal.valueOf(3500)).biller(mtn).build());
            planRepository.save(BillPlan.builder().planName("MTN Airtime VTU").price(BigDecimal.ZERO).biller(mtn).build());

            // 2. Create Electricity Category
            BillerCategory power = categoryRepository.save(BillerCategory.builder().name("Electricity").build());
            Biller ikedc = billerRepository.save(Biller.builder().name("Ikeja Electric").category(power).build());
            planRepository.save(BillPlan.builder().planName("IKEDC Prepaid Token").price(BigDecimal.ZERO).biller(ikedc).build());
        }
    }

    public List<BillerCategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> new BillerCategoryResponseDto(c.getId(), c.getName()))
                .toList(); // OPTIMIZED: Faster compilation, unmodifiable list, no Collectors import
    }

    public List<BillerResponseDto> getBillersByCategory(Long categoryId) {
        return billerRepository.findByCategoryId(categoryId).stream()
                .map(b -> new BillerResponseDto(b.getId(), b.getName()))
                .toList(); // OPTIMIZED
    }

    public List<BillPlanResponseDto> getPlansByBiller(Long billerId) {
        return planRepository.findByBillerId(billerId).stream()
                .map(p -> new BillPlanResponseDto(p.getId(), p.getPlanName(), p.getPrice()))
                .toList(); // OPTIMIZED
    }
}
