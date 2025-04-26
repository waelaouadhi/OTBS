package com.otbs.notification.service;

import com.otbs.notification.dto.NotificationRequestDTO;
import com.otbs.notification.dto.NotificationResponseDTO;
import com.otbs.notification.dto.WebSocketPayloadDTO;
import com.otbs.notification.model.Notification;
import com.otbs.notification.model.NotificationType;
import com.otbs.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public NotificationResponseDTO createNotification(NotificationRequestDTO requestDTO) {
        Notification notification = Notification.builder()
                .title(Optional.ofNullable(requestDTO.getTitle()).orElse(""))
                .message(Optional.ofNullable(requestDTO.getMessage()).orElse(""))
                .sender(Optional.ofNullable(requestDTO.getSender()).orElse(""))
                .recipient(requestDTO.getRecipient())
                .type(Optional.ofNullable(requestDTO.getType()).orElse(NotificationType.SYSTEM_ANNOUNCEMENT))
                .sourceId(Optional.ofNullable(requestDTO.getSourceId()).orElse(""))
                .actionUrl(Optional.ofNullable(requestDTO.getActionUrl()).orElse(""))
                .createdAt(LocalDateTime.now())
                .read(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification saved: {}", savedNotification);

        NotificationResponseDTO responseDTO = mapToResponseDTO(savedNotification);
        log.info("Notification created: {}", responseDTO);
        sendWebSocketNotification(responseDTO);
        log.info("WebSocket notification sent: {}", responseDTO);

        return responseDTO;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getUserNotifications(String username) {
        List<Notification> userNotifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(username);
        List<Notification> broadcastNotifications = notificationRepository.findByRecipientIsNullOrderByCreatedAtDesc();

        List<Notification> allNotifications = Stream.concat(userNotifications.stream(), broadcastNotifications.stream())
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .collect(Collectors.toList());

        return allNotifications.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getUnreadNotifications(String username) {
        List<Notification> unreadNotifications = notificationRepository.findByRecipientAndReadOrderByCreatedAtDesc(username, false);
        return unreadNotifications.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        return notificationRepository.countByRecipientAndRead(username, false);
    }

    @Transactional
    public NotificationResponseDTO markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));

        notification.setRead(true);
        Notification updatedNotification = notificationRepository.save(notification);

        return mapToResponseDTO(updatedNotification);
    }

    @Transactional
    public void markAllAsRead(String username) {
        List<Notification> unreadNotifications = notificationRepository.findByRecipientAndReadOrderByCreatedAtDesc(username, false);
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    private NotificationResponseDTO mapToResponseDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .sender(notification.getSender())
                .recipient(notification.getRecipient())
                .type(notification.getType())
                .createdAt(notification.getCreatedAt())
                .read(notification.isRead())
                .sourceId(notification.getSourceId())
                .actionUrl(notification.getActionUrl())
                .build();
    }

    private void sendWebSocketNotification(NotificationResponseDTO notification) {
        WebSocketPayloadDTO payload = WebSocketPayloadDTO.builder()
                .type("NOTIFICATION")
                .data(notification)
                .build();

        log.info("Sending WebSocket notification: {}", notification);
        messagingTemplate.convertAndSend("/topic/notifications", payload);
//        if (notification.getRecipient() != null && !notification.getRecipient().isEmpty()) {
//            messagingTemplate.convertAndSendToUser(
//                    notification.getRecipient(),
//                    "/queue/notifications",
//                    payload
//            );
//            log.info("Sent notification to user: {}", notification.getRecipient());
//        } else {
//            messagingTemplate.convertAndSend("/topic/notifications", payload);
//            log.info("Broadcasted notification to all users");
//        }
    }
}