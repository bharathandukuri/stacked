package com.bharath.stacked.modules.mcq.repository;

import com.bharath.stacked.modules.mcq.model.McqTopic;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface McqTopicRepository extends MongoRepository<McqTopic, String> {

    Optional<McqTopic> findBySlug(String slug);

    Optional<McqTopic> findByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);
}
