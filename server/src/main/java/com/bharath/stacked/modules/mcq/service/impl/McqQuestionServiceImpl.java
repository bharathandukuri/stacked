package com.bharath.stacked.modules.mcq.service.impl;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.common.cache.CacheInvalidationService;
import com.bharath.stacked.common.cache.CacheKeys;
import com.bharath.stacked.common.cache.StackedCacheService;
import com.bharath.stacked.config.cache.CacheProperties;
import tools.jackson.core.type.TypeReference;
import com.bharath.stacked.common.event.QuestionDeletedEvent;
import com.bharath.stacked.modules.analytics.enums.AnalyticsActorType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEntityType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.assessment.model.Assessment;
import com.bharath.stacked.modules.assessment.repository.AssessmentRepository;
import com.bharath.stacked.modules.file.service.FileService;
import com.bharath.stacked.modules.mcq.dto.request.CreateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.request.McqFilterRequest;
import com.bharath.stacked.modules.mcq.dto.request.McqOptionRequest;
import com.bharath.stacked.modules.mcq.dto.request.UpdateMcqRequest;
import com.bharath.stacked.modules.mcq.dto.response.CandidateMcqResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionDetailResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqQuestionSummaryResponse;
import com.bharath.stacked.modules.mcq.dto.response.McqStatsResponse;
import com.bharath.stacked.modules.mcq.enums.McqDifficulty;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.model.McqOption;
import com.bharath.stacked.modules.mcq.model.McqQuestion;
import com.bharath.stacked.modules.mcq.repository.McqQuestionRepository;
import com.bharath.stacked.modules.mcq.repository.McqTopicRepository;
import com.bharath.stacked.modules.mcq.service.McqQuestionService;
import com.bharath.stacked.modules.mcq.service.McqTopicService;
import com.bharath.stacked.modules.assessment.repository.AssessmentQuestionResponseRepository;
import com.bharath.stacked.modules.question_bank.model.QuestionBank;
import com.bharath.stacked.modules.question_bank.repository.QuestionBankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.HashMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class McqQuestionServiceImpl implements McqQuestionService {

    private static final Pattern MARKDOWN_IMAGE_PATTERN =
            Pattern.compile("!\\[.*?\\]\\(/api/files/([a-f0-9\\-]+)/content\\)");

    private final McqQuestionRepository questionRepository;
    private final McqTopicRepository topicRepository;
    private final McqTopicService topicService;
    private final FileService fileService;
    private final MongoTemplate mongoTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final QuestionBankRepository questionBankRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionResponseRepository assessmentQuestionResponseRepository;

    private final StackedCacheService cacheService;
    private final CacheProperties cacheProperties;
    private final CacheInvalidationService cacheInvalidationService;

    @org.springframework.beans.factory.annotation.Autowired
    public McqQuestionServiceImpl(McqQuestionRepository questionRepository,
                                  McqTopicRepository topicRepository,
                                  McqTopicService topicService,
                                  FileService fileService,
                                  MongoTemplate mongoTemplate,
                                  ApplicationEventPublisher eventPublisher,
                                  QuestionBankRepository questionBankRepository,
                                  AssessmentRepository assessmentRepository,
                                  AssessmentQuestionResponseRepository assessmentQuestionResponseRepository,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false)
                                  StackedCacheService cacheService,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false)
                                  CacheProperties cacheProperties,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false)
                                  CacheInvalidationService cacheInvalidationService) {
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.topicService = topicService;
        this.fileService = fileService;
        this.mongoTemplate = mongoTemplate;
        this.eventPublisher = eventPublisher;
        this.questionBankRepository = questionBankRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionResponseRepository = assessmentQuestionResponseRepository;
        this.cacheService = cacheService;
        this.cacheProperties = cacheProperties;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Override
    @NonNull
    public PageResponse<McqQuestionSummaryResponse> listQuestions(@Nullable McqFilterRequest filter) {
        if (filter == null) {
            filter = new McqFilterRequest();
        }

        if (filter.isDefault() && cacheService != null && cacheProperties != null) {
            final McqFilterRequest effectiveFilter = filter;
            return cacheService.getOrCompute(
                    CacheKeys.mcqDefaultPage(),
                    new TypeReference<PageResponse<McqQuestionSummaryResponse>>() {},
                    cacheProperties.catalogPageTtl(),
                    () -> executeListQuestions(effectiveFilter)
            );
        }

        return executeListQuestions(filter);
    }

    private PageResponse<McqQuestionSummaryResponse> executeListQuestions(McqFilterRequest filter) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (filter.getStatus() != null) {
            criteriaList.add(Criteria.where("status").is(filter.getStatus()));
        }

        if (filter.getType() != null) {
            criteriaList.add(Criteria.where("type").is(filter.getType()));
        }

        if (filter.getDifficulty() != null) {
            criteriaList.add(Criteria.where("difficulty").is(filter.getDifficulty()));
        }

        if (filter.getTopic() != null && !filter.getTopic().trim().isEmpty()) {
            String topicRegex = "^" + Pattern.quote(filter.getTopic().trim()) + "$";
            criteriaList.add(Criteria.where("topics").regex(topicRegex, "i"));
        }

        if (filter.getTag() != null && !filter.getTag().trim().isEmpty()) {
            String tagRegex = "^" + Pattern.quote(filter.getTag().trim()) + "$";
            criteriaList.add(Criteria.where("tags").regex(tagRegex, "i"));
        }

        if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
            String sanitizedSearch = Pattern.quote(filter.getSearch().trim());
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(sanitizedSearch, "i"),
                    Criteria.where("content").regex(sanitizedSearch, "i")
            ));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long totalElements = mongoTemplate.count(query, McqQuestion.class);

        int page = Math.max(0, filter.getPage());
        int size = filter.getSize() <= 0 ? 20 : Math.min(filter.getSize(), 100);

        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        query.with(Sort.by(direction, sortBy));
        query.skip((long) page * size);
        query.limit(size);

        List<McqQuestion> questions = mongoTemplate.find(query, McqQuestion.class);
        List<McqQuestionSummaryResponse> summaries = questions.stream()
                .map(McqQuestionSummaryResponse::fromModel)
                .toList();

        return PageResponse.of(summaries, page, size, totalElements);
    }

    @Override
    @NonNull
    public McqQuestionDetailResponse getQuestionById(@NonNull String id) {
        if (cacheService != null && cacheProperties != null) {
            return cacheService.getOrCompute(
                    CacheKeys.mcqDetail(id),
                    McqQuestionDetailResponse.class,
                    cacheProperties.detailsTtl(),
                    () -> executeGetQuestionById(id)
            );
        }
        return executeGetQuestionById(id);
    }

    private McqQuestionDetailResponse executeGetQuestionById(String id) {
        McqQuestion question = findEntityById(id);
        return McqQuestionDetailResponse.fromModel(question);
    }

    @Override
    @NonNull
    public CandidateMcqResponse getCandidateQuestionById(@NonNull String id, boolean shuffle) {
        McqQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with ID: " + id));

        if (question.getStatus() != McqStatus.PUBLISHED) {
            throw new BadRequestException("Question is not currently published.");
        }

        return CandidateMcqResponse.fromModel(question, shuffle);
    }

    @Override
    @NonNull
    public McqQuestionDetailResponse createQuestion(@NonNull CreateMcqRequest request, @Nullable String authorId) {
        ValidatedOptions validated = validateOptionsAndAnswers(request.type(), request.options(), request.correctOptionIds());

        String title = request.title().trim();

        Set<String> imageIds = extractAndPromoteImages(request.content(), request.imageFileIds());

        List<String> sanitizedTopics = sanitizeList(request.topics());
        List<String> sanitizedTags = sanitizeList(request.tags());

        McqQuestion question = McqQuestion.builder()
                .title(title)
                .content(request.content().trim())
                .type(request.type())
                .difficulty(request.difficulty())
                .status(request.status() != null ? request.status() : McqStatus.DRAFT)
                .topics(sanitizedTopics)
                .tags(sanitizedTags)
                .options(validated.options())
                .correctOptionIds(validated.correctOptionIds())
                .explanation(request.explanation() != null ? request.explanation().trim() : null)
                .shuffleOptions(request.shuffleOptions() == null || request.shuffleOptions())
                .imageFileIds(new ArrayList<>(imageIds))
                .authorId(authorId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        McqQuestion saved = questionRepository.save(question);
        log.info("Created MCQ question: [{}] (id: {})", saved.getTitle(), saved.getId());

        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateMcq(saved.getId());
        }

        publishAnalytics(AnalyticsEventType.MCQ_CREATED, saved.getId(), mcqAttributes(saved));

        topicService.incrementQuestionCounts(saved.getTopics());

        return McqQuestionDetailResponse.fromModel(saved);
    }

    @Override
    @NonNull
    public McqQuestionDetailResponse updateQuestion(@NonNull String id, @NonNull UpdateMcqRequest request) {
        McqQuestion existing = findEntityById(id);
        if (existing.getStatus() != McqStatus.DRAFT) {
            throw new ConflictException("Cannot edit MCQ question '" + existing.getTitle() + "' because it is currently " + existing.getStatus() + ". Unpublish to DRAFT first.");
        }
        String questionId = Objects.requireNonNull(existing.getId());

        ValidatedOptions validated = validateOptionsAndAnswers(request.type(), request.options(), request.correctOptionIds());

        String title = request.title().trim();

        Set<String> newImageIds = extractAndPromoteImages(request.content(), request.imageFileIds());
        cleanRemovedImages(new HashSet<>(existing.getImageFileIds()), newImageIds, questionId);

        List<String> oldTopics = existing.getTopics() != null ? existing.getTopics() : List.of();
        List<String> newTopics = sanitizeList(request.topics());

        updateTopicCountsDiff(oldTopics, newTopics);

        existing.setTitle(title);
        existing.setContent(request.content().trim());
        existing.setType(request.type());
        existing.setDifficulty(request.difficulty());
        // Enforce draft-only editing: status remains DRAFT and cannot be changed via update
        existing.setStatus(McqStatus.DRAFT);
        existing.setTopics(newTopics);
        existing.setTags(sanitizeList(request.tags()));
        existing.setOptions(validated.options());
        existing.setCorrectOptionIds(validated.correctOptionIds());
        existing.setExplanation(request.explanation() != null ? request.explanation().trim() : null);
        existing.setShuffleOptions(request.shuffleOptions() == null || request.shuffleOptions());
        existing.setImageFileIds(new ArrayList<>(newImageIds));
        existing.setUpdatedAt(Instant.now());

        McqQuestion updated = questionRepository.save(existing);
        log.info("Updated MCQ question: [{}] (id: {})", updated.getTitle(), updated.getId());

        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateMcq(updated.getId());
        }

        publishAnalytics(AnalyticsEventType.MCQ_UPDATED, updated.getId(), mcqAttributes(updated));

        return McqQuestionDetailResponse.fromModel(updated);
    }

    @Override
    public boolean isInUse(@NonNull String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }
        return questionBankRepository.existsByItemsQuestionId(id)
                || assessmentRepository.existsByItemsQuestionId(id)
                || assessmentQuestionResponseRepository.existsByQuestionId(id);
    }

    @Override
    public void deleteQuestion(@NonNull String id) {
        McqQuestion question = findEntityById(id);
        String questionId = Objects.requireNonNull(question.getId());

        if (isInUse(questionId)) {
            List<QuestionBank> banks = questionBankRepository.findAllByItemsQuestionId(questionId);
            List<Assessment> assessments = assessmentRepository.findAllByItemsQuestionId(questionId);
            List<String> usages = new ArrayList<>();
            if (!banks.isEmpty()) {
                usages.add(banks.size() + " question bank(s) [" + banks.stream().map(QuestionBank::getTitle).collect(Collectors.joining(", ")) + "]");
            }
            if (!assessments.isEmpty()) {
                usages.add(assessments.size() + " assessment(s) [" + assessments.stream().map(Assessment::getTitle).collect(Collectors.joining(", ")) + "]");
            }
            if (usages.isEmpty()) {
                usages.add("historical assessment attempt responses");
            }
            throw new ConflictException("Cannot delete MCQ question '" + question.getTitle() + "' because it is currently in use by: " + String.join("; ", usages) + ".");
        }

        if (question.getImageFileIds() != null && !question.getImageFileIds().isEmpty()) {
            cleanRemovedImages(new HashSet<>(question.getImageFileIds()), Set.of(), questionId);
        }

        if (question.getTopics() != null && !question.getTopics().isEmpty()) {
            topicService.decrementQuestionCounts(question.getTopics());
        }

        questionRepository.delete(question);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateMcq(questionId);
        }
        eventPublisher.publishEvent(new QuestionDeletedEvent(questionId));
        publishAnalytics(AnalyticsEventType.MCQ_DELETED, questionId, mcqAttributes(question));
        log.info("Deleted MCQ question: [{}] (id: {})", question.getTitle(), questionId);
    }

    @Override
    @NonNull
    public McqQuestionDetailResponse updateStatus(@NonNull String id, @NonNull McqStatus status) {
        McqQuestion question = findEntityById(id);
        McqStatus currentStatus = question.getStatus();

        if (currentStatus == status) {
            return McqQuestionDetailResponse.fromModel(question);
        }

        if (status == McqStatus.DRAFT && currentStatus == McqStatus.PUBLISHED) {
            if (isInUse(question.getId())) {
                List<QuestionBank> banks = questionBankRepository.findAllByItemsQuestionId(question.getId());
                List<Assessment> assessments = assessmentRepository.findAllByItemsQuestionId(question.getId());
                List<String> usages = new ArrayList<>();
                if (!banks.isEmpty()) {
                    usages.add(banks.size() + " question bank(s) [" + banks.stream().map(QuestionBank::getTitle).collect(Collectors.joining(", ")) + "]");
                }
                if (!assessments.isEmpty()) {
                    usages.add(assessments.size() + " assessment(s) [" + assessments.stream().map(Assessment::getTitle).collect(Collectors.joining(", ")) + "]");
                }
                if (usages.isEmpty()) {
                    usages.add("historical assessment attempt responses");
                }
                throw new ConflictException("Cannot move MCQ question '" + question.getTitle() + "' back to draft because it is currently in use by: " + String.join("; ", usages) + ".");
            }
        }

        if (status == McqStatus.PUBLISHED) {
            if (question.getTitle() == null || question.getTitle().isBlank()) {
                throw new BadRequestException("Cannot publish MCQ question without a title.");
            }
            if (question.getContent() == null || question.getContent().isBlank()) {
                throw new BadRequestException("Cannot publish MCQ question without question content.");
            }
            if (question.getOptions() == null || question.getOptions().size() < 2) {
                throw new BadRequestException("Cannot publish MCQ question without at least two options.");
            }
            if (question.getCorrectOptionIds() == null || question.getCorrectOptionIds().isEmpty()) {
                throw new BadRequestException("Cannot publish MCQ question without at least one correct option.");
            }
        }

        question.setStatus(status);
        question.setUpdatedAt(Instant.now());
        McqQuestion saved = questionRepository.save(question);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateMcq(saved.getId());
        }
        log.info("Updated MCQ status: [{}] -> {} (id: {})", question.getTitle(), status, question.getId());

        AnalyticsEventType eventType = saved.getStatus() == McqStatus.PUBLISHED
                ? AnalyticsEventType.MCQ_PUBLISHED
                : AnalyticsEventType.MCQ_UNPUBLISHED;
        publishAnalytics(eventType, saved.getId(), mcqAttributes(saved));

        return McqQuestionDetailResponse.fromModel(saved);
    }

    @Override
    @NonNull
    public McqQuestionDetailResponse publish(@NonNull String id) {
        return updateStatus(id, McqStatus.PUBLISHED);
    }

    @Override
    @NonNull
    public McqQuestionDetailResponse unpublish(@NonNull String id) {
        return updateStatus(id, McqStatus.DRAFT);
    }

    @Override
    @NonNull
    public McqStatsResponse getStats() {
        if (cacheService != null && cacheProperties != null) {
            return cacheService.getOrCompute(
                    CacheKeys.mcqStats(),
                    McqStatsResponse.class,
                    cacheProperties.metadataTtl(),
                    this::executeGetStats
            );
        }
        return executeGetStats();
    }

    private McqStatsResponse executeGetStats() {
        long total = questionRepository.count();
        long published = questionRepository.countByStatus(McqStatus.PUBLISHED);
        long draft = questionRepository.countByStatus(McqStatus.DRAFT);

        long single = questionRepository.countByType(McqType.SINGLE_CHOICE);
        long multi = questionRepository.countByType(McqType.MULTIPLE_CHOICE);
        long boolType = questionRepository.countByType(McqType.BOOLEAN);

        long easy = questionRepository.countByDifficulty(McqDifficulty.EASY);
        long medium = questionRepository.countByDifficulty(McqDifficulty.MEDIUM);
        long hard = questionRepository.countByDifficulty(McqDifficulty.HARD);

        long topicsCount = topicRepository.count();

        return new McqStatsResponse(
                total,
                published,
                draft,
                single,
                multi,
                boolType,
                easy,
                medium,
                hard,
                topicsCount
        );
    }

    private McqQuestion findEntityById(String id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MCQ Question not found: " + id));
    }

    private ValidatedOptions validateOptionsAndAnswers(
            McqType type,
            List<McqOptionRequest> optionRequests,
            List<String> correctOptionIds) {

        if (optionRequests == null || optionRequests.size() < 2) {
            throw new BadRequestException("An MCQ question requires at least two options.");
        }

        if (correctOptionIds == null || correctOptionIds.isEmpty()) {
            throw new BadRequestException("At least one correct option must be specified.");
        }

        List<McqOption> options = new ArrayList<>();
        Set<String> optionIdSet = new HashSet<>();
        int index = 1;

        for (McqOptionRequest req : optionRequests) {
            String optId = (req.id() != null && !req.id().isBlank())
                    ? req.id().trim()
                    : "opt_" + index;

            if (!optionIdSet.add(optId)) {
                optId = "opt_" + UUID.randomUUID().toString().substring(0, 8);
                optionIdSet.add(optId);
            }

            options.add(new McqOption(optId, req.text().trim(), req.explanation() != null ? req.explanation().trim() : null));
            index++;
        }

        List<String> normalizedCorrectIds = new ArrayList<>();
        for (String cId : correctOptionIds) {
            if (cId == null || cId.isBlank()) continue;
            String trimmed = cId.trim();
            if (!optionIdSet.contains(trimmed)) {
                throw new BadRequestException("Correct option ID '" + trimmed + "' does not match any provided option.");
            }
            if (!normalizedCorrectIds.contains(trimmed)) {
                normalizedCorrectIds.add(trimmed);
            }
        }

        if (normalizedCorrectIds.isEmpty()) {
            throw new BadRequestException("At least one valid correct option must be specified.");
        }

        switch (type) {
            case SINGLE_CHOICE -> {
                if (normalizedCorrectIds.size() != 1) {
                    throw new BadRequestException("Single choice question must have exactly one correct option.");
                }
            }
            case BOOLEAN -> {
                if (normalizedCorrectIds.size() != 1) {
                    throw new BadRequestException("True/False question must have exactly one correct option.");
                }
                if (options.size() != 2) {
                    throw new BadRequestException("True/False question must contain exactly two options.");
                }
            }
            case MULTIPLE_CHOICE -> {
                if (normalizedCorrectIds.isEmpty()) {
                    throw new BadRequestException("Multiple choice question must have at least one correct option.");
                }
            }
        }

        return new ValidatedOptions(options, normalizedCorrectIds);
    }

    private Set<String> extractAndPromoteImages(String content, @Nullable List<String> explicitImageIds) {
        Set<String> imageIds = new LinkedHashSet<>();

        if (content != null) {
            Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(content);
            while (matcher.find()) {
                imageIds.add(matcher.group(1));
            }
        }

        if (explicitImageIds != null) {
            imageIds.addAll(explicitImageIds);
        }

        for (String fileId : imageIds) {
            try {
                fileService.moveToPermanent(fileId);
            } catch (Exception e) {
                log.warn("Unable to promote image [{}] to permanent: {}", fileId, e.getMessage());
            }
        }

        return imageIds;
    }

    private void cleanRemovedImages(Set<String> oldImageIds, Set<String> newImageIds, String currentQuestionId) {
        oldImageIds.removeAll(newImageIds);
        for (String removedFileId : oldImageIds) {
            if (!questionRepository.isImageUsedByOtherQuestion(removedFileId, currentQuestionId)) {
                try {
                    fileService.deleteFile(removedFileId);
                    log.info("Cleaned unreferenced image file [{}]", removedFileId);
                } catch (Exception e) {
                    log.warn("Failed to delete unreferenced image file [{}]: {}", removedFileId, e.getMessage());
                }
            }
        }
    }

    private void updateTopicCountsDiff(List<String> oldTopics, List<String> newTopics) {
        Set<String> oldSet = new HashSet<>(oldTopics);
        Set<String> newSet = new HashSet<>(newTopics);

        List<String> removed = oldSet.stream().filter(t -> !newSet.contains(t)).toList();
        List<String> added = newSet.stream().filter(t -> !oldSet.contains(t)).toList();

        topicService.decrementQuestionCounts(removed);
        topicService.incrementQuestionCounts(added);
    }

    private List<String> sanitizeList(@Nullable List<String> list) {
        if (list == null) return new ArrayList<>();
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private Map<String, Object> mcqAttributes(McqQuestion question) {
        Map<String, Object> attrs = new HashMap<>();
        if (question != null && question.getTitle() != null) {
            attrs.put("title", question.getTitle());
        }
        return attrs;
    }

    private void publishAnalytics(AnalyticsEventType eventType, String entityId, java.util.Map<String, Object> attributes) {
        if (eventPublisher != null) {
            try {
                AnalyticsEvent event = AnalyticsEvent.builder()
                        .eventType(eventType)
                        .entityType(AnalyticsEntityType.MCQ)
                        .entityId(entityId)
                        .actorType(AnalyticsActorType.ADMIN)
                        .attributes(attributes != null ? new java.util.HashMap<>(attributes) : new java.util.HashMap<>())
                        .build();
                eventPublisher.publishEvent(event);
            } catch (Exception ex) {
                log.warn("Failed to publish analytics event [{}]: {}", eventType, ex.getMessage());
            }
        }
    }

    private record ValidatedOptions(List<McqOption> options, List<String> correctOptionIds) {}
}
