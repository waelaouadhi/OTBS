package com.otbs.notification.controller;

import com.otbs.feign.dto.EmployeeResponse;
import com.otbs.notification.dto.NotificationRequestDTO;
import com.otbs.notification.dto.NotificationResponseDTO;
import com.otbs.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.messaging.SessionConnectEvent;

import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class WebSocketController {

    private final NotificationService notificationService;

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null || !sessionAttributes.containsKey("auth")) {
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }
        Authentication authentication = (Authentication) sessionAttributes.get("auth");

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }
        String username = authentication.getName();
        log.info("User connected: {}", username);
    }

    @MessageMapping("/notification")
    public NotificationResponseDTO processNotification(
            @Payload NotificationRequestDTO notificationRequest,
            SimpMessageHeaderAccessor headerAccessor) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }

        EmployeeResponse user = (EmployeeResponse) authentication.getPrincipal();
        log.info("Processing notification from user: {}", user.username());

        return notificationService.createNotification(notificationRequest);
    }

}