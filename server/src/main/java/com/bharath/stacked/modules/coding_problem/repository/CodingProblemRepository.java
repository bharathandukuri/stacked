package com.bharath.stacked.modules.coding_problem.repository;

import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodingProblemRepository extends MongoRepository<CodingProblem, String> {
    Optional<CodingProblem> findBySlug(String slug);
}
