package com.fooddelivery.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application configuration class.
 * Scans all packages under com.fooddelivery for components.
 */
@SpringBootApplication(scanBasePackages = "com.fooddelivery")
public class CustomerServiceConfig {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceConfig.class, args);
    }
}
