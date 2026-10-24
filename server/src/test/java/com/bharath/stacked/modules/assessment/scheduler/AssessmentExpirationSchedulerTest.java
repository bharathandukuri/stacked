package com.bharath.stacked.modules.assessment.scheduler;

import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import com.bharath.stacked.modules.assessment.enums.CandidateParticipationStatus;
import com.bharath.stacked.modules.assessment.model.Assessment;
import com.bharath.stacked.modules.assessment.model.AssessmentAssignment;
import com.bharath.stacked.modules.assessment.model.AssessmentAttempt;
import com.bharath.stacked.modules.assessment.model.AssessmentQuestionResponse;
import com.bharath.stacked.modules.assessment.repository.AssessmentAssignmentRepository;
import com.bharath.stacked.modules.assessment.repository.AssessmentAttemptRepository;
import com.bharath.stacked.modules.assessment.repository.AssessmentQuestionResponseRepository;
import com.bharath.stacked.modules.assessment.repository.AssessmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentExpirationScheduler Unit Tests")
class AssessmentExpirationSchedulerTest {

    @Mock
    private AssessmentAttemptRepository attemptRepository;

    @Mock
    private AssessmentAssignmentRepository assignmentRepository;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private AssessmentQuestionResponseRepository responseRepository;

    @InjectMocks
    private AssessmentExpirationScheduler scheduler;

    @Test
    @DisplayName("sweepExpiredAttempts tallies candidate responses and updates statuses to EXPIRED")
    void sweepExpiredAttempts_TalliesScoreAndMarksExpired() {
        Instant deadline = Instant.now().minusSeconds(10);
        AssessmentAttempt attempt = AssessmentAttempt.builder()
                .id("att-1")
                .assessmentId("ass-1")
                .assignmentId("asg-1")
                .candidateUserId("cand-1")
                .status(AttemptStatus.IN_PROGRESS)
                .effectiveDeadline(deadline)
                .build();

        Assessment assessment = Assessment.builder()
                .id("ass-1")
                .passingScore(10.0)
                .build();

        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .id("asg-1")
                .status(CandidateParticipationStatus.IN_PROGRESS)
                .build();

        AssessmentQuestionResponse resp1 = AssessmentQuestionResponse.builder()
                .id("resp-1")
                .attemptId("att-1")
                .awardedPoints(5.0)
                .build();

        AssessmentQuestionResponse resp2 = AssessmentQuestionResponse.builder()
                .id("resp-2")
                .attemptId("att-1")
                .awardedPoints(6.0)
                .build();

        when(attemptRepository.findAllByStatusAndEffectiveDeadlineBefore(eq(AttemptStatus.IN_PROGRESS),
                any(Instant.class)))
                .thenReturn(List.of(attempt));
        when(responseRepository.findAllByAttemptId("att-1")).thenReturn(List.of(resp1, resp2));
        when(assessmentRepository.findById("ass-1")).thenReturn(Optional.of(assessment));
        when(assignmentRepository.findById("asg-1")).thenReturn(Optional.of(assignment));

        scheduler.sweepExpiredAttempts();

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(attempt.getScore()).isEqualTo(11.0);
        assertThat(attempt.getPassed()).isTrue();
        verify(attemptRepository).save(attempt);

        assertThat(assignment.getStatus()).isEqualTo(CandidateParticipationStatus.EXPIRED);
        verify(assignmentRepository).save(assignment);
    }

    @Test
    @DisplayName("syncAssessmentStatusProjections updates status when projected status changes")
    void syncAssessmentStatusProjections_UpdatesStatus() {
        Assessment assessment = Assessment.builder()
                .id("ass-1")
                .status(AssessmentStatus.NOT_STARTED)
                .scheduledStart(Instant.now().minusSeconds(100))
                .scheduledEnd(Instant.now().plusSeconds(1000))
                .build();

        when(assessmentRepository.findAll()).thenReturn(List.of(assessment));

        scheduler.syncAssessmentStatusProjections();

        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.ACTIVE);
        verify(assessmentRepository).save(assessment);
    }
}
