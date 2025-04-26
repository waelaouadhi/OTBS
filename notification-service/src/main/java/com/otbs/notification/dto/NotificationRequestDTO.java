package com.otbs.notification.dto;

import com.otbs.notification.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDTO {
    private String title;
    private String message;
    private String sender;
    private String recipient;
    private NotificationType type;
    private String sourceId;
    private String actionUrl;
}
