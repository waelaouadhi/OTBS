package com.otbs.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${notification.rabbitmq.exchange}")
    private String notificationExchange;

    @Value("${notification.rabbitmq.leave-request-queue}")
    private String leaveRequestQueue;

    @Value("${notification.rabbitmq.leave-request-routing-key}")
    private String leaveRequestRoutingKey;

    @Value("${notification.rabbitmq.medical-visit-queue}")
    private String medicalVisitQueue;

    @Value("${notification.rabbitmq.medical-visit-routing-key}")
    private String medicalVisitRoutingKey;

    @Value("${notification.rabbitmq.training-queue}")
    private String trainingQueue;

    @Value("${notification.rabbitmq.training-routing-key}")
    private String trainingRoutingKey;

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(notificationExchange);
    }

    @Bean
    public Queue leaveRequestQueue() {
        return new Queue(leaveRequestQueue, true);
    }

    @Bean
    public Queue medicalVisitQueue() {
        return new Queue(medicalVisitQueue, true);
    }

    @Bean
    public Queue trainingQueue() {
        return new Queue(trainingQueue, true);
    }

    @Bean
    public Binding trainingBinding() {
        return BindingBuilder
                .bind(trainingQueue())
                .to(notificationExchange())
                .with(trainingRoutingKey);
    }

    @Bean
    public Binding leaveRequestBinding() {
        return BindingBuilder
                .bind(leaveRequestQueue())
                .to(notificationExchange())
                .with(leaveRequestRoutingKey);
    }

    @Bean
    public Binding medicalVisitBinding() {
        return BindingBuilder
                .bind(medicalVisitQueue())
                .to(notificationExchange())
                .with(medicalVisitRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
