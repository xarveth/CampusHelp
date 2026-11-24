package com.campushelp.dto;

import com.campushelp.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateComplaintRequest(
        @NotBlank @Size(max = 200) String subject,
        @NotNull Long categoryId,
        @NotBlank String description,
        Priority priority
) {
    /** Priority is optional at submit — defaults to MEDIUM. */
    public CreateComplaintRequest(String subject, Long categoryId, String description) {
        this(subject, categoryId, description, null);
    }

    public Priority effectivePriority() {
        return priority == null ? Priority.MEDIUM : priority;
    }
}
