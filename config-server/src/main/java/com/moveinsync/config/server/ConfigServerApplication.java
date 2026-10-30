package com.moveinsync.config.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the MoveInSync Hierarchical Tenant Configuration Service.
 */
@SpringBootApplication
@EnableScheduling
public class ConfigServerApplication {

    // Launches the Spring Boot application and boots all configuration microservices.
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
