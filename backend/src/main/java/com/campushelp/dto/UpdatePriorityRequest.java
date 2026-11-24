package com.campushelp.dto;

import com.campushelp.entity.Priority;
import jakarta.validation.constraints.NotNull;

/** Admin changes a complaint's triage priority. */
public record UpdatePriorityRequest(
        @NotNull Priority priority
) {}
