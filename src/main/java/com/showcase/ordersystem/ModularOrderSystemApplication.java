package com.showcase.ordersystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Modular Order System - Spring Modulith Showcase
 * 
 * This application demonstrates:
 * - Modular architecture with Spring Modulith
 * - Asynchronous messaging with RabbitMQ
 * - Domain events for inter-module communication
 * - Clean architecture boundaries
 */
@EnableAsync
@Modulith
@SpringBootApplication
public class ModularOrderSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModularOrderSystemApplication.class, args);
    }
}