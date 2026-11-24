package com.campushelp.controller;

import com.campushelp.dto.NotificationResponse;
import com.campushelp.entity.User;
import com.campushelp.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** In-app notifications (bell icon). */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal User user) {
        return notifications.list(user);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal User user) {
        return Map.of("unread", notifications.unreadCount(user));
    }

    @PatchMapping("/{id}/read")
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        notifications.markRead(user, id);
    }

    @PatchMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal User user) {
        notifications.markAllRead(user);
    }
}
