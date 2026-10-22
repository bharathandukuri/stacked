package com.bharath.stacked.modules.assessment.repository;

import com.bharath.stacked.modules.assessment.model.AssessmentConnectionEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentConnectionEventRepository extends MongoRepository<AssessmentConnectionEvent, String> {

    List<AssessmentConnectionEvent> findAllByAttemptIdOrderByTimestampDesc(String attemptId);

    Page<AssessmentConnectionEvent> findAllByAttemptIdOrderByTimestampDesc(String attemptId, Pageable pageable);

    long countByAttemptIdAndIsIntegrityFlagTrue(String attemptId);
}

