package com.campushelp.service;

import com.campushelp.dto.ComplaintResponse;
import com.campushelp.dto.CreateComplaintRequest;
import com.campushelp.dto.RateComplaintRequest;
import com.campushelp.entity.Category;
import com.campushelp.entity.Complaint;
import com.campushelp.entity.ComplaintRating;
import com.campushelp.entity.ComplaintStatus;
import com.campushelp.entity.Role;
import com.campushelp.entity.StatusHistory;
import com.campushelp.entity.User;
import com.campushelp.repository.CategoryRepository;
import com.campushelp.repository.ComplaintRepository;
import com.campushelp.repository.RatingRepository;
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

    public ComplaintService(ComplaintRepository complaints,
                            CategoryRepository categories,
                            StatusHistoryRepository history,
                            RatingRepository ratings) {
        this.complaints = complaints;
        this.categories = categories;
        this.history = history;
        this.ratings = ratings;
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
        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setPhotoUrl(photoUrl);
        // Temporary code; replaced with CH-<id> once the row has an id.
        complaint.setCode("CH-TEMP");
        complaint = complaints.save(complaint);
        complaint.setCode("CH-" + (1000 + complaint.getId()));
        complaint = complaints.save(complaint);
        history.save(new StatusHistory(complaint, null, null, ComplaintStatus.PENDING));
        return toResponse(complaint);
    }

    public List<ComplaintResponse> myComplaints(User student) {
        return complaints.findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream().map(this::toResponse).toList();
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
        return toResponse(complaint);
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
        return toResponse(complaint);
    }

    public ComplaintResponse toResponse(Complaint complaint) {
        List<ComplaintResponse.StatusHistoryResponse> entries = history
                .findByComplaintIdOrderByChangedAtAsc(complaint.getId())
                .stream()
                .map(h -> new ComplaintResponse.StatusHistoryResponse(
                        h.getOldStatus() == null ? null : h.getOldStatus().name(),
                        h.getNewStatus().name(),
                        h.getAdmin() == null ? "System" : h.getAdmin().getName(),
                        h.getChangedAt()))
                .toList();
        ComplaintResponse.RatingResponse rating = ratings.findByComplaintId(complaint.getId())
                .map(r -> new ComplaintResponse.RatingResponse(r.getStars(), r.getComment(), r.getCreatedAt()))
                .orElse(null);
        return ComplaintResponse.of(complaint, entries, rating);
    }
}
