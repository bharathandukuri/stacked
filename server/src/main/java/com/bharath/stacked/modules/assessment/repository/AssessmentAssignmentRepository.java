package com.bharath.stacked.modules.assessment.repository;

import com.bharath.stacked.modules.assessment.enums.CandidateParticipationStatus;
import com.bharath.stacked.modules.assessment.model.AssessmentAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentAssignmentRepository extends MongoRepository<AssessmentAssignment, String> {

    Optional<AssessmentAssignment> findByAssessmentIdAndCandidateEmail(String assessmentId, String candidateEmail);

    Optional<AssessmentAssignment> findByAssessmentIdAndCandidateUserId(String assessmentId, String candidateUserId);

    List<AssessmentAssignment> findAllByAssessmentId(String assessmentId);

    Page<AssessmentAssignment> findAllByAssessmentId(String assessmentId, Pageable pageable);

    Page<AssessmentAssignment> findAllByAssessmentIdAndStatus(String assessmentId, CandidateParticipationStatus status, Pageable pageable);

    List<AssessmentAssignment> findAllByCandidateUserId(String candidateUserId);

    List<AssessmentAssignment> findAllByCandidateEmail(String candidateEmail);

    long countByAssessmentId(String assessmentId);

    long countByAssessmentIdAndStatus(String assessmentId, CandidateParticipationStatus status);

    boolean existsByAssessmentIdAndCandidateEmail(String assessmentId, String candidateEmail);
}

