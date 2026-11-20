package com.campushelp.dto;

import com.campushelp.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull ComplaintStatus status) {}
