package com.campushelp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin/staff posts an update (comment) on a complaint. */
public record AddUpdateRequest(
        @NotBlank @Size(max = 2000) String message
) {}
