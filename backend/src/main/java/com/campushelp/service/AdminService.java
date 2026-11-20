package com.campushelp.service;

import com.campushelp.dto.ComplaintResponse;
import com.campushelp.entity.Complaint;
import com.campushelp.entity.ComplaintStatus;
import com.campushelp.entity.StatusHistory;
import com.campushelp.entity.User;
import com.campushelp.repository.ComplaintRepository;
import com.campushelp.repository.StatusHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final ComplaintRepository complaints;
    private final StatusHistoryRepository history;
    private final ComplaintService complaintService;

    public AdminService(ComplaintRepository complaints,
                        StatusHistoryRepository history,
                        ComplaintService complaintService) {
        this.complaints = complaints;
        this.history = history;
        this.complaintService = complaintService;
    }

    public Map<String, Object> dashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", complaints.count());
        stats.put("pending", complaints.countByStatus(ComplaintStatus.PENDING));
        stats.put("inProgress", complaints.countByStatus(ComplaintStatus.IN_PROGRESS));
        stats.put("resolved", complaints.countByStatus(ComplaintStatus.RESOLVED));
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : complaints.countByCategory()) {
            byCategory.put((String) row[0], (Long) row[1]);
        }
        stats.put("byCategory", byCategory);
        stats.put("recent", complaints.findAllByOrderByCreatedAtDesc().stream()
                .limit(5).map(complaintService::toResponse).toList());
        return stats;
    }

    public List<ComplaintResponse> allComplaints(String status, String query) {
        List<Complaint> list = (status == null || status.isBlank())
                ? complaints.findAllByOrderByCreatedAtDesc()
                : complaints.findByStatusOrderByCreatedAtDesc(ComplaintStatus.valueOf(status));
        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase();
            list = list.stream().filter(c ->
                    c.getCode().toLowerCase().contains(q)
                            || c.getSubject().toLowerCase().contains(q)
                            || c.getStudent().getName().toLowerCase().contains(q)
                            || c.getCategory().getName().toLowerCase().contains(q)).toList();
        }
        return list.stream().map(complaintService::toResponse).toList();
    }

    @Transactional
    public ComplaintResponse updateStatus(Long complaintId, ComplaintStatus newStatus, User admin) {
        Complaint complaint = complaints.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        ComplaintStatus oldStatus = complaint.getStatus();
        if (oldStatus == newStatus) {
            return complaintService.toResponse(complaint);
        }
        complaint.setStatus(newStatus);
        complaints.save(complaint);
        history.save(new StatusHistory(complaint, admin, oldStatus, newStatus));
        return complaintService.toResponse(complaint);
    }
}
