package com.campushelp.controller;

import com.campushelp.dto.AddUpdateRequest;
import com.campushelp.dto.ComplaintResponse;
import com.campushelp.dto.CreateComplaintRequest;
import com.campushelp.dto.DuplicateCheckResponse;
import com.campushelp.dto.RateComplaintRequest;
import com.campushelp.dto.ReopenRequest;
import com.campushelp.entity.Priority;
import com.campushelp.entity.User;
import com.campushelp.service.ComplaintService;
import com.campushelp.service.DuplicateDetectionService;
import com.campushelp.service.PhotoStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final DuplicateDetectionService duplicates;
    private final PhotoStorageService photos;

    public ComplaintController(ComplaintService complaintService,
                               DuplicateDetectionService duplicates,
                               PhotoStorageService photos) {
        this.complaintService = complaintService;
        this.duplicates = duplicates;
        this.photos = photos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse submit(@AuthenticationPrincipal User student,
                                    @Valid @RequestBody CreateComplaintRequest req) {
        return complaintService.submit(student, req);
    }

    /** Submit with a photo: multipart form (subject, categoryId, description, priority, photo). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse submitWithPhoto(@AuthenticationPrincipal User student,
                                             @RequestParam String subject,
                                             @RequestParam Long categoryId,
                                             @RequestParam String description,
                                             @RequestParam(required = false) Priority priority,
                                             @RequestParam(required = false) MultipartFile photo) {
        CreateComplaintRequest req = new CreateComplaintRequest(
                subject == null ? "" : subject, categoryId,
                description == null ? "" : description, priority);
        if (req.subject().isBlank() || req.description().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Subject and description are required");
        }
        String photoUrl = photos.store(photo);
        return complaintService.submit(student, req, photoUrl);
    }

    /**
     * Duplicate check — call before submitting. Returns open complaints in the
     * same category whose subject/description look like the new one.
     */
    @GetMapping("/check-duplicate")
    public DuplicateCheckResponse checkDuplicate(@RequestParam Long categoryId,
                                                 @RequestParam String subject,
                                                 @RequestParam(required = false) String description) {
        return duplicates.check(categoryId, subject, description);
    }

    @GetMapping("/mine")
    public List<ComplaintResponse> mine(@AuthenticationPrincipal User student) {
        return complaintService.myComplaints(student);
    }

    @GetMapping("/{id}")
    public ComplaintResponse getOne(@PathVariable Long id,
                                    @AuthenticationPrincipal User requester) {
        return complaintService.getById(id, requester);
    }

    /** Reopen a resolved complaint (student owner only, reason required). */
    @PostMapping("/{id}/reopen")
    public ComplaintResponse reopen(@PathVariable Long id,
                                    @AuthenticationPrincipal User student,
                                    @Valid @RequestBody ReopenRequest req) {
        return complaintService.reopen(student, id, req);
    }

    /** "I have the same problem" — mark someone else's open complaint. */
    @PostMapping("/{id}/same-issue")
    public ComplaintResponse sameIssue(@PathVariable Long id,
                                       @AuthenticationPrincipal User student) {
        return complaintService.markSameIssue(student, id);
    }

    /** Admin/staff posts an update (comment) on a complaint. */
    @PostMapping("/{id}/updates")
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse addUpdate(@PathVariable Long id,
                                       @AuthenticationPrincipal User author,
                                       @Valid @RequestBody AddUpdateRequest req) {
        return complaintService.addUpdate(author, id, req);
    }

    /** Rate a resolved complaint (student owner only, once, 1–5 stars). */
    @PostMapping("/{id}/rating")
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse rate(@PathVariable Long id,
                                  @AuthenticationPrincipal User student,
                                  @Valid @RequestBody RateComplaintRequest req) {
        return complaintService.rateComplaint(student, id, req);
    }
}
