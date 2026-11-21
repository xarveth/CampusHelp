package com.campushelp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record RateComplaintRequest(
        @Min(1) @Max(5) int stars,
        @Size(max = 500) String comment
) {}
