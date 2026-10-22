package com.bharath.stacked.modules.assessment.repository;

import com.bharath.stacked.modules.assessment.model.AssessmentQuestionResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentQuestionResponseRepository extends MongoRepository<AssessmentQuestionResponse, String> {

    Optional<AssessmentQuestionResponse> findByAttemptIdAndQuestionId(String attemptId, String questionId);

    List<AssessmentQuestionResponse> findAllByAttemptId(String attemptId);

    List<AssessmentQuestionResponse> findAllByQuestionId(String questionId);

    long countByAttemptId(String attemptId);
}

