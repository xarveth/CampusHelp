package com.campushelp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Student reopens a resolved complaint — a reason is mandatory. */
public record ReopenRequest(
        @NotBlank @Size(max = 500) String reason
) {}
