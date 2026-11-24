package com.campushelp.service;

import com.campushelp.dto.ComplaintResponse;
import com.campushelp.dto.CreateStaffRequest;
import com.campushelp.dto.StaffResponse;
import com.campushelp.entity.Complaint;
import com.campushelp.entity.ComplaintStatus;
import com.campushelp.entity.Priority;
import com.campushelp.entity.Role;
import com.campushelp.entity.StatusHistory;
import com.campushelp.entity.User;
import com.campushelp.repository.ComplaintRepository;
import com.campushelp.repository.RatingRepository;
import com.campushelp.repository.SameIssueRepository;
import com.campushelp.repository.StatusHistoryRepository;
import com.campushelp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    /** A complaint still open after this long counts as overdue. */
    public static final int OVERDUE_DAYS = 7;

    private final ComplaintRepository complaints;
    private final StatusHistoryRepository history;
    private final ComplaintService complaintService;
    private final UserRepository users;
    private final RatingRepository ratings;
    private final SameIssueRepository sameIssues;
    private final NotificationService notifications;
    private final PasswordEncoder passwordEncoder;

    public AdminService(ComplaintRepository complaints,
                        StatusHistoryRepository history,
                        ComplaintService complaintService,
                        UserRepository users,
                        RatingRepository ratings,
                        SameIssueRepository sameIssues,
                        NotificationService notifications,
                        PasswordEncoder passwordEncoder) {
        this.complaints = complaints;
        this.history = history;
        this.complaintService = complaintService;
        this.users = users;
        this.ratings = ratings;
        this.sameIssues = sameIssues;
        this.notifications = notifications;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> dashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = complaints.count();
        long resolved = complaints.countByStatus(ComplaintStatus.RESOLVED);
        stats.put("total", total);
        stats.put("resolved", resolved);
        stats.put("open", total - resolved);
        stats.put("overdue", overdueComplaints().size());
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ComplaintStatus s : ComplaintStatus.values()) {
            byStatus.put(s.name(), complaints.countByStatus(s));
        }
        stats.put("byStatus", byStatus);
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : complaints.countByCategory()) {
            byCategory.put((String) row[0], (Long) row[1]);
        }
        stats.put("byCategory", byCategory);
        Double avg = ratings.averageStars();
        stats.put("averageRating", avg == null ? null : Math.round(avg * 10.0) / 10.0);
        stats.put("ratingCount", ratings.count());

        // Average resolution time (hours) across resolved complaints.
        List<Complaint> resolvedList = complaints.findResolvedWithResolvedAt();
        Double avgHours = null;
        if (!resolvedList.isEmpty()) {
            double sum = 0;
            for (Complaint c : resolvedList) {
                sum += Duration.between(c.getCreatedAt(), c.getResolvedAt()).toMinutes() / 60.0;
            }
            avgHours = Math.round((sum / resolvedList.size()) * 10.0) / 10.0;
        }
        stats.put("averageResolutionHours", avgHours);
        stats.put("averageResolutionText", avgHours == null ? null : formatHours(avgHours));

        // Frequently reported problems: most-submitted subjects.
        List<Map<String, Object>> frequent = new ArrayList<>();
        for (Object[] row : complaints.countBySubject()) {
            if (frequent.size() >= 5) break;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("subject", row[0]);
            entry.put("count", row[1]);
            frequent.add(entry);
        }
        stats.put("frequentProblems", frequent);

        // Most "same problem" reports (students affected by one complaint).
        List<Map<String, Object>> mostReported = new ArrayList<>();
        for (Object[] row : sameIssues.mostReportedComplaintIds()) {
            if (mostReported.size() >= 5) break;
            complaints.findById((Long) row[0]).ifPresent(c -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("code", c.getCode());
                entry.put("subject", c.getSubject());
                entry.put("count", row[1]);
                mostReported.add(entry);
            });
        }
        stats.put("mostReported", mostReported);

        stats.put("recent", complaints.findAllByOrderByCreatedAtDesc().stream()
                .limit(5).map(complaintService::toResponse).toList());
        stats.put("overdueList", overdueComplaints().stream()
                .map(complaintService::toResponse).toList());
        return stats;
    }

    /** Open complaints older than OVERDUE_DAYS (reopened ones count from the reopen time). */
    public List<Complaint> overdueComplaints() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(OVERDUE_DAYS);
        List<Complaint> overdue = new ArrayList<>();
        for (Complaint c : complaints.findAllByOrderByCreatedAtDesc()) {
            if (c.getStatus() == ComplaintStatus.RESOLVED) continue;
            LocalDateTime openSince = c.getCreatedAt();
            if (c.getStatus() == ComplaintStatus.REOPENED) {
                // The clock restarts when a complaint is reopened.
                LocalDateTime reopenedAt = history
                        .findByComplaintIdOrderByChangedAtAsc(c.getId()).stream()
                        .filter(h -> h.getNewStatus() == ComplaintStatus.REOPENED)
                        .map(StatusHistory::getChangedAt)
                        .reduce((a, b) -> b).orElse(null);
                if (reopenedAt != null) openSince = reopenedAt;
            }
            if (openSince != null && openSince.isBefore(cutoff)) {
                overdue.add(c);
            }
        }
        return overdue;
    }

    private static String formatHours(double hours) {
        if (hours < 1) return Math.round(hours * 60) + " min";
        if (hours < 48) return hours + " hours";
        return Math.round(hours / 24.0 * 10.0) / 10.0 + " days";
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
            // Unassigning sends it back for review.
            if (complaint.getStatus() == ComplaintStatus.ASSIGNED) {
                changeStatus(complaint, ComplaintStatus.UNDER_REVIEW, null, "Unassigned — back for review");
            }
        } else {
            User staff = users.findById(staffId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found"));
            if (staff.getRole() != Role.STAFF) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not staff");
            }
            complaint.setAssignedTo(staff);
            // Assigning moves the complaint into the ASSIGNED step.
            if (complaint.getStatus() == ComplaintStatus.SUBMITTED
                    || complaint.getStatus() == ComplaintStatus.UNDER_REVIEW
                    || complaint.getStatus() == ComplaintStatus.REOPENED) {
                changeStatus(complaint, ComplaintStatus.ASSIGNED, null,
                        "Assigned to " + staff.getName());
            }
            notifications.notify(staff, complaint,
                    complaint.getCode() + " was assigned to you: " + complaint.getSubject());
        }
        complaints.save(complaint);
        return complaintService.toResponse(complaint);
    }

    /** Admin changes a complaint's triage priority. */
    @Transactional
    public ComplaintResponse setPriority(Long complaintId, Priority priority) {
        Complaint complaint = complaints.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        complaint.setPriority(priority);
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
    public ComplaintResponse updateStatus(Long complaintId, ComplaintStatus newStatus,
                                          User admin) {
        return updateStatus(complaintId, newStatus, null, admin);
    }

    @Transactional
    public ComplaintResponse updateStatus(Long complaintId, ComplaintStatus newStatus,
                                          String note, User admin) {
        Complaint complaint = complaints.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        ComplaintStatus oldStatus = complaint.getStatus();
        if (oldStatus == newStatus) {
            return complaintService.toResponse(complaint);
        }
        if (newStatus == ComplaintStatus.REOPENED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only the student can reopen a complaint, with a reason");
        }
        if (!oldStatus.allowedNext().contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot move a complaint from " + oldStatus + " to " + newStatus);
        }
        changeStatus(complaint, newStatus, admin, note);
        return complaintService.toResponse(complaint);
    }

    /** Applies a status change: updates the row, resolvedAt, history and the student's notification. */
    private void changeStatus(Complaint complaint, ComplaintStatus newStatus,
                              User actor, String note) {
        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setStatus(newStatus);
        if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
        } else if (oldStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(null);
        }
        complaints.save(complaint);
        String cleanNote = note == null ? null : note.trim();
        history.save(new StatusHistory(complaint, actor, oldStatus, newStatus,
                cleanNote == null || cleanNote.isEmpty() ? null : cleanNote));
        notifications.notify(complaint.getStudent(), complaint,
                complaint.getCode() + " moved from " + pretty(oldStatus)
                        + " to " + pretty(newStatus)
                        + (cleanNote == null ? "" : " — " + cleanNote));
    }

    private static String pretty(ComplaintStatus s) {
        return switch (s) {
            case SUBMITTED -> "Submitted";
            case UNDER_REVIEW -> "Under Review";
            case ASSIGNED -> "Assigned";
            case IN_PROGRESS -> "In Progress";
            case RESOLVED -> "Resolved";
            case REOPENED -> "Reopened";
        };
    }
}
