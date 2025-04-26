package com.otbs.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.otbs.feign")
public class TrainingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrainingApplication.class, args);
    }
}