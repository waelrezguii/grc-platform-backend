package com.project.grcplatform.controller;

import com.project.grcplatform.dto.NotificationResponseDTO;
import com.project.grcplatform.scheduler.NotificationScheduler;
import com.project.grcplatform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationScheduler notificationScheduler;

    @GetMapping
    public ResponseEntity<Page<NotificationResponseDTO>> getMyNotifications(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                notificationService.getMyNotifications(
                        read,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())
                )
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        notificationService.delete(id);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "Notification deleted"));
    }
    @PostMapping("/test/trigger-scheduler")
    public ResponseEntity<Map<String, String>> triggerScheduler() {
        notificationScheduler.checkOverdueControlReviews();
        notificationScheduler.checkOverdueTreatmentActions();
        notificationScheduler.checkAssessmentsDueSoon();
        return ResponseEntity.ok(Map.of(
                "code", "SUCCESS",
                "message", "All 3 scheduler jobs triggered"
        ));
    }
}