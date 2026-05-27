package com.twinzpay.apigateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {
    // List of endpoints that are explicitly public
    public static final List<String> openApiEndpoints = List.of(
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/api/v1/payments/webhook",
            "/v3/api-docs",        // Allows discovery of microservice schemas
            "/swagger-ui",         // Allows loading the UI layout assets
            "/eureka"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}
