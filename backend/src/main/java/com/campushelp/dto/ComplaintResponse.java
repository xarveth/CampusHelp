package com.campushelp.dto;

import com.campushelp.entity.Complaint;
import com.campushelp.entity.ComplaintStatus;
import com.campushelp.entity.Priority;
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
        String priority,
        String studentName,
        String studentEmail,
        String photoUrl,
        Long assignedToId,
        String assignedToName,
        RatingResponse rating,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String duplicateOfCode,
        long sameIssueCount,
        boolean iReportedSameIssue,
        List<StatusHistoryResponse> history,
        List<UpdateResponse> updates
) {
    public static ComplaintResponse of(Complaint c, List<StatusHistoryResponse> history) {
        return of(c, history, null, null, 0, false, List.of());
    }

    public static ComplaintResponse of(Complaint c, List<StatusHistoryResponse> history,
                                       RatingResponse rating) {
        return of(c, history, rating, null, 0, false, List.of());
    }

    public static ComplaintResponse of(Complaint c, List<StatusHistoryResponse> history,
                                       RatingResponse rating, String duplicateOfCode,
                                       long sameIssueCount, boolean iReportedSameIssue,
                                       List<UpdateResponse> updates) {
        return new ComplaintResponse(
                c.getId(), c.getCode(), c.getSubject(), c.getDescription(),
                c.getCategory().getName(), c.getCategory().getId(),
                c.getStatus().name(),
                c.getPriority() == null ? Priority.MEDIUM.name() : c.getPriority().name(),
                c.getStudent().getName(), c.getStudent().getEmail(),
                c.getPhotoUrl(),
                c.getAssignedTo() == null ? null : c.getAssignedTo().getId(),
                c.getAssignedTo() == null ? null : c.getAssignedTo().getName(),
                rating,
                c.getCreatedAt(), c.getResolvedAt(), duplicateOfCode,
                sameIssueCount, iReportedSameIssue,
                history, updates);
    }

    public record StatusHistoryResponse(
            String oldStatus, String newStatus, String changedBy,
            String note, LocalDateTime changedAt) {}

    public record RatingResponse(int stars, String comment, LocalDateTime createdAt) {}

    public record UpdateResponse(
            Long id, String authorName, String authorRole,
            String message, LocalDateTime createdAt) {}
}
