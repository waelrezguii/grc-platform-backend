package com.project.grcplatform.repository;

import com.project.grcplatform.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    Optional<Notification> findByIdAndRecipient_Id(String id, String recipientId);

    @Query("""
        SELECT n FROM Notification n
        WHERE n.recipient.id = :recipientId
          AND (:read IS NULL OR n.read = :read)
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> findByRecipientId(
            @Param("recipientId") String recipientId,
            @Param("read") Boolean read,
            Pageable pageable
    );

    long countByRecipient_IdAndReadFalse(String recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now WHERE n.recipient.id = :recipientId AND n.read = false")
    void markAllAsRead(@Param("recipientId") String recipientId, @Param("now") LocalDateTime now);
}