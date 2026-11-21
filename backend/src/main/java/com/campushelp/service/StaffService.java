package com.campushelp.service;

import com.campushelp.dto.ComplaintResponse;
import com.campushelp.entity.ComplaintStatus;
import com.campushelp.entity.User;
import com.campushelp.repository.ComplaintRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Staff work: only the complaints assigned to the logged-in staff member. */
@Service
public class StaffService {

    private final ComplaintRepository complaints;
    private final ComplaintService complaintService;
    private final AdminService adminService;

    public StaffService(ComplaintRepository complaints,
                        ComplaintService complaintService,
                        AdminService adminService) {
        this.complaints = complaints;
        this.complaintService = complaintService;
        this.adminService = adminService;
    }

    public List<ComplaintResponse> assignedComplaints(User staff) {
        return complaints.findByAssignedToIdOrderByCreatedAtDesc(staff.getId())
                .stream().map(complaintService::toResponse).toList();
    }

    /** A staff member may only open complaints assigned to them. */
    public ComplaintResponse getAssigned(Long id, User staff) {
        return complaintService.getById(id, staff);
    }

    /** A staff member updates the status of an assigned complaint (same rules as admin). */
    public ComplaintResponse updateStatus(Long id, ComplaintStatus newStatus, User staff) {
        complaintService.getById(id, staff); // throws 403 unless assigned to this staff member
        return adminService.updateStatus(id, newStatus, staff);
    }
}
