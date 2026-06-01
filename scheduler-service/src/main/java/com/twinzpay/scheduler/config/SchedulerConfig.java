package com.twinzpay.scheduler.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SchedulerConfig {
    @Bean
    @LoadBalanced // Tells Spring to resolve service names like "PAYMENT-SERVICE" via Eureka
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient loadBalancedRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
