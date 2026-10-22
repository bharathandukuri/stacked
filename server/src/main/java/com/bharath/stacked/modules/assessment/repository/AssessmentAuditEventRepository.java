package com.bharath.stacked.modules.assessment.repository;

import com.bharath.stacked.modules.assessment.enums.AssessmentAuditEventType;
import com.bharath.stacked.modules.assessment.model.AssessmentAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentAuditEventRepository extends MongoRepository<AssessmentAuditEvent, String> {

    List<AssessmentAuditEvent> findAllByAttemptIdOrderByTimestampAsc(String attemptId);

    Page<AssessmentAuditEvent> findAllByAttemptIdOrderByTimestampDesc(String attemptId, Pageable pageable);

    Page<AssessmentAuditEvent> findAllByAssessmentIdOrderByTimestampDesc(String assessmentId, Pageable pageable);

    long countByAssessmentIdAndEventType(String assessmentId, AssessmentAuditEventType eventType);

    long countByAttemptIdAndEventType(String attemptId, AssessmentAuditEventType eventType);

    List<AssessmentAuditEvent> findAllByAssessmentId(String assessmentId);
}

