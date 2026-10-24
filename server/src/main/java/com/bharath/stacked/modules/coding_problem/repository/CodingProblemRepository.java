package com.bharath.stacked.modules.coding_problem.repository;

import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodingProblemRepository extends MongoRepository<CodingProblem, String> {
    Optional<CodingProblem> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByStatus(CodingProblemStatus status);

    @Query(value = "{ 'imageFileIds': ?0, '_id': { $ne: ?1 } }", exists = true)
    boolean isImageUsedByOtherProblem(String fileId, String excludeProblemId);
}
