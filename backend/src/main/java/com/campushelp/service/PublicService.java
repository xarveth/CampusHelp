package com.campushelp.service;

import com.campushelp.entity.ComplaintStatus;
import com.campushelp.repository.ComplaintRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public numbers for the homepage. Only aggregate counts and an anonymised
 * board of open complaints — no student names, emails or descriptions.
 */
@Service
public class PublicService {

    private final ComplaintRepository complaints;

    public PublicService(ComplaintRepository complaints) {
        this.complaints = complaints;
    }

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = complaints.count();
        long resolved = complaints.countByStatus(ComplaintStatus.RESOLVED);
        stats.put("total", total);
        stats.put("resolved", resolved);
        stats.put("open", total - resolved);
        stats.put("resolutionRate", total == 0 ? 0
                : Math.round((resolved * 100.0 / total) * 10.0) / 10.0);
        List<com.campushelp.entity.Complaint> resolvedList = complaints.findResolvedWithResolvedAt();
        Double avgHours = null;
        if (!resolvedList.isEmpty()) {
            double sum = 0;
            for (com.campushelp.entity.Complaint c : resolvedList) {
                sum += Duration.between(c.getCreatedAt(), c.getResolvedAt()).toMinutes() / 60.0;
            }
            avgHours = Math.round((sum / resolvedList.size()) * 10.0) / 10.0;
        }
        stats.put("averageResolutionHours", avgHours);
        return stats;
    }

    /** Open complaints, newest first — subject and category only, no student data. */
    public List<Map<String, Object>> openBoard() {
        return complaints.findAllByOrderByCreatedAtDesc().stream()
                .filter(c -> c.getStatus() != ComplaintStatus.RESOLVED)
                .limit(10)
                .map(c -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", c.getId());
                    row.put("code", c.getCode());
                    row.put("subject", c.getSubject());
                    row.put("category", c.getCategory().getName());
                    row.put("status", c.getStatus().name());
                    row.put("createdAt", c.getCreatedAt());
                    return row;
                })
                .toList();
    }
}
