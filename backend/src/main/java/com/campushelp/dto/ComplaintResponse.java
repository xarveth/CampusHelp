package com.campushelp.dto;

import com.campushelp.entity.Complaint;
import com.campushelp.entity.ComplaintStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ComplaintResponse(
        Long id,
        String code,
        String subject,
        String description,
        String category,
        Long categoryId,
        String status,
        String studentName,
        String studentEmail,
        LocalDateTime createdAt,
        List<StatusHistoryResponse> history
) {
    public static ComplaintResponse of(Complaint c, List<StatusHistoryResponse> history) {
        return new ComplaintResponse(
                c.getId(), c.getCode(), c.getSubject(), c.getDescription(),
                c.getCategory().getName(), c.getCategory().getId(),
                c.getStatus().name(),
                c.getStudent().getName(), c.getStudent().getEmail(),
                c.getCreatedAt(), history);
    }

    public record StatusHistoryResponse(
            String oldStatus, String newStatus, String changedBy, LocalDateTime changedAt) {}
}
