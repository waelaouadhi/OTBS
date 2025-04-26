package com.otbs.leave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.otbs.feign")
public class LeaveApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeaveApplication.class, args);
    }
}