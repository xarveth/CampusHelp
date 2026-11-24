package com.campushelp.service;

import com.campushelp.dto.NotificationResponse;
import com.campushelp.entity.Complaint;
import com.campushelp.entity.Notification;
import com.campushelp.entity.Role;
import com.campushelp.entity.User;
import com.campushelp.repository.NotificationRepository;
import com.campushelp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** In-app notifications (bell icon). No email is sent. */
@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    /** Store a notification for one user. */
    @Transactional
    public void notify(User user, Complaint complaint, String message) {
        notifications.save(new Notification(user, complaint, message));
    }

    /** Store the same notification for every admin. */
    @Transactional
    public void notifyAdmins(Complaint complaint, String message) {
        for (User admin : users.findByRole(Role.ADMIN)) {
            notifications.save(new Notification(admin, complaint, message));
        }
    }

    public List<NotificationResponse> list(User user) {
        return notifications.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(NotificationResponse::of).toList();
    }

    public long unreadCount(User user) {
        return notifications.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public void markRead(User user, Long notificationId) {
        Notification n = notifications.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        n.setRead(true);
        notifications.save(n);
    }

    @Transactional
    public void markAllRead(User user) {
        for (Notification n : notifications.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId())) {
            n.setRead(true);
            notifications.save(n);
        }
    }
}
