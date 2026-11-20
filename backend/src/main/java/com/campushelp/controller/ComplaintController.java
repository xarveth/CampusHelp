package com.campushelp.controller;

import com.campushelp.dto.ComplaintResponse;
import com.campushelp.dto.CreateComplaintRequest;
import com.campushelp.entity.User;
import com.campushelp.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse submit(@AuthenticationPrincipal User student,
                                    @Valid @RequestBody CreateComplaintRequest req) {
        return complaintService.submit(student, req);
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
}
