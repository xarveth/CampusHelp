package com.campushelp.controller;

import com.campushelp.dto.ComplaintResponse;
import com.campushelp.dto.CreateComplaintRequest;
import com.campushelp.dto.RateComplaintRequest;
import com.campushelp.entity.User;
import com.campushelp.service.ComplaintService;
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
    private final PhotoStorageService photos;

    public ComplaintController(ComplaintService complaintService, PhotoStorageService photos) {
        this.complaintService = complaintService;
        this.photos = photos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse submit(@AuthenticationPrincipal User student,
                                    @Valid @RequestBody CreateComplaintRequest req) {
        return complaintService.submit(student, req);
    }

    /** Submit with a photo: multipart form (subject, categoryId, description, photo). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse submitWithPhoto(@AuthenticationPrincipal User student,
                                             @RequestParam String subject,
                                             @RequestParam Long categoryId,
                                             @RequestParam String description,
                                             @RequestParam(required = false) MultipartFile photo) {
        CreateComplaintRequest req = new CreateComplaintRequest(
                subject == null ? "" : subject, categoryId,
                description == null ? "" : description);
        if (req.subject().isBlank() || req.description().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Subject and description are required");
        }
        String photoUrl = photos.store(photo);
        return complaintService.submit(student, req, photoUrl);
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

    /** Rate a resolved complaint (student owner only, once, 1–5 stars). */
    @PostMapping("/{id}/rating")
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse rate(@PathVariable Long id,
                                  @AuthenticationPrincipal User student,
                                  @Valid @RequestBody RateComplaintRequest req) {
        return complaintService.rateComplaint(student, id, req);
    }
}
