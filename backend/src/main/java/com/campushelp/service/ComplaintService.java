package com.campushelp.service;

import com.campushelp.dto.AddUpdateRequest;
import com.campushelp.dto.ComplaintResponse;
import com.campushelp.dto.CreateComplaintRequest;
import com.campushelp.dto.RateComplaintRequest;
import com.campushelp.dto.ReopenRequest;
import com.campushelp.entity.Category;
import com.campushelp.entity.Complaint;
import com.campushelp.entity.ComplaintRating;
import com.campushelp.entity.ComplaintStatus;
import com.campushelp.entity.ComplaintUpdate;
import com.campushelp.entity.Role;
import com.campushelp.entity.SameIssue;
import com.campushelp.entity.StatusHistory;
import com.campushelp.entity.User;
import com.campushelp.repository.CategoryRepository;
import com.campushelp.repository.ComplaintRepository;
import com.campushelp.repository.ComplaintUpdateRepository;
import com.campushelp.repository.RatingRepository;
import com.campushelp.repository.SameIssueRepository;
import com.campushelp.repository.StatusHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintRepository complaints;
    private final CategoryRepository categories;
    private final StatusHistoryRepository history;
    private final RatingRepository ratings;
    private final ComplaintUpdateRepository updates;
    private final SameIssueRepository sameIssues;
    private final NotificationService notifications;

    public ComplaintService(ComplaintRepository complaints,
                            CategoryRepository categories,
                            StatusHistoryRepository history,
                            RatingRepository ratings,
                            ComplaintUpdateRepository updates,
                            SameIssueRepository sameIssues,
                            NotificationService notifications) {
        this.complaints = complaints;
        this.categories = categories;
        this.history = history;
        this.ratings = ratings;
        this.updates = updates;
        this.sameIssues = sameIssues;
        this.notifications = notifications;
    }

    @Transactional
    public ComplaintResponse submit(User student, CreateComplaintRequest req) {
        return submit(student, req, null);
    }

    /** Creates a complaint, optionally with a photo (photoUrl is the stored /api/uploads/... path). */
    @Transactional
    public ComplaintResponse submit(User student, CreateComplaintRequest req, String photoUrl) {
        Category category = categories.findById(req.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category"));
        Complaint complaint = new Complaint();
        complaint.setStudent(student);
        complaint.setCategory(category);
        complaint.setSubject(req.subject().trim());
        complaint.setDescription(req.description().trim());
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setPriority(req.effectivePriority());
        complaint.setPhotoUrl(photoUrl);
        // Temporary code; replaced with CH-<id> once the row has an id.
        complaint.setCode("CH-TEMP");
        complaint = complaints.save(complaint);
        complaint.setCode("CH-" + (1000 + complaint.getId()));
        complaint = complaints.save(complaint);
        history.save(new StatusHistory(complaint, null, null, ComplaintStatus.SUBMITTED));
        notifications.notifyAdmins(complaint,
                "New complaint " + complaint.getCode() + ": " + complaint.getSubject());
        return toResponse(complaint, student);
    }

    public List<ComplaintResponse> myComplaints(User student) {
        return complaints.findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream().map(c -> toResponse(c, student)).toList();
    }

    /**
     * Visibility: a student may only fetch their own complaint; a staff member
     * only complaints assigned to them; an admin may fetch any.
     */
    public ComplaintResponse getById(Long id, User requester) {
        Complaint complaint = complaints.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        if (requester.getRole() == Role.STUDENT
                && !complaint.getStudent().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own complaints");
        }
        if (requester.getRole() == Role.STAFF
                && (complaint.getAssignedTo() == null
                    || !complaint.getAssignedTo().getId().equals(requester.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This complaint is not assigned to you");
        }
        return toResponse(complaint, requester);
    }

    /** A student reopens their own resolved complaint, with a mandatory reason. */
    @Transactional
    public ComplaintResponse reopen(User student, Long complaintId, ReopenRequest req) {
        Complaint complaint = complaints.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        if (!complaint.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only reopen your own complaints");
        }
        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only a resolved complaint can be reopened");
        }
        complaint.setStatus(ComplaintStatus.REOPENED);
        complaint.setResolvedAt(null);
        complaints.save(complaint);
        history.save(new StatusHistory(complaint, student,
                ComplaintStatus.RESOLVED, ComplaintStatus.REOPENED,
                req.reason().trim()));
        notifications.notifyAdmins(complaint,
                complaint.getCode() + " was reopened: " + req.reason().trim());
        return toResponse(complaint, student);
    }

    /**
     * "I have the same problem" — a student marks someone else's open complaint
     * as affecting them too, instead of filing a duplicate.
     */
    @Transactional
    public ComplaintResponse markSameIssue(User student, Long complaintId) {
        Complaint complaint = complaints.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        if (complaint.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This is your own complaint");
        }
        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This complaint is already resolved");
        }
        if (sameIssues.existsByComplaintIdAndStudentId(complaintId, student.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already marked this complaint");
        }
        sameIssues.save(new SameIssue(complaint, student));
        return toResponse(complaint, student);
    }

    /** Admin/staff posts an update (comment) on a complaint they can access. */
    @Transactional
    public ComplaintResponse addUpdate(User author, Long complaintId, AddUpdateRequest req) {
        Complaint complaint = complaints.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        // Visibility check: staff must be assigned; students cannot post updates.
        if (author.getRole() == Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Students cannot post updates");
        }
        if (author.getRole() == Role.STAFF
                && (complaint.getAssignedTo() == null
                    || !complaint.getAssignedTo().getId().equals(author.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This complaint is not assigned to you");
        }
        updates.save(new ComplaintUpdate(complaint, author, req.message().trim()));
        notifications.notify(complaint.getStudent(), complaint,
                "Update on " + complaint.getCode() + ": " + req.message().trim());
        return toResponse(complaint, author);
    }

    /** Rate a resolved complaint (student owner only, once only, 1–5 stars). */
    @Transactional
    public ComplaintResponse rateComplaint(User student, Long complaintId, RateComplaintRequest req) {
        Complaint complaint = complaints.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        if (!complaint.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only rate your own complaints");
        }
        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only a resolved complaint can be rated");
        }
        if (ratings.existsByComplaintId(complaintId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This complaint is already rated");
        }
        String comment = req.comment() == null ? null : req.comment().trim();
        ratings.save(new ComplaintRating(complaint, student, req.stars(),
                comment == null || comment.isEmpty() ? null : comment));
        return toResponse(complaint, student);
    }

    public ComplaintResponse toResponse(Complaint complaint) {
        return toResponse(complaint, null);
    }

    public ComplaintResponse toResponse(Complaint complaint, User viewer) {
        List<ComplaintResponse.StatusHistoryResponse> entries = history
                .findByComplaintIdOrderByChangedAtAsc(complaint.getId())
                .stream()
                .map(h -> new ComplaintResponse.StatusHistoryResponse(
                        h.getOldStatus() == null ? null : h.getOldStatus().name(),
                        h.getNewStatus().name(),
                        h.getAdmin() == null ? "System" : h.getAdmin().getName(),
                        h.getNote(),
                        h.getChangedAt()))
                .toList();
        ComplaintResponse.RatingResponse rating = ratings.findByComplaintId(complaint.getId())
                .map(r -> new ComplaintResponse.RatingResponse(r.getStars(), r.getComment(), r.getCreatedAt()))
                .orElse(null);
        List<ComplaintResponse.UpdateResponse> updateList = updates
                .findByComplaintIdOrderByCreatedAtAsc(complaint.getId())
                .stream()
                .map(u -> new ComplaintResponse.UpdateResponse(
                        u.getId(), u.getAuthor().getName(), u.getAuthor().getRole().name(),
                        u.getMessage(), u.getCreatedAt()))
                .toList();
        long sameCount = sameIssues.countByComplaintId(complaint.getId());
        boolean iReported = viewer != null && viewer.getRole() == Role.STUDENT
                && sameIssues.existsByComplaintIdAndStudentId(complaint.getId(), viewer.getId());
        String duplicateOfCode = complaint.getDuplicateOf() == null
                ? null : complaint.getDuplicateOf().getCode();
        return ComplaintResponse.of(complaint, entries, rating, duplicateOfCode,
                sameCount, iReported, updateList);
    }
}
