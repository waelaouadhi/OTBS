package com.otbs.notification.dto;

import com.otbs.notification.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private Long id;
    private String title;
    private String message;
    private String sender;
    private String recipient;
    private NotificationType type;
    private LocalDateTime createdAt;
    private boolean read;
    private String sourceId;
    private String actionUrl;
}
