package com.otbs.training.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrainingNotificationService {
    private final RabbitTemplate rabbitTemplate;

    @Value("${notification.rabbitmq.exchange}")
    private String notificationExchange;

    @Value("${notification.rabbitmq.training-routing-key}")
    private String medicalVisitRoutingKey;


    public void sendTrainingNotification(Long trainingId, String recipient) {
        Map<String, Object> message = new HashMap<>();
        message.put("trainingId", trainingId);
        message.put("recipient", recipient);
        try {
            rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
            rabbitTemplate.convertAndSend(notificationExchange, medicalVisitRoutingKey, message);
            log.info("Training notification sent successfully for trainingId: {}", trainingId);
        } catch (Exception e) {
            log.error("Failed to send training notification for trainingId: {}. Error: {}", trainingId, e.getMessage());
        }
    }
}
