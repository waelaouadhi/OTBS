package com.otbs.notification.controller;

import com.otbs.notification.dto.NotificationRequestDTO;
import com.otbs.notification.dto.NotificationResponseDTO;
import com.otbs.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public void createNotification(@RequestBody NotificationRequestDTO requestDTO) {
        notificationService.createNotification(requestDTO);
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getUserNotifications() {
        String username = getCurrentUsername();
        List<NotificationResponseDTO> notifications = notificationService.getUserNotifications(username);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponseDTO>> getUnreadNotifications() {
        String username = getCurrentUsername();
        List<NotificationResponseDTO> notifications = notificationService.getUnreadNotifications(username);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount() {
        String username = getCurrentUsername();
        long count = notificationService.getUnreadCount(username);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable Long id) {
        NotificationResponseDTO notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        String username = getCurrentUsername();
        notificationService.markAllAsRead(username);
        return ResponseEntity.noContent().build();
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
