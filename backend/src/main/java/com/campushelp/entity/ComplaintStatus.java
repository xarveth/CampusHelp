package com.campushelp.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Detailed complaint lifecycle. A complaint moves forward through review and
 * assignment; only a student can reopen a resolved complaint (via the reopen
 * endpoint, with a reason), never through a plain status update.
 *
 *   SUBMITTED → UNDER_REVIEW → ASSIGNED → IN_PROGRESS → RESOLVED
 *   RESOLVED → REOPENED → UNDER_REVIEW / ASSIGNED / IN_PROGRESS / RESOLVED
 */
public enum ComplaintStatus {
    SUBMITTED,
    UNDER_REVIEW,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    REOPENED;

    /** True for every status that still needs work (used for overdue counts). */
    public boolean isOpen() {
        return this != RESOLVED;
    }

    /**
     * Statuses an admin/staff member may move TO from this status through a
     * plain status update. REOPENED is deliberately absent — reopening is a
     * student action with a mandatory reason (see ComplaintService.reopen).
     */
    public Set<ComplaintStatus> allowedNext() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(UNDER_REVIEW, RESOLVED);
            case UNDER_REVIEW -> EnumSet.of(ASSIGNED, IN_PROGRESS, RESOLVED);
            case ASSIGNED -> EnumSet.of(IN_PROGRESS, RESOLVED, UNDER_REVIEW);
            case IN_PROGRESS -> EnumSet.of(RESOLVED, ASSIGNED, UNDER_REVIEW);
            case RESOLVED -> EnumSet.noneOf(ComplaintStatus.class);
            case REOPENED -> EnumSet.of(UNDER_REVIEW, ASSIGNED, IN_PROGRESS, RESOLVED);
        };
    }
}
