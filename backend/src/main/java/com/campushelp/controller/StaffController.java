package com.campushelp.controller;

import com.campushelp.dto.ComplaintResponse;
import com.campushelp.dto.UpdateStatusRequest;
import com.campushelp.entity.User;
import com.campushelp.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Staff endpoints — a staff member only sees and works on assigned complaints. */
@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping("/complaints")
    public List<ComplaintResponse> assigned(@AuthenticationPrincipal User staff) {
        return staffService.assignedComplaints(staff);
    }

    @GetMapping("/complaints/{id}")
    public ComplaintResponse getOne(@PathVariable Long id,
                                    @AuthenticationPrincipal User staff) {
        return staffService.getAssigned(id, staff);
    }

    @PatchMapping("/complaints/{id}/status")
    public ComplaintResponse updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody UpdateStatusRequest req,
                                          @AuthenticationPrincipal User staff) {
        return staffService.updateStatus(id, req.status(), staff);
    }
}
