package com.bharath.stacked.modules.question_bank.service.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.assessment.model.Assessment;
import com.bharath.stacked.modules.assessment.repository.AssessmentRepository;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.model.McqQuestion;
import com.bharath.stacked.modules.mcq.repository.McqQuestionRepository;
import com.bharath.stacked.modules.question_bank.dto.*;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import com.bharath.stacked.modules.question_bank.model.QuestionBank;
import com.bharath.stacked.modules.question_bank.model.QuestionBankItem;
import com.bharath.stacked.modules.question_bank.repository.QuestionBankRepository;
import com.bharath.stacked.modules.question_bank.service.QuestionBankService;
import com.bharath.stacked.common.cache.CacheInvalidationService;
import com.bharath.stacked.common.cache.CacheKeys;
import com.bharath.stacked.common.cache.StackedCacheService;
import com.bharath.stacked.config.cache.CacheProperties;
import tools.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import com.bharath.stacked.modules.analytics.enums.AnalyticsActorType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEntityType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuestionBankServiceImpl implements QuestionBankService, ApplicationEventPublisherAware {

    private final QuestionBankRepository questionBankRepository;
    private final CodingProblemRepository codingProblemRepository;
    private final McqQuestionRepository mcqQuestionRepository;
    private final AssessmentRepository assessmentRepository;
    private final MongoTemplate mongoTemplate;

    private final StackedCacheService cacheService;
    private final CacheProperties cacheProperties;
    private final CacheInvalidationService cacheInvalidationService;

    @org.springframework.beans.factory.annotation.Autowired
    public QuestionBankServiceImpl(QuestionBankRepository questionBankRepository,
                                   CodingProblemRepository codingProblemRepository,
                                   McqQuestionRepository mcqQuestionRepository,
                                   AssessmentRepository assessmentRepository,
                                   MongoTemplate mongoTemplate,
                                   @org.springframework.beans.factory.annotation.Autowired(required = false)
                                   StackedCacheService cacheService,
                                   @org.springframework.beans.factory.annotation.Autowired(required = false)
                                   CacheProperties cacheProperties,
                                   @org.springframework.beans.factory.annotation.Autowired(required = false)
                                   CacheInvalidationService cacheInvalidationService) {
        this.questionBankRepository = questionBankRepository;
        this.codingProblemRepository = codingProblemRepository;
        this.mcqQuestionRepository = mcqQuestionRepository;
        this.assessmentRepository = assessmentRepository;
        this.mongoTemplate = mongoTemplate;
        this.cacheService = cacheService;
        this.cacheProperties = cacheProperties;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    private ApplicationEventPublisher eventPublisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    private static final double DEFAULT_MCQ_POINTS = 1.0;

    @Override
    public QuestionBankSummaryResponse createQuestionBank(CreateQuestionBankRequest request, String authorId) {
        ValidatedItemsResult validated = processAndValidateItems(request.getItems());
        QuestionBankStatus status = request.getStatus() != null ? request.getStatus() : QuestionBankStatus.DRAFT;
        if (status == QuestionBankStatus.PUBLISHED && validated.items.isEmpty()) {
            throw new BadRequestException("Cannot publish an empty Question Bank. Curate at least one question first.");
        }

        QuestionBank bank = QuestionBank.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .category(request.getCategory() != null ? request.getCategory().trim() : "General")
                .status(status)
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .items(validated.items)
                .totalQuestions(validated.items.size())
                .mcqCount(validated.mcqCount)
                .codingCount(validated.codingCount)
                .totalPoints(validated.totalPoints)
                .authorId(authorId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        QuestionBank saved = questionBankRepository.save(bank);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateQuestionBank(saved.getId());
        }
        publishAnalytics(AnalyticsEventType.QUESTION_BANK_CREATED, saved.getId(), bankAttributes(saved));
        log.info("Created Question Bank: [{}] (id: {})", saved.getTitle(), saved.getId());
        return mapToSummary(saved);
    }

    @Override
    public PageResponse<QuestionBankSummaryResponse> getQuestionBanks(QuestionBankFilterRequest filter) {
        if (filter == null) {
            filter = new QuestionBankFilterRequest();
        }

        if (filter.isDefault() && cacheService != null && cacheProperties != null) {
            final QuestionBankFilterRequest effectiveFilter = filter;
            return cacheService.getOrCompute(
                    CacheKeys.questionBankDefaultPage(),
                    new TypeReference<PageResponse<QuestionBankSummaryResponse>>() {},
                    cacheProperties.catalogPageTtl(),
                    () -> executeGetQuestionBanks(effectiveFilter)
            );
        }

        return executeGetQuestionBanks(filter);
    }

    private PageResponse<QuestionBankSummaryResponse> executeGetQuestionBanks(QuestionBankFilterRequest filter) {
        Query query = new Query();

        if (StringUtils.hasText(filter.getSearch())) {
            String pattern = ".*" + filter.getSearch().trim() + ".*";
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(pattern, "i"),
                    Criteria.where("description").regex(pattern, "i"),
                    Criteria.where("category").regex(pattern, "i"),
                    Criteria.where("tags").regex(pattern, "i")
            );
            query.addCriteria(searchCriteria);
        }

        if (StringUtils.hasText(filter.getCategory())) {
            query.addCriteria(Criteria.where("category").is(filter.getCategory().trim()));
        }

        if (filter.getStatus() != null) {
            query.addCriteria(Criteria.where("status").is(filter.getStatus()));
        }

        long totalElements = mongoTemplate.count(query, QuestionBank.class);

        int page = filter.getPage() != null && filter.getPage() >= 0 ? filter.getPage() : 0;
        int size = filter.getSize() != null && filter.getSize() > 0 ? filter.getSize() : 10;

        String sortBy = StringUtils.hasText(filter.getSortBy()) ? filter.getSortBy() : "updatedAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        query.with(pageable);

        List<QuestionBank> banks = mongoTemplate.find(query, QuestionBank.class);
        List<QuestionBankSummaryResponse> items = banks.stream()
                .map(this::mapToSummary)
                .toList();

        return PageResponse.of(items, page, size, totalElements);
    }

    @Override
    public QuestionBankDetailResponse getQuestionBank(String id) {
        if (cacheService != null && cacheProperties != null) {
            return cacheService.getOrCompute(
                    CacheKeys.questionBankDetail(id),
                    QuestionBankDetailResponse.class,
                    cacheProperties.detailsTtl(),
                    () -> executeGetQuestionBank(id)
            );
        }
        return executeGetQuestionBank(id);
    }

    private QuestionBankDetailResponse executeGetQuestionBank(String id) {
        QuestionBank bank = findById(id);
        List<HydratedQuestionItemResponse> hydratedItems = hydrateItems(bank.getItems());

        return QuestionBankDetailResponse.builder()
                .id(bank.getId())
                .title(bank.getTitle())
                .description(bank.getDescription())
                .category(bank.getCategory())
                .status(bank.getStatus())
                .tags(bank.getTags())
                .totalQuestions(bank.getTotalQuestions())
                .mcqCount(bank.getMcqCount())
                .codingCount(bank.getCodingCount())
                .totalPoints(bank.getTotalPoints())
                .items(hydratedItems)
                .authorId(bank.getAuthorId())
                .createdAt(bank.getCreatedAt())
                .updatedAt(bank.getUpdatedAt())
                .build();
    }

    @Override
    public QuestionBankSummaryResponse updateQuestionBank(String id, UpdateQuestionBankRequest request) {
        QuestionBank existing = findById(id);

        if (existing.getStatus() != QuestionBankStatus.DRAFT) {
            throw new ConflictException("Cannot edit Question Bank '" + existing.getTitle() + "' because it is currently " + existing.getStatus() + ". Unpublish to DRAFT first.");
        }

        ValidatedItemsResult validated = processAndValidateItems(request.getItems());

        existing.setTitle(request.getTitle().trim());
        existing.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        existing.setCategory(request.getCategory() != null ? request.getCategory().trim() : "General");
        // Status remains DRAFT during editing
        existing.setStatus(QuestionBankStatus.DRAFT);
        existing.setTags(request.getTags() != null ? request.getTags() : new ArrayList<>());
        existing.setItems(validated.items);
        existing.setTotalQuestions(validated.items.size());
        existing.setMcqCount(validated.mcqCount);
        existing.setCodingCount(validated.codingCount);
        existing.setTotalPoints(validated.totalPoints);
        existing.setUpdatedAt(Instant.now());

        QuestionBank updated = questionBankRepository.save(existing);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateQuestionBank(updated.getId());
        }
        publishAnalytics(AnalyticsEventType.QUESTION_BANK_UPDATED, updated.getId(), bankAttributes(updated));
        log.info("Updated Question Bank: [{}] (id: {})", updated.getTitle(), updated.getId());
        return mapToSummary(updated);
    }

    @Override
    public boolean isInUse(String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }
        return assessmentRepository.existsByQuestionBankId(id);
    }

    @Override
    public QuestionBankSummaryResponse updateStatus(String id, QuestionBankStatus status) {
        if (id == null || id.isBlank()) {
            throw new BadRequestException("Question bank identifier cannot be empty.");
        }
        if (status == null) {
            throw new BadRequestException("Status cannot be null.");
        }

        QuestionBank bank = findById(id);
        QuestionBankStatus currentStatus = bank.getStatus();
        if (currentStatus == status) {
            return mapToSummary(bank);
        }

        if (status == QuestionBankStatus.DRAFT && currentStatus == QuestionBankStatus.PUBLISHED) {
            if (isInUse(bank.getId())) {
                List<Assessment> assessments = assessmentRepository.findAllByQuestionBankId(bank.getId());
                String assessmentTitles = assessments.stream()
                        .map(Assessment::getTitle)
                        .collect(Collectors.joining(", "));
                throw new ConflictException("Cannot move question bank '" + bank.getTitle() + "' back to draft because it is currently in use by " + assessments.size() + " assessment(s) [" + assessmentTitles + "].");
            }
        }

        if (status == QuestionBankStatus.PUBLISHED) {
            if (bank.getItems() == null || bank.getItems().isEmpty()) {
                throw new BadRequestException("Cannot publish an empty Question Bank. Curate at least one question first.");
            }
            validateAllItemsPublished(bank.getItems());
        }

        bank.setStatus(status);
        bank.setUpdatedAt(Instant.now());
        QuestionBank updated = questionBankRepository.save(bank);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateQuestionBank(updated.getId());
        }
        AnalyticsEventType eventType = updated.getStatus() == QuestionBankStatus.PUBLISHED
                ? AnalyticsEventType.QUESTION_BANK_PUBLISHED
                : AnalyticsEventType.QUESTION_BANK_UNPUBLISHED;
        publishAnalytics(eventType, updated.getId(), bankAttributes(updated));
        log.info("Updated Question Bank status: [{}] -> {} (id: {})", updated.getTitle(), status, updated.getId());
        return mapToSummary(updated);
    }

    @Override
    public QuestionBankSummaryResponse publish(String id) {
        return updateStatus(id, QuestionBankStatus.PUBLISHED);
    }

    @Override
    public QuestionBankSummaryResponse unpublish(String id) {
        return updateStatus(id, QuestionBankStatus.DRAFT);
    }

    @Override
    public void deleteQuestionBank(String id) {
        QuestionBank bank = findById(id);

        if (isInUse(bank.getId())) {
            List<Assessment> assessments = assessmentRepository.findAllByQuestionBankId(bank.getId());
            String assessmentTitles = assessments.stream()
                    .map(Assessment::getTitle)
                    .collect(Collectors.joining(", "));
            throw new ConflictException("Cannot delete question bank '" + bank.getTitle() + "' because it is currently in use by " + assessments.size() + " assessment(s) [" + assessmentTitles + "].");
        }

        questionBankRepository.delete(bank);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateQuestionBank(bank.getId());
        }
        publishAnalytics(AnalyticsEventType.QUESTION_BANK_DELETED, bank.getId(), bankAttributes(bank));
        log.info("Deleted Question Bank: [{}] (id: {})", bank.getTitle(), bank.getId());
    }

    @Override
    public QuestionBankSummaryResponse duplicateQuestionBank(String id, String authorId) {
        QuestionBank source = findById(id);

        String newTitle = "Copy of " + source.getTitle();

        QuestionBank duplicate = QuestionBank.builder()
                .title(newTitle)
                .description(source.getDescription())
                .category(source.getCategory())
                .status(QuestionBankStatus.DRAFT)
                .tags(new ArrayList<>(source.getTags()))
                .items(new ArrayList<>(source.getItems()))
                .totalQuestions(source.getTotalQuestions())
                .mcqCount(source.getMcqCount())
                .codingCount(source.getCodingCount())
                .totalPoints(source.getTotalPoints())
                .authorId(authorId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        QuestionBank saved = questionBankRepository.save(duplicate);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateQuestionBank(saved.getId());
        }
        Map<String, Object> dupAttrs = bankAttributes(saved);
        if (source != null && source.getId() != null) {
            dupAttrs.put("duplicatedFrom", source.getId());
        }
        publishAnalytics(AnalyticsEventType.QUESTION_BANK_CREATED, saved.getId(), dupAttrs);
        log.info("Duplicated Question Bank: [{}] -> [{}] (id: {})", source.getTitle(), saved.getTitle(), saved.getId());
        return mapToSummary(saved);
    }

    @Override
    public List<String> getDistinctCategories() {
        if (cacheService != null && cacheProperties != null) {
            return cacheService.getOrCompute(
                    CacheKeys.questionBankCategories(),
                    new TypeReference<List<String>>() {},
                    cacheProperties.metadataTtl(),
                    this::executeGetDistinctCategories
            );
        }
        return executeGetDistinctCategories();
    }

    private List<String> executeGetDistinctCategories() {
        return mongoTemplate.query(QuestionBank.class)
                .distinct("category")
                .as(String.class)
                .all()
                .stream()
                .filter(StringUtils::hasText)
                .sorted()
                .toList();
    }

    @Override
    public QuestionBankStatsResponse getStats() {
        if (cacheService != null && cacheProperties != null) {
            return cacheService.getOrCompute(
                    CacheKeys.questionBankStats(),
                    QuestionBankStatsResponse.class,
                    cacheProperties.metadataTtl(),
                    this::executeGetStats
            );
        }
        return executeGetStats();
    }

    private QuestionBankStatsResponse executeGetStats() {
        long totalBanks = questionBankRepository.count();
        long publishedBanks = questionBankRepository.countByStatus(QuestionBankStatus.PUBLISHED);
        long draftBanks = questionBankRepository.countByStatus(QuestionBankStatus.DRAFT);

        List<QuestionBank> allBanks = questionBankRepository.findAll();
        long totalCuratedQuestions = allBanks.stream()
                .mapToLong(QuestionBank::getTotalQuestions)
                .sum();

        long totalCategories = allBanks.stream()
                .map(QuestionBank::getCategory)
                .filter(StringUtils::hasText)
                .distinct()
                .count();

        return QuestionBankStatsResponse.builder()
                .totalBanks(totalBanks)
                .publishedBanks(publishedBanks)
                .draftBanks(draftBanks)
                .totalCuratedQuestions(totalCuratedQuestions)
                .totalCategories(totalCategories)
                .build();
    }

    @Override
    @Transactional
    public void removeQuestionFromAllBanks(String questionId) {
        if (!StringUtils.hasText(questionId)) {
            return;
        }

        List<QuestionBank> affectedBanks = questionBankRepository.findAllByItemsQuestionId(questionId);
        if (affectedBanks.isEmpty()) {
            return;
        }

        for (QuestionBank bank : affectedBanks) {
            if (bank.getItems() == null || bank.getItems().isEmpty()) {
                continue;
            }

            boolean removed = bank.getItems().removeIf(item -> questionId.equals(item.getQuestionId()));
            if (!removed) {
                continue;
            }

            recalculateBankMetrics(bank);

            // If question bank has no questions left and was PUBLISHED, fall back to DRAFT
            if (bank.getItems().isEmpty() && bank.getStatus() == QuestionBankStatus.PUBLISHED) {
                bank.setStatus(QuestionBankStatus.DRAFT);
                log.info("Question Bank [{}] has no questions left after archiving question [{}]; status changed to DRAFT",
                        bank.getTitle(), questionId);
            }

            bank.setUpdatedAt(Instant.now());
            questionBankRepository.save(bank);
            log.info("Removed deleted question [{}] from Question Bank [{}] (remaining items: {})",
                    questionId, bank.getTitle(), bank.getItems().size());
        }
    }

    private void recalculateBankMetrics(QuestionBank bank) {
        if (bank.getItems() == null || bank.getItems().isEmpty()) {
            bank.setItems(new ArrayList<>());
            bank.setTotalQuestions(0);
            bank.setMcqCount(0);
            bank.setCodingCount(0);
            bank.setTotalPoints(0.0);
            return;
        }

        List<String> codingIds = bank.getItems().stream()
                .filter(item -> item.getQuestionType() == BankQuestionType.CODING)
                .map(QuestionBankItem::getQuestionId)
                .toList();

        List<String> mcqIds = bank.getItems().stream()
                .filter(item -> item.getQuestionType() == BankQuestionType.MCQ)
                .map(QuestionBankItem::getQuestionId)
                .toList();

        Map<String, CodingProblem> codingMap = codingIds.isEmpty() ? Collections.emptyMap() :
                codingProblemRepository.findAllById(codingIds).stream()
                        .collect(Collectors.toMap(CodingProblem::getId, Function.identity()));

        Map<String, McqQuestion> mcqMap = mcqIds.isEmpty() ? Collections.emptyMap() :
                mcqQuestionRepository.findAllById(mcqIds).stream()
                        .collect(Collectors.toMap(McqQuestion::getId, Function.identity()));

        int mcqCount = 0;
        int codingCount = 0;
        double totalPoints = 0.0;

        for (int i = 0; i < bank.getItems().size(); i++) {
            QuestionBankItem item = bank.getItems().get(i);
            item.setOrder(i + 1);

            if (item.getQuestionType() == BankQuestionType.CODING) {
                codingCount++;
                if (item.getCustomPoints() != null) {
                    totalPoints += item.getCustomPoints();
                } else {
                    CodingProblem cp = codingMap.get(item.getQuestionId());
                    totalPoints += (cp != null ? getDefaultCodingPoints(cp) : 10.0);
                }
            } else {
                mcqCount++;
                if (item.getCustomPoints() != null) {
                    totalPoints += item.getCustomPoints();
                } else {
                    totalPoints += DEFAULT_MCQ_POINTS;
                }
            }
        }

        bank.setTotalQuestions(mcqCount + codingCount);
        bank.setMcqCount(mcqCount);
        bank.setCodingCount(codingCount);
        bank.setTotalPoints(totalPoints);
    }

    // ==========================================
    // Internal Helper Methods
    // ==========================================

    private record ValidatedItemsResult(List<QuestionBankItem> items, int mcqCount, int codingCount, double totalPoints) {}

    private ValidatedItemsResult processAndValidateItems(List<QuestionBankItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return new ValidatedItemsResult(new ArrayList<>(), 0, 0, 0.0);
        }

        List<String> codingIds = itemRequests.stream()
                .filter(r -> r.getQuestionType() == BankQuestionType.CODING)
                .map(QuestionBankItemRequest::getQuestionId)
                .distinct()
                .toList();

        List<String> mcqIds = itemRequests.stream()
                .filter(r -> r.getQuestionType() == BankQuestionType.MCQ)
                .map(QuestionBankItemRequest::getQuestionId)
                .distinct()
                .toList();

        Map<String, CodingProblem> codingMap = codingIds.isEmpty() ? Collections.emptyMap() :
                codingProblemRepository.findAllById(codingIds).stream()
                        .collect(Collectors.toMap(CodingProblem::getId, Function.identity()));

        Map<String, McqQuestion> mcqMap = mcqIds.isEmpty() ? Collections.emptyMap() :
                mcqQuestionRepository.findAllById(mcqIds).stream()
                        .collect(Collectors.toMap(McqQuestion::getId, Function.identity()));

        List<QuestionBankItem> items = new ArrayList<>();
        int mcqCount = 0;
        int codingCount = 0;
        double totalPoints = 0.0;

        for (int i = 0; i < itemRequests.size(); i++) {
            QuestionBankItemRequest req = itemRequests.get(i);
            double points;

            if (req.getQuestionType() == BankQuestionType.CODING) {
                CodingProblem cp = codingMap.get(req.getQuestionId());
                if (cp == null) {
                    throw new ResourceNotFoundException("Coding Problem not found with ID: " + req.getQuestionId());
                }
                if (cp.getStatus() != CodingProblemStatus.PUBLISHED) {
                    throw new BadRequestException("Coding Problem '" + cp.getTitle() + "' is not published and cannot be added to a Question Bank.");
                }
                codingCount++;
                points = req.getCustomPoints() != null ? req.getCustomPoints() : getDefaultCodingPoints(cp);
            } else {
                McqQuestion mcq = mcqMap.get(req.getQuestionId());
                if (mcq == null) {
                    throw new ResourceNotFoundException("MCQ Question not found with ID: " + req.getQuestionId());
                }
                if (mcq.getStatus() != McqStatus.PUBLISHED) {
                    throw new BadRequestException("MCQ Question '" + mcq.getTitle() + "' is not published and cannot be added to a Question Bank.");
                }
                mcqCount++;
                points = req.getCustomPoints() != null ? req.getCustomPoints() : DEFAULT_MCQ_POINTS;
            }

            totalPoints += points;
            items.add(QuestionBankItem.builder()
                    .questionType(req.getQuestionType())
                    .questionId(req.getQuestionId())
                    .order(req.getOrder() > 0 ? req.getOrder() : i + 1)
                    .customPoints(req.getCustomPoints())
                    .build());
        }

        return new ValidatedItemsResult(items, mcqCount, codingCount, totalPoints);
    }

    private void validateAllItemsPublished(List<QuestionBankItem> items) {
        if (items == null || items.isEmpty()) return;

        List<String> codingIds = items.stream()
                .filter(it -> it.getQuestionType() == BankQuestionType.CODING)
                .map(QuestionBankItem::getQuestionId)
                .distinct()
                .toList();

        List<String> mcqIds = items.stream()
                .filter(it -> it.getQuestionType() == BankQuestionType.MCQ)
                .map(QuestionBankItem::getQuestionId)
                .distinct()
                .toList();

        if (!codingIds.isEmpty()) {
            Map<String, CodingProblem> codingMap = codingProblemRepository.findAllById(codingIds).stream()
                    .collect(Collectors.toMap(CodingProblem::getId, Function.identity()));
            for (String cId : codingIds) {
                CodingProblem cp = codingMap.get(cId);
                if (cp == null) {
                    throw new ResourceNotFoundException("Coding Problem not found with ID: " + cId);
                }
                if (cp.getStatus() != CodingProblemStatus.PUBLISHED) {
                    throw new BadRequestException("Coding Problem '" + cp.getTitle() + "' is not published. All questions must be published to publish this Question Bank.");
                }
            }
        }

        if (!mcqIds.isEmpty()) {
            Map<String, McqQuestion> mcqMap = mcqQuestionRepository.findAllById(mcqIds).stream()
                    .collect(Collectors.toMap(McqQuestion::getId, Function.identity()));
            for (String mId : mcqIds) {
                McqQuestion mcq = mcqMap.get(mId);
                if (mcq == null) {
                    throw new ResourceNotFoundException("MCQ Question not found with ID: " + mId);
                }
                if (mcq.getStatus() != McqStatus.PUBLISHED) {
                    throw new BadRequestException("MCQ Question '" + mcq.getTitle() + "' is not published. All questions must be published to publish this Question Bank.");
                }
            }
        }
    }

    private List<HydratedQuestionItemResponse> hydrateItems(List<QuestionBankItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> codingIds = items.stream()
                .filter(it -> it.getQuestionType() == BankQuestionType.CODING)
                .map(QuestionBankItem::getQuestionId)
                .distinct()
                .toList();

        List<String> mcqIds = items.stream()
                .filter(it -> it.getQuestionType() == BankQuestionType.MCQ)
                .map(QuestionBankItem::getQuestionId)
                .distinct()
                .toList();

        Map<String, CodingProblem> codingMap = codingIds.isEmpty() ? Collections.emptyMap() :
                codingProblemRepository.findAllById(codingIds).stream()
                        .collect(Collectors.toMap(CodingProblem::getId, Function.identity()));

        Map<String, McqQuestion> mcqMap = mcqIds.isEmpty() ? Collections.emptyMap() :
                mcqQuestionRepository.findAllById(mcqIds).stream()
                        .collect(Collectors.toMap(McqQuestion::getId, Function.identity()));

        List<HydratedQuestionItemResponse> result = new ArrayList<>();

        for (QuestionBankItem item : items) {
            if (item.getQuestionType() == BankQuestionType.CODING) {
                CodingProblem cp = codingMap.get(item.getQuestionId());
                if (cp != null) {
                    double effectivePoints = item.getCustomPoints() != null ? item.getCustomPoints() : getDefaultCodingPoints(cp);
                    result.add(HydratedQuestionItemResponse.builder()
                            .questionType(BankQuestionType.CODING)
                            .questionId(cp.getId())
                            .order(item.getOrder())
                            .customPoints(item.getCustomPoints())
                            .effectivePoints(effectivePoints)
                            .title(cp.getTitle())
                            .slug(cp.getSlug())
                            .snippet(createSnippet(cp.getStatement()))
                            .difficulty(cp.getDifficulty() != null ? cp.getDifficulty().name() : "MEDIUM")
                            .topics(cp.getTopics())
                            .build());
                }
            } else {
                McqQuestion mcq = mcqMap.get(item.getQuestionId());
                if (mcq != null) {
                    double effectivePoints = item.getCustomPoints() != null ? item.getCustomPoints() : DEFAULT_MCQ_POINTS;
                    result.add(HydratedQuestionItemResponse.builder()
                            .questionType(BankQuestionType.MCQ)
                            .questionId(mcq.getId())
                            .order(item.getOrder())
                            .customPoints(item.getCustomPoints())
                            .effectivePoints(effectivePoints)
                            .title(mcq.getTitle())
                            .snippet(createSnippet(mcq.getContent()))
                            .difficulty(mcq.getDifficulty() != null ? mcq.getDifficulty().name() : "MEDIUM")
                            .topics(mcq.getTopics())
                            .build());
                }
            }
        }

        result.sort(Comparator.comparingInt(HydratedQuestionItemResponse::getOrder));
        return result;
    }

    private double getDefaultCodingPoints(CodingProblem cp) {
        if (cp.getDifficulty() == null) return 20.0;
        return switch (cp.getDifficulty()) {
            case EASY -> 10.0;
            case MEDIUM -> 20.0;
            case HARD -> 30.0;
        };
    }

    private String createSnippet(String content) {
        if (!StringUtils.hasText(content)) return "";
        String cleaned = content.replaceAll("[#*`>\\[\\]]", " ").replaceAll("\\s+", " ").trim();
        return cleaned.length() > 140 ? cleaned.substring(0, 137) + "..." : cleaned;
    }

    private QuestionBank findById(String id) {
        return questionBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question Bank not found: " + id));
    }

    private QuestionBankSummaryResponse mapToSummary(QuestionBank bank) {
        return QuestionBankSummaryResponse.builder()
                .id(bank.getId())
                .title(bank.getTitle())
                .description(bank.getDescription())
                .category(bank.getCategory())
                .status(bank.getStatus())
                .tags(bank.getTags())
                .totalQuestions(bank.getTotalQuestions())
                .mcqCount(bank.getMcqCount())
                .codingCount(bank.getCodingCount())
                .totalPoints(bank.getTotalPoints())
                .createdAt(bank.getCreatedAt())
                .updatedAt(bank.getUpdatedAt())
                .build();
    }

    private Map<String, Object> bankAttributes(QuestionBank bank) {
        Map<String, Object> attrs = new HashMap<>();
        if (bank != null && bank.getTitle() != null) {
            attrs.put("title", bank.getTitle());
        }
        return attrs;
    }

    private void publishAnalytics(AnalyticsEventType eventType, String entityId, Map<String, Object> attributes) {
        if (eventPublisher != null) {
            try {
                AnalyticsEvent event = AnalyticsEvent.builder()
                        .eventType(eventType)
                        .entityType(AnalyticsEntityType.QUESTION_BANK)
                        .entityId(entityId)
                        .actorType(AnalyticsActorType.ADMIN)
                        .attributes(attributes != null ? new HashMap<>(attributes) : new HashMap<>())
                        .build();
                eventPublisher.publishEvent(event);
            } catch (Exception ex) {
                log.warn("Failed to publish analytics event [{}]: {}", eventType, ex.getMessage());
            }
        }
    }
}
