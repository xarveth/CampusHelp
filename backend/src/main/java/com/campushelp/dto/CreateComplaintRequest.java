package com.campushelp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateComplaintRequest(
        @NotBlank @Size(max = 200) String subject,
        @NotNull Long categoryId,
        @NotBlank String description
) {}
