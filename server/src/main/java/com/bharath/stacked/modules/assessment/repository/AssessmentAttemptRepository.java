package com.bharath.stacked.modules.assessment.repository;

import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import com.bharath.stacked.modules.assessment.model.AssessmentAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentAttemptRepository extends MongoRepository<AssessmentAttempt, String> {

    Optional<AssessmentAttempt> findByAssessmentIdAndCandidateUserIdAndStatus(
            String assessmentId,
            String candidateUserId,
            AttemptStatus status
    );

    List<AssessmentAttempt> findAllByAssessmentIdAndCandidateUserId(String assessmentId, String candidateUserId);

    List<AssessmentAttempt> findAllByAssessmentId(String assessmentId);

    Page<AssessmentAttempt> findAllByAssessmentId(String assessmentId, Pageable pageable);

    long countByAssessmentIdAndCandidateUserId(String assessmentId, String candidateUserId);

    long countByAssessmentId(String assessmentId);

    long countByAssessmentIdAndStatus(String assessmentId, AttemptStatus status);

    List<AssessmentAttempt> findAllByAssessmentIdAndStatus(String assessmentId, AttemptStatus status);

    List<AssessmentAttempt> findAllByStatusAndEffectiveDeadlineBefore(AttemptStatus status, Instant now);
}

