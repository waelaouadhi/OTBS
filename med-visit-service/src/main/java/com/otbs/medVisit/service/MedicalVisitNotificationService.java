package com.otbs.medVisit.service;

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
public class MedicalVisitNotificationService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${notification.rabbitmq.exchange}")
    private String notificationExchange;

    @Value("${notification.rabbitmq.medical-visit-routing-key}")
    private String medicalVisitRoutingKey;

    public MedicalVisitNotificationService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMedicalVisitNotification(String title, String scheduleDate, String recipient) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("message", "A new medical visit has been scheduled ");
            message.put("sender", "medical-visit-service");
            message.put("actionUrl", "/medical-visits");
            message.put("recipient", Optional.ofNullable(recipient).orElse(""));

            log.info("Sending medical visit notification: {}", message);
            rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
            rabbitTemplate.convertAndSend(notificationExchange, medicalVisitRoutingKey, message);
        } catch (Exception e) {
            log.error("Error sending medical visit notification: {}", e.getMessage(), e);
        }
    }
}