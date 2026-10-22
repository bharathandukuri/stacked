package com.bharath.stacked.modules.assessment.enums;

import java.time.Instant;

public enum AssessmentStatus {
    NOT_STARTED,
    ACTIVE,
    COMPLETED;

    /**
     * Derives the authoritative effective lifecycle status of an assessment based on its schedule:
     *
     * Precedence:
     * 1. If endTime exists and now >= endTime -> COMPLETED
     * 2. Otherwise, if startTime exists and now < startTime -> NOT_STARTED
     * 3. Otherwise -> ACTIVE
     *
     * Boundaries:
     * - Start boundary is inclusive (now >= startTime -> ACTIVE).
     * - End boundary is exclusive for ACTIVE (now >= endTime -> COMPLETED).
     * - If startTime is null, assessment becomes ACTIVE immediately upon creation (unless now >= endTime).
     */
    public static AssessmentStatus deriveStatus(Instant startTime, Instant endTime, Instant now) {
        if (now == null) {
            now = Instant.now();
        }
        if (endTime != null && !now.isBefore(endTime)) {
            return COMPLETED;
        }
        if (startTime != null && now.isBefore(startTime)) {
            return NOT_STARTED;
        }
        return ACTIVE;
    }

    public static AssessmentStatus deriveStatus(Instant startTime, Instant endTime) {
        return deriveStatus(startTime, endTime, Instant.now());
    }
}
