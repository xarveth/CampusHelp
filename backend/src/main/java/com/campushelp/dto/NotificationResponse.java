package com.campushelp.dto;

import com.campushelp.entity.Notification;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String message,
        Long complaintId,
        String complaintCode,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse of(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getMessage(),
                n.getComplaint() == null ? null : n.getComplaint().getId(),
                n.getComplaint() == null ? null : n.getComplaint().getCode(),
                n.isRead(), n.getCreatedAt());
    }
}
