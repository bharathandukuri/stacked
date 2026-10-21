package com.bharath.stacked.modules.question_bank.repository;

import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import com.bharath.stacked.modules.question_bank.model.QuestionBank;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionBankRepository extends MongoRepository<QuestionBank, String> {

    Optional<QuestionBank> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByStatus(QuestionBankStatus status);

    List<QuestionBank> findAllByItemsQuestionId(String questionId);
}

