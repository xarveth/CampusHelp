package com.campushelp.dto;

import com.campushelp.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin/staff status change. `note` is optional — shown in the timeline. */
public record UpdateStatusRequest(
        @NotNull ComplaintStatus status,
        @Size(max = 500) String note
) {
    public UpdateStatusRequest(ComplaintStatus status) {
        this(status, null);
    }
}
