package com.campushelp.service;

import com.campushelp.dto.ComplaintResponse;
import com.campushelp.dto.CreateComplaintRequest;
import com.campushelp.entity.Category;
import com.campushelp.entity.Complaint;
import com.campushelp.entity.ComplaintStatus;
import com.campushelp.entity.StatusHistory;
import com.campushelp.entity.User;
import com.campushelp.repository.CategoryRepository;
import com.campushelp.repository.ComplaintRepository;
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

    public ComplaintService(ComplaintRepository complaints,
                            CategoryRepository categories,
                            StatusHistoryRepository history) {
        this.complaints = complaints;
        this.categories = categories;
        this.history = history;
    }

    @Transactional
    public ComplaintResponse submit(User student, CreateComplaintRequest req) {
        Category category = categories.findById(req.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category"));
        Complaint complaint = new Complaint();
        complaint.setStudent(student);
        complaint.setCategory(category);
        complaint.setSubject(req.subject().trim());
        complaint.setDescription(req.description().trim());
        complaint.setStatus(ComplaintStatus.PENDING);
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

    /** Ownership check: a student may only fetch their own complaint; admins may fetch any. */
    public ComplaintResponse getById(Long id, User requester) {
        Complaint complaint = complaints.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        if (requester.getRole().name().equals("STUDENT")
                && !complaint.getStudent().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own complaints");
        }
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
        return ComplaintResponse.of(complaint, entries);
    }
}
