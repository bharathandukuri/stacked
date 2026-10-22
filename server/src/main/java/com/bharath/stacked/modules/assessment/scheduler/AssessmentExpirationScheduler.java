package com.bharath.stacked.modules.assessment.scheduler;

import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import com.bharath.stacked.modules.assessment.enums.CandidateParticipationStatus;
import com.bharath.stacked.modules.assessment.model.Assessment;
import com.bharath.stacked.modules.assessment.model.AssessmentAttempt;
import com.bharath.stacked.modules.assessment.repository.AssessmentAssignmentRepository;
import com.bharath.stacked.modules.assessment.repository.AssessmentAttemptRepository;
import com.bharath.stacked.modules.assessment.repository.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssessmentExpirationScheduler {

    private final AssessmentAttemptRepository attemptRepository;
    private final AssessmentAssignmentRepository assignmentRepository;
    private final AssessmentRepository assessmentRepository;

    @Scheduled(fixedDelay = 60000) // Runs every minute
    @Transactional
    public void sweepExpiredAttempts() {
        Instant now = Instant.now();
        List<AssessmentAttempt> expired = attemptRepository.findAllByStatusAndEffectiveDeadlineBefore(
                AttemptStatus.IN_PROGRESS, now
        );

        if (expired.isEmpty()) {
            return;
        }

        log.info("Sweeping {} expired assessment attempt(s)", expired.size());
        for (AssessmentAttempt attempt : expired) {
            attempt.setStatus(AttemptStatus.EXPIRED);
            attempt.setUpdatedAt(now);
            attemptRepository.save(attempt);

            assignmentRepository.findById(attempt.getAssignmentId()).ifPresent(assignment -> {
                if (assignment.getStatus() == CandidateParticipationStatus.IN_PROGRESS) {
                    assignment.setStatus(CandidateParticipationStatus.EXPIRED);
                    assignment.setUpdatedAt(now);
                    assignmentRepository.save(assignment);
                }
            });
            log.info("Marked attempt [{}] as EXPIRED (deadline was {})", attempt.getId(), attempt.getEffectiveDeadline());
        }
    }

    @Scheduled(fixedDelay = 60000) // Runs every minute
    public void syncAssessmentStatusProjections() {
        Instant now = Instant.now();
        List<Assessment> assessments = assessmentRepository.findAll();
        for (Assessment assessment : assessments) {
            AssessmentStatus effective = assessment.getEffectiveStatus(now);
            if (assessment.getStatus() != effective) {
                AssessmentStatus old = assessment.getStatus();
                assessment.setStatus(effective);
                assessment.setUpdatedAt(now);
                assessmentRepository.save(assessment);
                log.info("Projected status updated for Assessment [{}] ({} -> {})", assessment.getId(), old, effective);
            }
        }
    }
}

