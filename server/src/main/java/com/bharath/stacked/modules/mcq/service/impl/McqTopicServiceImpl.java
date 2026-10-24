package com.bharath.stacked.modules.mcq.service.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqTopicRequest;
import com.bharath.stacked.modules.mcq.dto.request.UpdateMcqTopicRequest;
import com.bharath.stacked.modules.mcq.dto.response.McqTopicResponse;
import com.bharath.stacked.modules.mcq.model.McqTopic;
import com.bharath.stacked.modules.mcq.repository.McqQuestionRepository;
import com.bharath.stacked.modules.mcq.repository.McqTopicRepository;
import com.bharath.stacked.modules.mcq.service.McqTopicService;
import com.bharath.stacked.common.cache.CacheInvalidationService;
import com.bharath.stacked.common.cache.CacheKeys;
import com.bharath.stacked.common.cache.StackedCacheService;
import com.bharath.stacked.config.cache.CacheProperties;
import tools.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class McqTopicServiceImpl implements McqTopicService {

    private final McqTopicRepository topicRepository;
    private final McqQuestionRepository questionRepository;
    private final MongoTemplate mongoTemplate;

    private final StackedCacheService cacheService;
    private final CacheProperties cacheProperties;
    private final CacheInvalidationService cacheInvalidationService;

    @org.springframework.beans.factory.annotation.Autowired
    public McqTopicServiceImpl(McqTopicRepository topicRepository,
                               McqQuestionRepository questionRepository,
                               MongoTemplate mongoTemplate,
                               @org.springframework.beans.factory.annotation.Autowired(required = false)
                               StackedCacheService cacheService,
                               @org.springframework.beans.factory.annotation.Autowired(required = false)
                               CacheProperties cacheProperties,
                               @org.springframework.beans.factory.annotation.Autowired(required = false)
                               CacheInvalidationService cacheInvalidationService) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.mongoTemplate = mongoTemplate;
        this.cacheService = cacheService;
        this.cacheProperties = cacheProperties;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Override
    @NonNull
    public List<McqTopicResponse> listTopics() {
        if (cacheService != null && cacheProperties != null) {
            return cacheService.getOrCompute(
                    CacheKeys.mcqTopics(),
                    new TypeReference<List<McqTopicResponse>>() {},
                    cacheProperties.metadataTtl(),
                    this::executeListTopics
            );
        }
        return executeListTopics();
    }

    private List<McqTopicResponse> executeListTopics() {
        return topicRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(McqTopicResponse::fromModel)
                .toList();
    }

    @Override
    @NonNull
    public McqTopicResponse getTopicById(@NonNull String id) {
        McqTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with ID: " + id));
        return McqTopicResponse.fromModel(topic);
    }

    @Override
    @NonNull
    public McqTopicResponse createTopic(@NonNull CreateMcqTopicRequest request) {
        String trimmedName = request.name().trim();
        String slug = slugify(trimmedName);

        if (topicRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException("A topic with name '" + trimmedName + "' already exists.");
        }

        if (topicRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
        }

        McqTopic topic = McqTopic.builder()
                .name(trimmedName)
                .slug(slug)
                .description(request.description() != null ? request.description().trim() : null)
                .questionCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        McqTopic saved = topicRepository.save(topic);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateMcqTopics();
        }
        log.info("Created MCQ topic: [{}] (id: {})", saved.getName(), saved.getId());
        return McqTopicResponse.fromModel(saved);
    }

    @Override
    @NonNull
    public McqTopicResponse updateTopic(@NonNull String id, @NonNull UpdateMcqTopicRequest request) {
        McqTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with ID: " + id));

        String trimmedName = request.name().trim();
        if (!topic.getName().equalsIgnoreCase(trimmedName) && topicRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException("A topic with name '" + trimmedName + "' already exists.");
        }

        topic.setName(trimmedName);
        if (request.description() != null) {
            topic.setDescription(request.description().trim());
        }
        topic.setUpdatedAt(Instant.now());

        McqTopic updated = topicRepository.save(topic);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateMcqTopics();
        }
        log.info("Updated MCQ topic: [{}] (id: {})", updated.getName(), updated.getId());
        return McqTopicResponse.fromModel(updated);
    }

    @Override
    public void deleteTopic(@NonNull String id) {
        McqTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with ID: " + id));

        long referencingCount = questionRepository.countByTopicsContaining(topic.getName());
        if (referencingCount > 0) {
            throw new BadRequestException("Cannot delete topic '" + topic.getName() + "' because it is referenced by "
                    + referencingCount + " question(s).");
        }

        topicRepository.delete(topic);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateMcqTopics();
        }
        log.info("Deleted MCQ topic: [{}] (id: {})", topic.getName(), id);
    }

    @Override
    public void incrementQuestionCounts(@NonNull List<String> topicNames) {
        if (topicNames.isEmpty())
            return;
        for (String topicName : topicNames) {
            Query query = new Query(
                    Criteria.where("name").regex("^" + java.util.regex.Pattern.quote(topicName) + "$", "i"));
            Update update = new Update().inc("questionCount", 1).set("updatedAt", Instant.now());
            mongoTemplate.updateMulti(query, update, McqTopic.class);
        }
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateMcqTopics();
        }
    }

    @Override
    public void decrementQuestionCounts(@NonNull List<String> topicNames) {
        if (topicNames.isEmpty())
            return;
        for (String topicName : topicNames) {
            Query query = new Query(new Criteria().andOperator(
                    Criteria.where("name").regex("^" + java.util.regex.Pattern.quote(topicName) + "$", "i"),
                    Criteria.where("questionCount").gt(0)));
            Update update = new Update().inc("questionCount", -1).set("updatedAt", Instant.now());
            mongoTemplate.updateMulti(query, update, McqTopic.class);
        }
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateMcqTopics();
        }
    }

    private String slugify(String input) {
        if (input == null || input.isBlank())
            return "topic";
        String normalized = input.toLowerCase(Locale.ROOT).trim();
        return normalized.replaceAll("[^a-z0-9\\s-]", "").replaceAll("[\\s-]+", "-");
    }
}
