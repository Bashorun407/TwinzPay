package com.twinzpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class UtilityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UtilityServiceApplication.class, args);
    }
}
