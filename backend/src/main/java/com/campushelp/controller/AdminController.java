package com.campushelp.controller;

import com.campushelp.dto.ComplaintResponse;
import com.campushelp.dto.UpdateStatusRequest;
import com.campushelp.entity.User;
import com.campushelp.service.AdminService;
import com.campushelp.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ComplaintService complaintService;

    public AdminController(AdminService adminService, ComplaintService complaintService) {
        this.adminService = adminService;
        this.complaintService = complaintService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return adminService.dashboardStats();
    }

    @GetMapping("/complaints")
    public List<ComplaintResponse> complaints(@RequestParam(required = false) String status,
                                              @RequestParam(required = false) String q) {
        return adminService.allComplaints(status, q);
    }

    @GetMapping("/complaints/{id}")
    public ComplaintResponse complaint(@PathVariable Long id,
                                       @AuthenticationPrincipal User admin) {
        // Admins may view any complaint; the ownership check is skipped for ADMIN.
        return complaintService.getById(id, admin);
    }

    @PatchMapping("/complaints/{id}/status")
    public ComplaintResponse updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody UpdateStatusRequest req,
                                          @AuthenticationPrincipal User admin) {
        return adminService.updateStatus(id, req.status(), admin);
    }
}
