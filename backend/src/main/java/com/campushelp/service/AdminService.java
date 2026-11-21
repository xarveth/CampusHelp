package com.campushelp.service;

import com.campushelp.dto.ComplaintResponse;
import com.campushelp.dto.CreateStaffRequest;
import com.campushelp.dto.StaffResponse;
import com.campushelp.entity.Complaint;
import com.campushelp.entity.ComplaintStatus;
import com.campushelp.entity.Role;
import com.campushelp.entity.StatusHistory;
import com.campushelp.entity.User;
import com.campushelp.repository.ComplaintRepository;
import com.campushelp.repository.RatingRepository;
import com.campushelp.repository.StatusHistoryRepository;
import com.campushelp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserRepository users;
    private final RatingRepository ratings;
    private final PasswordEncoder passwordEncoder;

    public AdminService(ComplaintRepository complaints,
                        StatusHistoryRepository history,
                        ComplaintService complaintService,
                        UserRepository users,
                        RatingRepository ratings,
                        PasswordEncoder passwordEncoder) {
        this.complaints = complaints;
        this.history = history;
        this.complaintService = complaintService;
        this.users = users;
        this.ratings = ratings;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> dashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", complaints.count());
        stats.put("pending", complaints.countByStatus(ComplaintStatus.PENDING));
        stats.put("inProgress", complaints.countByStatus(ComplaintStatus.IN_PROGRESS));
        stats.put("resolved", complaints.countByStatus(ComplaintStatus.RESOLVED));
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("PENDING", complaints.countByStatus(ComplaintStatus.PENDING));
        byStatus.put("IN_PROGRESS", complaints.countByStatus(ComplaintStatus.IN_PROGRESS));
        byStatus.put("RESOLVED", complaints.countByStatus(ComplaintStatus.RESOLVED));
        stats.put("byStatus", byStatus);
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : complaints.countByCategory()) {
            byCategory.put((String) row[0], (Long) row[1]);
        }
        stats.put("byCategory", byCategory);
        Double avg = ratings.averageStars();
        stats.put("averageRating", avg == null ? null : Math.round(avg * 10.0) / 10.0);
        stats.put("ratingCount", ratings.count());
        stats.put("recent", complaints.findAllByOrderByCreatedAtDesc().stream()
                .limit(5).map(complaintService::toResponse).toList());
        return stats;
    }

    /** Admin creates a staff account (STAFF role). */
    public StaffResponse createStaff(CreateStaffRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        User staff = new User(req.name().trim(), req.email().trim(),
                passwordEncoder.encode(req.password()), Role.STAFF);
        users.save(staff);
        return StaffResponse.of(staff);
    }

    public List<StaffResponse> listStaff() {
        return users.findByRole(Role.STAFF).stream().map(StaffResponse::of).toList();
    }

    /** Assign a complaint to a staff member (or unassign with staffId null). */
    @Transactional
    public ComplaintResponse assignComplaint(Long complaintId, Long staffId) {
        Complaint complaint = complaints.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        if (staffId == null) {
            complaint.setAssignedTo(null);
        } else {
            User staff = users.findById(staffId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));
            if (staff.getRole() != Role.STAFF) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not staff");
            }
            complaint.setAssignedTo(staff);
        }
        complaints.save(complaint);
        return complaintService.toResponse(complaint);
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
