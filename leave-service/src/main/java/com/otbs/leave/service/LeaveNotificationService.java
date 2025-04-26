package com.otbs.leave.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeaveNotificationService {
    private final RabbitTemplate rabbitTemplate;

    @Value("${notification.rabbitmq.exchange}")
    private String notificationExchange;

    @Value("${notification.rabbitmq.leave-request-routing-key}")
    private String medicalVisitRoutingKey;


    public void sendMedicalVisitNotification(String title, String scheduleDate, String recipient) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("message", "A new leave request has been scheduled ");
            message.put("sender", "leave-request-service");
            message.put("actionUrl", "/leave");
            message.put("recipient", Optional.ofNullable(recipient).orElse(""));

            log.info("Sending leave request notification: {}", message);
            rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
            rabbitTemplate.convertAndSend(notificationExchange, medicalVisitRoutingKey, message);
        } catch (Exception e) {
            log.error("Error sending leave request notification: {}", e.getMessage(), e);
        }
    }
}
