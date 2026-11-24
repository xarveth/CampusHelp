package com.campushelp.dto;

import java.util.List;

/** Result of the duplicate check: open complaints that look like the new one. */
public record DuplicateCheckResponse(
        List<DuplicateMatch> matches
) {
    public record DuplicateMatch(
            Long id,
            String code,
            String subject,
            String status,
            String category,
            long sameIssueCount,
            double similarity
    ) {}
}
