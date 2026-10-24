package com.bharath.stacked.modules.assessment.repository;

import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.model.Assessment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends MongoRepository<Assessment, String> {

    Optional<Assessment> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Assessment> findByInvitationCode(String invitationCode);

    boolean existsByInvitationCode(String invitationCode);

    long countByStatus(AssessmentStatus status);

    List<Assessment> findAllByItemsQuestionId(String questionId);

    boolean existsByItemsQuestionId(String questionId);

    List<Assessment> findAllByQuestionBankId(String questionBankId);

    boolean existsByQuestionBankId(String questionBankId);
}

