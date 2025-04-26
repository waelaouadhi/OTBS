package com.otbs.notification.repository;
import com.otbs.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(String recipient);
    List<Notification> findByRecipientIsNullOrderByCreatedAtDesc();
    List<Notification> findByRecipientAndReadOrderByCreatedAtDesc(String recipient, boolean read);
    long countByRecipientAndRead(String recipient, boolean read);
}
