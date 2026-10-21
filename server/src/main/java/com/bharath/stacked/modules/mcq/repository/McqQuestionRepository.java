package com.bharath.stacked.modules.mcq.repository;

import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.model.McqQuestion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface McqQuestionRepository extends MongoRepository<McqQuestion, String> {

    Optional<McqQuestion> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByStatus(McqStatus status);

    long countByType(McqType type);

    long countByDifficulty(McqDifficulty difficulty);

    long countByTopicsContaining(String topic);

    @Query(value = "{ 'imageFileIds': ?0, '_id': { $ne: ?1 } }", exists = true)
    boolean isImageUsedByOtherQuestion(String fileId, String excludeQuestionId);
}
