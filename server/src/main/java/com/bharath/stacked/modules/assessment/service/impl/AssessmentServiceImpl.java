package com.bharath.stacked.modules.assessment.service.impl;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.assessment.dto.*;
import com.bharath.stacked.modules.assessment.enums.AssessmentAuditEventSource;
import com.bharath.stacked.modules.assessment.enums.AssessmentAuditEventType;
import com.bharath.stacked.modules.assessment.enums.AssessmentSelectionMode;
import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.enums.AttemptStatus;
import com.bharath.stacked.modules.assessment.enums.CandidateParticipationStatus;
import com.bharath.stacked.modules.assessment.model.*;
import com.bharath.stacked.modules.assessment.repository.*;
import com.bharath.stacked.modules.assessment.service.AssessmentService;
import com.bharath.stacked.modules.assessment.websocket.AssessmentWebSocketHandler;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.CandidateTestcaseItem;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.JudgeCandidateExecutionRequest;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.model.TestCase;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.resolver.ExecutionConstraintResolver;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.LanguageType;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import com.bharath.stacked.modules.mcq.enums.McqStatus;
import com.bharath.stacked.modules.mcq.enums.McqType;
import com.bharath.stacked.modules.mcq.model.McqOption;
import com.bharath.stacked.modules.mcq.model.McqQuestion;
import com.bharath.stacked.modules.mcq.repository.McqQuestionRepository;
import com.bharath.stacked.modules.question_bank.enums.BankQuestionType;
import com.bharath.stacked.modules.question_bank.enums.QuestionBankStatus;
import com.bharath.stacked.modules.question_bank.model.QuestionBank;
import com.bharath.stacked.modules.question_bank.model.QuestionBankItem;
import com.bharath.stacked.modules.question_bank.repository.QuestionBankRepository;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
import com.bharath.stacked.common.cache.CacheInvalidationService;
import com.bharath.stacked.common.cache.CacheKeys;
import com.bharath.stacked.common.cache.StackedCacheService;
import com.bharath.stacked.config.cache.CacheProperties;
import tools.jackson.core.type.TypeReference;
import com.bharath.stacked.modules.analytics.enums.AnalyticsActorType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEntityType;
import com.bharath.stacked.modules.analytics.enums.AnalyticsEventType;
import com.bharath.stacked.modules.analytics.model.AnalyticsEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService, ApplicationEventPublisherAware {

    private ApplicationEventPublisher eventPublisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private final AssessmentRepository assessmentRepository;
    private final AssessmentAssignmentRepository assignmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AssessmentQuestionResponseRepository responseRepository;
    private final AssessmentConnectionEventRepository connectionEventRepository;
    private final QuestionBankRepository questionBankRepository;
    private final CodingProblemRepository codingProblemRepository;
    private final McqQuestionRepository mcqQuestionRepository;
    private final UserRepository userRepository;
    private final CodingProblemJudgeService judgeService;
    private final LanguageRegistry languageRegistry;
    private final ExecutionConstraintResolver executionConstraintResolver;
    private final MongoTemplate mongoTemplate;
    private final AssessmentAuditEventRepository auditEventRepository;
    private final AssessmentWebSocketHandler webSocketHandler;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private StackedCacheService cacheService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CacheProperties cacheProperties;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CacheInvalidationService cacheInvalidationService;

    private void recordAuditEvent(String assessmentId, String attemptId, String candidateUserId, String candidateEmail,
                                  String questionId, AssessmentAuditEventType eventType, AssessmentAuditEventSource source,
                                  Map<String, Object> metadata) {
        try {
            AssessmentAuditEvent event = AssessmentAuditEvent.builder()
                    .assessmentId(assessmentId)
                    .attemptId(attemptId)
                    .candidateUserId(candidateUserId)
                    .candidateEmail(candidateEmail)
                    .questionId(questionId)
                    .eventType(eventType)
                    .source(source)
                    .metadata(metadata != null ? new HashMap<>(metadata) : new HashMap<>())
                    .timestamp(Instant.now())
                    .createdAt(Instant.now())
                    .build();
            auditEventRepository.save(event);
        } catch (Exception ex) {
            log.warn("Failed to record audit event [{}] for attempt [{}]: {}", eventType, attemptId, ex.getMessage());
        }
    }

    // ==========================================
    // Administrator Operations
    // ==========================================

    @Override
    @Transactional
    public AssessmentSummaryResponse createAssessment(CreateAssessmentRequest request, String authorId) {
        // Dedicated pre-creation validation
        AssessmentValidationResponse validation = validateAssessmentConfig(request);
        if (!validation.isValid()) {
            String details = validation.getErrors().stream()
                    .map(e -> "[" + e.getField() + "] " + e.getMessage())
                    .collect(Collectors.joining("; "));
            throw new BadRequestException("Assessment configuration validation failed: " + details);
        }

        // Prevent rapid duplicate creation (within 5 seconds by same author)
        if (authorId != null && StringUtils.hasText(request.getTitle())) {
            Instant recentThreshold = Instant.now().minusSeconds(5);
            Query duplicateQuery = new Query(Criteria.where("title").is(request.getTitle().trim())
                    .and("authorId").is(authorId)
                    .and("createdAt").gte(recentThreshold));
            if (mongoTemplate.exists(duplicateQuery, Assessment.class)) {
                throw new BadRequestException("An assessment with this title was recently submitted. Duplicate creation prevented.");
            }
        }

        String slug = generateUniqueSlug(request.getTitle(), null);
        ProcessedItemsResult itemsResult = processAndSnapshotItems(
                request.getSelectionMode(),
                request.getQuestionBankId(),
                request.getCustomQuestionItems()
        );

        validatePublicationEligibility(itemsResult.items(), request.getDurationMinutes(), request.getScheduledStart(), request.getScheduledEnd());

        Instant now = Instant.now();
        AssessmentStatus initialStatus = AssessmentStatus.deriveStatus(request.getScheduledStart(), request.getScheduledEnd(), now);

        Assessment assessment = Assessment.builder()
                .title(request.getTitle().trim())
                .slug(slug)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .status(initialStatus)
                .selectionMode(request.getSelectionMode())
                .questionBankId(request.getQuestionBankId())
                .items(itemsResult.items())
                .scheduledStart(request.getScheduledStart())
                .scheduledEnd(request.getScheduledEnd())
                .durationMinutes(request.getDurationMinutes())
                .maxAttempts(request.getMaxAttempts() != null && request.getMaxAttempts() > 0 ? request.getMaxAttempts() : null)
                .passingScore(request.getPassingScore())
                .totalScore(itemsResult.totalPoints())
                .totalQuestions(itemsResult.items().size())
                .mcqCount(itemsResult.mcqCount())
                .codingCount(itemsResult.codingCount())
                .invitationCode(request.isEnableInvitationCode() ? generateSecureInvitationCode() : null)
                .invitationCodeEnabled(request.isEnableInvitationCode())
                .invitationCodeExpiresAt(request.getInvitationCodeExpiresAt() != null ? request.getInvitationCodeExpiresAt() : request.getScheduledEnd())
                .invitationCodeMaxUses(request.getInvitationCodeMaxUses())
                .authorId(authorId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Assessment saved = assessmentRepository.save(assessment);

        // Process candidate emails if provided
        Set<String> parsedEmails = parseEmails(request.getCandidateEmailsRaw(), request.getCandidateEmails());
        if (!parsedEmails.isEmpty()) {
            int assignedCount = assignCandidatesInternal(saved.getId(), parsedEmails);
            saved.setAssignedCandidateCount(assignedCount);
            saved = assessmentRepository.save(saved);
        }

        log.info("Created Assessment: [{}] (id: {}, slug: {}, status: {})", saved.getTitle(), saved.getId(), saved.getSlug(), saved.getStatus());
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateAssessment(saved.getId(), saved.getSlug());
        }
        publishAnalytics(AnalyticsEventType.ASSESSMENT_CREATED,
                saved.getId(),
                AnalyticsEntityType.ASSESSMENT,
                saved.getId(),
                null,
                AnalyticsActorType.ADMIN,
                authorId,
                assessmentAttributes(saved));
        return AssessmentSummaryResponse.fromModel(saved);
    }

    @Override
    public AssessmentValidationResponse validateAssessmentConfig(CreateAssessmentRequest request) {
        List<AssessmentValidationError> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (request == null) {
            errors.add(AssessmentValidationError.builder()
                    .field("request")
                    .code("REQUIRED")
                    .message("Assessment creation request cannot be null.")
                    .build());
            return AssessmentValidationResponse.builder().valid(false).errors(errors).build();
        }

        // 1. Basic Fields
        if (!StringUtils.hasText(request.getTitle())) {
            errors.add(AssessmentValidationError.builder()
                    .field("title")
                    .code("REQUIRED")
                    .message("Assessment title is required.")
                    .build());
        } else if (request.getTitle().trim().length() < 3 || request.getTitle().trim().length() > 200) {
            errors.add(AssessmentValidationError.builder()
                    .field("title")
                    .code("INVALID_LENGTH")
                    .message("Title must be between 3 and 200 characters.")
                    .build());
        }

        // 2. Duration & Schedule
        if (request.getDurationMinutes() == null) {
            errors.add(AssessmentValidationError.builder()
                    .field("durationMinutes")
                    .code("REQUIRED")
                    .message("Duration is required.")
                    .build());
        } else if (request.getDurationMinutes() < 1 || request.getDurationMinutes() > 1440) {
            errors.add(AssessmentValidationError.builder()
                    .field("durationMinutes")
                    .code("OUT_OF_RANGE")
                    .message("Duration must be between 1 minute and 1440 minutes (24 hours).")
                    .build());
        }

        if (request.getScheduledStart() != null && request.getScheduledEnd() != null) {
            if (!request.getScheduledEnd().isAfter(request.getScheduledStart())) {
                errors.add(AssessmentValidationError.builder()
                        .field("scheduledWindow")
                        .code("INVALID_WINDOW")
                        .message("Scheduled end time must be after scheduled start time.")
                        .build());
            } else if (request.getDurationMinutes() != null) {
                long windowMinutes = Duration.between(request.getScheduledStart(), request.getScheduledEnd()).toMinutes();
                if (windowMinutes < request.getDurationMinutes()) {
                    errors.add(AssessmentValidationError.builder()
                            .field("scheduledWindow")
                            .code("WINDOW_TOO_SHORT")
                            .message("Scheduled window (" + windowMinutes + " mins) is shorter than assessment duration (" + request.getDurationMinutes() + " mins).")
                            .build());
                }
            }
        }

        if (request.getMaxAttempts() != null && request.getMaxAttempts() < 1) {
            errors.add(AssessmentValidationError.builder()
                    .field("maxAttempts")
                    .code("OUT_OF_RANGE")
                    .message("Maximum attempts must be at least 1.")
                    .build());
        }

        // 3. Question Bank Selection & Question Completeness
        QuestionBankPreviewDto bankPreview = null;
        double totalPoints = 0.0;
        int totalQuestions = 0;

        if (request.getSelectionMode() == AssessmentSelectionMode.QUESTION_BANK) {
            if (!StringUtils.hasText(request.getQuestionBankId())) {
                errors.add(AssessmentValidationError.builder()
                        .field("questionBankId")
                        .code("REQUIRED")
                        .message("Question Bank must be selected.")
                        .build());
            } else {
                Optional<QuestionBank> bankOpt = questionBankRepository.findById(request.getQuestionBankId());
                if (bankOpt.isEmpty()) {
                    errors.add(AssessmentValidationError.builder()
                            .field("questionBankId")
                            .code("NOT_FOUND")
                            .message("Question Bank not found with ID: " + request.getQuestionBankId())
                            .build());
                } else {
                    QuestionBank bank = bankOpt.get();
                    bankPreview = buildQuestionBankPreview(bank, errors, warnings);
                    if (bankPreview != null) {
                        totalPoints = bankPreview.getTotalPoints();
                        totalQuestions = bankPreview.getTotalQuestions();
                    }
                }
            }
        } else {
            // INDIVIDUAL_QUESTIONS mode (if used)
            if (request.getCustomQuestionItems() == null || request.getCustomQuestionItems().isEmpty()) {
                errors.add(AssessmentValidationError.builder()
                        .field("questions")
                        .code("EMPTY")
                        .message("At least one question must be selected for the assessment.")
                        .build());
            } else {
                Set<String> seenIds = new HashSet<>();
                for (AssessmentCustomItemRequest itemReq : request.getCustomQuestionItems()) {
                    if (!seenIds.add(itemReq.getQuestionId())) {
                        errors.add(AssessmentValidationError.builder()
                                .field("questions")
                                .code("DUPLICATE_QUESTION")
                                .questionId(itemReq.getQuestionId())
                                .message("Duplicate question detected: " + itemReq.getQuestionId())
                                .build());
                    }
                    validateSingleQuestionEligibility(itemReq.getQuestionType(), itemReq.getQuestionId(), errors);
                    double pts = itemReq.getPoints() != null ? itemReq.getPoints() : getDefaultPoints(itemReq.getQuestionType(), itemReq.getQuestionId());
                    totalPoints += pts;
                    totalQuestions++;
                }
            }
        }

        // 4. Passing score check
        if (request.getPassingScore() != null) {
            if (request.getPassingScore() <= 0) {
                errors.add(AssessmentValidationError.builder()
                        .field("passingScore")
                        .code("OUT_OF_RANGE")
                        .message("Passing score must be greater than 0.")
                        .build());
            } else if (totalPoints > 0 && request.getPassingScore() > totalPoints) {
                errors.add(AssessmentValidationError.builder()
                        .field("passingScore")
                        .code("EXCEEDS_TOTAL")
                        .message("Passing score (" + request.getPassingScore() + ") cannot exceed total assessment points (" + totalPoints + ").")
                        .build());
            }
        }

        // 5. Candidate emails validation
        Set<String> rawTokens = parseRawTokens(request.getCandidateEmailsRaw(), request.getCandidateEmails());
        Set<String> validEmails = new HashSet<>();
        for (String token : rawTokens) {
            String normalized = token.trim().toLowerCase();
            if (!EMAIL_PATTERN.matcher(normalized).matches()) {
                errors.add(AssessmentValidationError.builder()
                        .field("candidateEmails")
                        .code("INVALID_EMAIL")
                        .message("Invalid email address format: '" + token + "'")
                        .build());
            } else {
                validEmails.add(normalized);
            }
        }

        // 6. Invitation code settings
        if (request.isEnableInvitationCode()) {
            if (request.getInvitationCodeMaxUses() != null && request.getInvitationCodeMaxUses() < 1) {
                errors.add(AssessmentValidationError.builder()
                        .field("invitationCodeMaxUses")
                        .code("OUT_OF_RANGE")
                        .message("Invitation code max uses must be at least 1.")
                        .build());
            }
            if (request.getInvitationCodeExpiresAt() != null) {
                if (request.getInvitationCodeExpiresAt().isBefore(Instant.now())) {
                    errors.add(AssessmentValidationError.builder()
                            .field("invitationCodeExpiresAt")
                            .code("EXPIRED")
                            .message("Invitation code expiration date cannot be in the past.")
                            .build());
                } else if (request.getScheduledEnd() != null && request.getInvitationCodeExpiresAt().isAfter(request.getScheduledEnd())) {
                    warnings.add("Invitation code expires after assessment scheduled end time.");
                }
            }
        }

        return AssessmentValidationResponse.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .bankPreview(bankPreview)
                .calculatedTotalScore(totalPoints)
                .calculatedTotalQuestions(totalQuestions)
                .calculatedDurationMinutes(request.getDurationMinutes())
                .candidateCount(validEmails.size())
                .build();
    }

    @Override
    public QuestionBankPreviewDto getQuestionBankPreview(String bankId) {
        if (cacheService != null && cacheProperties != null) {
            return cacheService.getOrCompute(
                    CacheKeys.assessmentBankPreview(bankId),
                    QuestionBankPreviewDto.class,
                    cacheProperties.detailsTtl(),
                    () -> executeGetQuestionBankPreview(bankId)
            );
        }
        return executeGetQuestionBankPreview(bankId);
    }

    private QuestionBankPreviewDto executeGetQuestionBankPreview(String bankId) {
        QuestionBank bank = questionBankRepository.findById(bankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question Bank not found with ID: " + bankId));
        List<AssessmentValidationError> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        QuestionBankPreviewDto preview = buildQuestionBankPreview(bank, errors, warnings);
        preview.setEligibleForAssessment(errors.isEmpty());
        if (!errors.isEmpty()) {
            preview.setIneligibilityReason(errors.stream().map(AssessmentValidationError::getMessage).collect(Collectors.joining("; ")));
        }
        return preview;
    }

    private QuestionBankPreviewDto buildQuestionBankPreview(QuestionBank bank, List<AssessmentValidationError> errors, List<String> warnings) {
        if (bank.getStatus() != QuestionBankStatus.PUBLISHED) {
            errors.add(AssessmentValidationError.builder()
                    .field("questionBankId")
                    .code("NOT_PUBLISHED")
                    .message("Question Bank '" + bank.getTitle() + "' is in " + bank.getStatus() + " status. Only PUBLISHED banks can be used for assessments.")
                    .build());
        }

        if (bank.getItems() == null || bank.getItems().isEmpty()) {
            errors.add(AssessmentValidationError.builder()
                    .field("questionBankId")
                    .code("EMPTY_BANK")
                    .message("Question Bank '" + bank.getTitle() + "' contains no questions.")
                    .build());
            return QuestionBankPreviewDto.builder()
                    .id(bank.getId())
                    .title(bank.getTitle())
                    .description(bank.getDescription())
                    .category(bank.getCategory())
                    .status(bank.getStatus() != null ? bank.getStatus().name() : null)
                    .totalQuestions(0)
                    .mcqCount(0)
                    .codingCount(0)
                    .totalPoints(0.0)
                    .eligibleForAssessment(false)
                    .ineligibilityReason("Question bank contains no questions.")
                    .build();
        }

        Map<String, Integer> difficultyDistribution = new LinkedHashMap<>();
        difficultyDistribution.put("EASY", 0);
        difficultyDistribution.put("MEDIUM", 0);
        difficultyDistribution.put("HARD", 0);

        Set<String> supportedCodingLanguages = new TreeSet<>();
        List<QuestionBankPreviewItemDto> previewItems = new ArrayList<>();

        int mcqCount = 0;
        int codingCount = 0;
        double totalPoints = 0.0;

        for (QuestionBankItem bankItem : bank.getItems()) {
            List<AssessmentValidationError> itemErrors = new ArrayList<>();
            validateSingleQuestionEligibility(bankItem.getQuestionType(), bankItem.getQuestionId(), itemErrors);
            errors.addAll(itemErrors);

            String title = "Unknown";
            String difficulty = "EASY";
            List<String> topics = List.of();
            List<String> langs = List.of();
            double pts = bankItem.getCustomPoints() != null ? bankItem.getCustomPoints() : getDefaultPoints(bankItem.getQuestionType(), bankItem.getQuestionId());

            if (bankItem.getQuestionType() == BankQuestionType.MCQ) {
                mcqCount++;
                Optional<McqQuestion> mcqOpt = mcqQuestionRepository.findById(bankItem.getQuestionId());
                if (mcqOpt.isPresent()) {
                    McqQuestion mcq = mcqOpt.get();
                    title = mcq.getTitle();
                    difficulty = mcq.getDifficulty() != null ? mcq.getDifficulty().name() : "EASY";
                    topics = mcq.getTopics() != null ? mcq.getTopics() : List.of();
                }
            } else {
                codingCount++;
                Optional<CodingProblem> cpOpt = codingProblemRepository.findById(bankItem.getQuestionId());
                if (cpOpt.isPresent()) {
                    CodingProblem cp = cpOpt.get();
                    title = cp.getTitle();
                    difficulty = cp.getDifficulty() != null ? cp.getDifficulty().name() : "MEDIUM";
                    topics = cp.getTopics() != null ? cp.getTopics() : List.of();
                    langs = cp.getSupportedLanguageIds() != null ? cp.getSupportedLanguageIds() : List.of();
                    supportedCodingLanguages.addAll(langs);
                }
            }

            difficultyDistribution.put(difficulty, difficultyDistribution.getOrDefault(difficulty, 0) + 1);
            totalPoints += pts;

            previewItems.add(QuestionBankPreviewItemDto.builder()
                    .questionId(bankItem.getQuestionId())
                    .questionType(bankItem.getQuestionType())
                    .title(title)
                    .difficulty(difficulty)
                    .points(pts)
                    .topics(topics)
                    .supportedLanguages(langs)
                    .eligible(itemErrors.isEmpty())
                    .ineligibilityReason(itemErrors.isEmpty() ? null : itemErrors.stream().map(AssessmentValidationError::getMessage).collect(Collectors.joining("; ")))
                    .build());
        }

        boolean eligible = errors.isEmpty();
        return QuestionBankPreviewDto.builder()
                .id(bank.getId())
                .title(bank.getTitle())
                .description(bank.getDescription())
                .category(bank.getCategory())
                .status(bank.getStatus() != null ? bank.getStatus().name() : null)
                .totalQuestions(bank.getItems().size())
                .mcqCount(mcqCount)
                .codingCount(codingCount)
                .totalPoints(totalPoints)
                .difficultyDistribution(difficultyDistribution)
                .supportedCodingLanguages(new ArrayList<>(supportedCodingLanguages))
                .items(previewItems)
                .eligibleForAssessment(eligible)
                .ineligibilityReason(eligible ? null : errors.stream().map(AssessmentValidationError::getMessage).collect(Collectors.joining("; ")))
                .build();
    }

    private void validateSingleQuestionEligibility(BankQuestionType type, String questionId, List<AssessmentValidationError> errors) {
        if (type == BankQuestionType.MCQ) {
            Optional<McqQuestion> mcqOpt = mcqQuestionRepository.findById(questionId);
            if (mcqOpt.isEmpty()) {
                errors.add(AssessmentValidationError.builder()
                        .field("questions")
                        .code("NOT_FOUND")
                        .questionId(questionId)
                        .message("MCQ question not found: " + questionId)
                        .build());
                return;
            }
            McqQuestion mcq = mcqOpt.get();
            if (mcq.getStatus() != McqStatus.PUBLISHED) {
                errors.add(AssessmentValidationError.builder()
                        .field("questions")
                        .code("NOT_PUBLISHED")
                        .questionId(questionId)
                        .message("MCQ '" + mcq.getTitle() + "' is not PUBLISHED (current status: " + mcq.getStatus() + ").")
                        .build());
            }
            if (mcq.getOptions() == null || mcq.getOptions().size() < 2) {
                errors.add(AssessmentValidationError.builder()
                        .field("questions")
                        .code("INVALID_OPTIONS")
                        .questionId(questionId)
                        .message("MCQ '" + mcq.getTitle() + "' must have at least 2 options.")
                        .build());
            } else {
                for (McqOption opt : mcq.getOptions()) {
                    if (opt == null || !StringUtils.hasText(opt.getText())) {
                        errors.add(AssessmentValidationError.builder()
                                .field("questions")
                                .code("BLANK_OPTION")
                                .questionId(questionId)
                                .message("MCQ '" + mcq.getTitle() + "' contains an option with blank text.")
                                .build());
                        break;
                    }
                }
            }
            if (mcq.getCorrectOptionIds() == null || mcq.getCorrectOptionIds().isEmpty()) {
                errors.add(AssessmentValidationError.builder()
                        .field("questions")
                        .code("NO_CORRECT_OPTION")
                        .questionId(questionId)
                        .message("MCQ '" + mcq.getTitle() + "' does not configure any correct answer options.")
                        .build());
            } else if (mcq.getOptions() != null) {
                Set<String> validIds = mcq.getOptions().stream().map(McqOption::getId).collect(Collectors.toSet());
                for (String correctId : mcq.getCorrectOptionIds()) {
                    if (!validIds.contains(correctId)) {
                        errors.add(AssessmentValidationError.builder()
                                .field("questions")
                                .code("INVALID_CORRECT_OPTION")
                                .questionId(questionId)
                                .message("MCQ '" + mcq.getTitle() + "' has correct option ID '" + correctId + "' that is not in its options list.")
                                .build());
                    }
                }
                if (mcq.getType() == McqType.SINGLE_CHOICE && mcq.getCorrectOptionIds().size() != 1) {
                    errors.add(AssessmentValidationError.builder()
                            .field("questions")
                            .code("INVALID_SINGLE_CHOICE")
                            .questionId(questionId)
                            .message("Single-choice MCQ '" + mcq.getTitle() + "' must have exactly one correct option.")
                            .build());
                }
            }
        } else {
            Optional<CodingProblem> cpOpt = codingProblemRepository.findById(questionId);
            if (cpOpt.isEmpty()) {
                errors.add(AssessmentValidationError.builder()
                        .field("questions")
                        .code("NOT_FOUND")
                        .questionId(questionId)
                        .message("Coding problem not found: " + questionId)
                        .build());
                return;
            }
            CodingProblem cp = cpOpt.get();
            if (cp.getStatus() != CodingProblemStatus.PUBLISHED) {
                errors.add(AssessmentValidationError.builder()
                        .field("questions")
                        .code("NOT_PUBLISHED")
                        .questionId(questionId)
                        .message("Coding problem '" + cp.getTitle() + "' is not PUBLISHED (current status: " + cp.getStatus() + ").")
                        .build());
            }
            if (cp.getSupportedLanguageIds() == null || cp.getSupportedLanguageIds().isEmpty()) {
                errors.add(AssessmentValidationError.builder()
                        .field("questions")
                        .code("NO_SUPPORTED_LANGUAGES")
                        .questionId(questionId)
                        .message("Coding problem '" + cp.getTitle() + "' must support at least one programming language.")
                        .build());
            }
            boolean hasSample = cp.getSampleTestCases() != null && !cp.getSampleTestCases().isEmpty();
            boolean hasHidden = cp.getHiddenTestCases() != null && !cp.getHiddenTestCases().isEmpty();
            if (!hasSample && !hasHidden) {
                errors.add(AssessmentValidationError.builder()
                        .field("questions")
                        .code("NO_TESTCASES")
                        .questionId(questionId)
                        .message("Coding problem '" + cp.getTitle() + "' must have at least one testcase configured.")
                        .build());
            }
            if (!StringUtils.hasText(cp.getReferenceSolution())) {
                errors.add(AssessmentValidationError.builder()
                        .field("questions")
                        .code("NO_REFERENCE_SOLUTION")
                        .questionId(questionId)
                        .message("Coding problem '" + cp.getTitle() + "' does not have a reference solution.")
                        .build());
            }
            if (!StringUtils.hasText(cp.getReferenceSolutionLanguage()) ||
                    (cp.getSupportedLanguageIds() != null && !cp.getSupportedLanguageIds().contains(cp.getReferenceSolutionLanguage()))) {
                errors.add(AssessmentValidationError.builder()
                        .field("questions")
                        .code("INVALID_REFERENCE_LANGUAGE")
                        .questionId(questionId)
                        .message("Coding problem '" + cp.getTitle() + "' reference solution language is invalid or not in supported languages.")
                        .build());
            }
            if (cp.isRequireVerificationScript()) {
                if (!StringUtils.hasText(cp.getVerificationScript()) || !StringUtils.hasText(cp.getVerificationScriptLanguage())) {
                    errors.add(AssessmentValidationError.builder()
                            .field("questions")
                            .code("INVALID_VERIFICATION_SCRIPT")
                            .questionId(questionId)
                            .message("Coding problem '" + cp.getTitle() + "' requires a verification script, but none is provided.")
                            .build());
                }
            }
        }
    }

    @Override
    public PageResponse<AssessmentSummaryResponse> getAssessments(AssessmentFilterRequest filter) {
        if (filter == null) {
            filter = new AssessmentFilterRequest();
        }

        if (filter.isDefault() && cacheService != null && cacheProperties != null) {
            final AssessmentFilterRequest effectiveFilter = filter;
            return cacheService.getOrCompute(
                    CacheKeys.assessmentDefaultPage(),
                    new TypeReference<PageResponse<AssessmentSummaryResponse>>() {},
                    cacheProperties.catalogPageTtl(),
                    () -> executeGetAssessments(effectiveFilter)
            );
        }

        return executeGetAssessments(filter);
    }

    private PageResponse<AssessmentSummaryResponse> executeGetAssessments(AssessmentFilterRequest filter) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (filter.getStatus() != null) {
            Instant now = Instant.now();
            switch (filter.getStatus()) {
                case COMPLETED -> criteriaList.add(new Criteria().orOperator(
                        Criteria.where("status").is(AssessmentStatus.COMPLETED),
                        Criteria.where("scheduledEnd").lte(now)
                ));
                case NOT_STARTED -> criteriaList.add(new Criteria().andOperator(
                        Criteria.where("scheduledStart").gt(now),
                        new Criteria().orOperator(
                                Criteria.where("scheduledEnd").exists(false),
                                Criteria.where("scheduledEnd").gt(now)
                        )
                ));
                case ACTIVE -> criteriaList.add(new Criteria().andOperator(
                        new Criteria().orOperator(
                                Criteria.where("scheduledStart").exists(false),
                                Criteria.where("scheduledStart").is(null),
                                Criteria.where("scheduledStart").lte(now)
                        ),
                        new Criteria().orOperator(
                                Criteria.where("scheduledEnd").exists(false),
                                Criteria.where("scheduledEnd").is(null),
                                Criteria.where("scheduledEnd").gt(now)
                        )
                ));
            }
        }

        if (StringUtils.hasText(filter.getSearch())) {
            String term = filter.getSearch().trim();
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(term, "i"),
                    Criteria.where("slug").regex(term, "i")
            ));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, Assessment.class);

        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortBy = StringUtils.hasText(filter.getSortBy()) ? filter.getSortBy() : "createdAt";

        int page = Math.max(0, filter.getPage());
        int size = filter.getSize() > 0 ? filter.getSize() : 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        query.with(pageable);
        List<Assessment> list = mongoTemplate.find(query, Assessment.class);

        List<AssessmentSummaryResponse> content = list.stream()
                .map(AssessmentSummaryResponse::fromModel)
                .toList();

        return PageResponse.of(content, page, size, total);
    }

    @Override
    public AssessmentDetailResponse getAssessment(String idOrSlug) {
        if (cacheService != null && cacheProperties != null) {
            return cacheService.getOrCompute(
                    CacheKeys.assessmentDetail(idOrSlug),
                    AssessmentDetailResponse.class,
                    cacheProperties.detailsTtl(),
                    () -> executeGetAssessment(idOrSlug)
            );
        }
        return executeGetAssessment(idOrSlug);
    }

    private AssessmentDetailResponse executeGetAssessment(String idOrSlug) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        return AssessmentDetailResponse.fromModel(assessment);
    }

    @Override
    @Transactional
    public void deleteAssessment(String idOrSlug) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        if (assessment.getEffectiveStatus() == AssessmentStatus.ACTIVE) {
            throw new BadRequestException("Cannot delete an ACTIVE assessment.");
        }

        long activeAttempts = attemptRepository.countByAssessmentIdAndStatus(assessment.getId(), AttemptStatus.IN_PROGRESS);
        if (activeAttempts > 0) {
            throw new BadRequestException("Cannot delete assessment with active in-progress attempts.");
        }

        // Delete associated records
        List<AssessmentAssignment> assignments = assignmentRepository.findAllByAssessmentId(assessment.getId());
        assignmentRepository.deleteAll(assignments);

        assessmentRepository.delete(assessment);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateAssessment(assessment.getId(), assessment.getSlug());
        }
        publishAnalytics(AnalyticsEventType.ASSESSMENT_DELETED,
                assessment.getId(),
                AnalyticsEntityType.ASSESSMENT,
                assessment.getId(),
                null,
                AnalyticsActorType.ADMIN,
                null,
                assessmentAttributes(assessment));
        log.info("Deleted Assessment: [{}] (id: {})", assessment.getTitle(), assessment.getId());
    }

    @Override
    @Transactional
    public AssessmentDetailResponse activateAssessment(String idOrSlug) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        Instant now = Instant.now();

        AssessmentStatus currentEffectiveStatus = assessment.getEffectiveStatus(now);
        if (currentEffectiveStatus == AssessmentStatus.ACTIVE) {
            throw new BadRequestException("Assessment is already ACTIVE.");
        }
        if (currentEffectiveStatus == AssessmentStatus.COMPLETED) {
            throw new BadRequestException("Assessment is already COMPLETED and cannot be activated.");
        }
        if (assessment.isManuallyActivated()) {
            throw new BadRequestException("Assessment has already been manually activated.");
        }
        if (assessment.getItems() == null || assessment.getItems().isEmpty()) {
            throw new BadRequestException("Cannot activate an assessment with no questions configured.");
        }

        assessment.setManuallyActivated(true);
        assessment.setManuallyActivatedAt(now);
        if (assessment.getScheduledStart() == null || assessment.getScheduledStart().isAfter(now)) {
            assessment.setScheduledStart(now);
        }
        assessment.setStatus(AssessmentStatus.ACTIVE);
        String interventionMsg = "The administrator has manually activated this assessment.";
        assessment.setManualInterventionMessage(interventionMsg);
        assessment.setUpdatedAt(now);
        Assessment saved = assessmentRepository.save(assessment);

        recordAuditEvent(saved.getId(), null, null, null,
                null, AssessmentAuditEventType.MANUAL_ACTIVATION, AssessmentAuditEventSource.SERVER_VERIFIED,
                Map.of("action", "MANUAL_ACTIVATION", "timestamp", now.toString(), "overrodeStart", true));

        if (webSocketHandler != null) {
            webSocketHandler.broadcastToAssessment(saved.getId(), Map.of(
                    "type", "ASSESSMENT_MANUALLY_ACTIVATED",
                    "assessmentId", saved.getId(),
                    "status", AssessmentStatus.ACTIVE.name(),
                    "message", interventionMsg,
                    "timestamp", now.toString()
            ));
        }

        log.info("Admin manually activated Assessment: [{}] (id: {}) at {}", saved.getTitle(), saved.getId(), now);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateAssessment(saved.getId(), saved.getSlug());
        }
        Map<String, Object> actAttrs = assessmentAttributes(saved);
        actAttrs.put("isManual", true);
        publishAnalytics(AnalyticsEventType.ASSESSMENT_ACTIVATED,
                saved.getId(),
                AnalyticsEntityType.ASSESSMENT,
                saved.getId(),
                null,
                AnalyticsActorType.ADMIN,
                null,
                actAttrs);
        return AssessmentDetailResponse.fromModel(saved);
    }

    @Override
    @Transactional
    public AssessmentDetailResponse completeAssessment(String idOrSlug) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        Instant now = Instant.now();

        if (assessment.getEffectiveStatus(now) == AssessmentStatus.COMPLETED || assessment.isManuallyCompleted()) {
            throw new BadRequestException("Assessment is already COMPLETED.");
        }

        assessment.setManuallyCompleted(true);
        assessment.setManuallyCompletedAt(now);
        assessment.setScheduledEnd(now);
        assessment.setStatus(AssessmentStatus.COMPLETED);
        String interventionMsg = "The administrator has manually completed this assessment.";
        assessment.setManualInterventionMessage(interventionMsg);
        assessment.setUpdatedAt(now);
        Assessment saved = assessmentRepository.save(assessment);

        // Terminate any in-progress attempts whose effective deadline is now capped to 'now'
        List<AssessmentAttempt> activeAttempts = attemptRepository.findAllByAssessmentIdAndStatus(saved.getId(), AttemptStatus.IN_PROGRESS);
        for (AssessmentAttempt attempt : activeAttempts) {
            if (attempt.getEffectiveDeadline() == null || attempt.getEffectiveDeadline().isAfter(now)) {
                attempt.setEffectiveDeadline(now);
            }
            expireAttempt(attempt, saved, now);
        }

        recordAuditEvent(saved.getId(), null, null, null,
                null, AssessmentAuditEventType.MANUAL_COMPLETION, AssessmentAuditEventSource.SERVER_VERIFIED,
                Map.of("action", "MANUAL_COMPLETION", "timestamp", now.toString(), "terminatedActiveAttempts", activeAttempts.size()));

        if (webSocketHandler != null) {
            webSocketHandler.broadcastToAssessment(saved.getId(), Map.of(
                    "type", "ASSESSMENT_MANUALLY_COMPLETED",
                    "assessmentId", saved.getId(),
                    "status", AssessmentStatus.COMPLETED.name(),
                    "message", interventionMsg,
                    "timestamp", now.toString()
            ));
        }

        log.info("Admin manually completed Assessment: [{}] (id: {}) at {}", saved.getTitle(), saved.getId(), now);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateAssessment(saved.getId(), saved.getSlug());
        }
        Map<String, Object> compAttrs = assessmentAttributes(saved);
        compAttrs.put("isManual", true);
        publishAnalytics(AnalyticsEventType.ASSESSMENT_COMPLETED,
                saved.getId(),
                AnalyticsEntityType.ASSESSMENT,
                saved.getId(),
                null,
                AnalyticsActorType.ADMIN,
                null,
                compAttrs);
        return AssessmentDetailResponse.fromModel(saved);
    }

    @Override
    @Transactional
    public BulkAssignCandidatesResponse bulkAssignCandidates(String idOrSlug, BulkAssignCandidatesRequest request) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentStatus effectiveStatus = assessment.getEffectiveStatus();
        if (effectiveStatus == AssessmentStatus.COMPLETED) {
            throw new BadRequestException("Candidate enrollment is closed. The assessment has already ended.");
        }
        if (assessment.isManuallyActivated() || effectiveStatus != AssessmentStatus.NOT_STARTED) {
            throw new BadRequestException("Candidate roster is frozen. Candidates cannot be added after the assessment has been activated or started.");
        }

        Set<String> validEmails = new HashSet<>();
        List<String> invalidEmails = new ArrayList<>();

        Set<String> rawTokens = parseRawTokens(request.getEmailsRaw(), request.getEmails());
        for (String token : rawTokens) {
            String normalized = token.trim().toLowerCase();
            if (EMAIL_PATTERN.matcher(normalized).matches()) {
                validEmails.add(normalized);
            } else {
                invalidEmails.add(token);
            }
        }

        List<String> assignedEmails = new ArrayList<>();
        int duplicatesSkipped = 0;

        for (String email : validEmails) {
            if (assignmentRepository.existsByAssessmentIdAndCandidateEmail(assessment.getId(), email)) {
                duplicatesSkipped++;
                continue;
            }

            Optional<User> userOpt = userRepository.findByEmail(email);
            AssessmentAssignment assignment = AssessmentAssignment.builder()
                    .assessmentId(assessment.getId())
                    .candidateEmail(email)
                    .candidateUserId(userOpt.map(User::getId).orElse(null))
                    .status(CandidateParticipationStatus.INVITED)
                    .attemptsCount(0)
                    .invitedAt(Instant.now())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            assignmentRepository.save(assignment);
            assignedEmails.add(email);
        }

        int totalAssigned = (int) assignmentRepository.countByAssessmentId(assessment.getId());
        assessment.setAssignedCandidateCount(totalAssigned);
        assessment.setUpdatedAt(Instant.now());
        assessmentRepository.save(assessment);

        log.info("Bulk assigned candidates to assessment [{}]: {} new, {} skipped, {} invalid",
                assessment.getTitle(), assignedEmails.size(), duplicatesSkipped, invalidEmails.size());

        for (String email : assignedEmails) {
            publishAnalytics(AnalyticsEventType.CANDIDATE_INVITED,
                    assessment.getId() + ":" + email,
                    AnalyticsEntityType.ASSESSMENT_ASSIGNMENT,
                    assessment.getId(),
                    email,
                    AnalyticsActorType.ADMIN,
                    null,
                    Map.of("email", email));
        }

        return BulkAssignCandidatesResponse.builder()
                .totalProcessed(rawTokens.size())
                .successfullyAssigned(assignedEmails.size())
                .duplicatesSkipped(duplicatesSkipped)
                .invalidEmails(invalidEmails)
                .assignedEmails(assignedEmails)
                .build();
    }

    @Override
    public PageResponse<CandidateAssignmentReportResponse> getCandidateAssignments(String idOrSlug, Pageable pageable) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        Page<AssessmentAssignment> page = assignmentRepository.findAllByAssessmentId(assessment.getId(), pageable);

        List<CandidateAssignmentReportResponse> content = page.getContent().stream()
                .map(a -> {
                    Double latestScore = null;
                    if (a.getLastAttemptId() != null) {
                        latestScore = attemptRepository.findById(a.getLastAttemptId())
                                .map(AssessmentAttempt::getScore)
                                .orElse(null);
                    }
                    return CandidateAssignmentReportResponse.fromModel(a, latestScore);
                })
                .toList();

        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public PageResponse<AttemptReportResponse> getAssessmentAttempts(String idOrSlug, Pageable pageable) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        Page<AssessmentAttempt> page = attemptRepository.findAllByAssessmentId(assessment.getId(), pageable);

        List<AttemptReportResponse> content = page.getContent().stream()
                .map(att -> {
                    long flagCount = connectionEventRepository.countByAttemptIdAndIsIntegrityFlagTrue(att.getId());
                    return AttemptReportResponse.fromModel(att, flagCount);
                })
                .toList();

        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public PageResponse<AssessmentConnectionEvent> getAttemptConnectionEvents(String attemptId, Pageable pageable) {
        Page<AssessmentConnectionEvent> page = connectionEventRepository.findAllByAttemptIdOrderByTimestampDesc(attemptId, pageable);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional
    public AssessmentDetailResponse generateInvitationCode(String idOrSlug) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        String code = generateSecureInvitationCode();
        assessment.setInvitationCode(code);
        assessment.setInvitationCodeEnabled(true);
        if (assessment.getInvitationCodeExpiresAt() == null) {
            assessment.setInvitationCodeExpiresAt(assessment.getScheduledEnd());
        }
        assessment.setUpdatedAt(Instant.now());
        Assessment saved = assessmentRepository.save(assessment);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateAssessment(saved.getId(), saved.getSlug());
        }
        log.info("Generated new invitation code [{}] for assessment [{}]", code, saved.getId());
        return AssessmentDetailResponse.fromModel(saved);
    }

    @Override
    @Transactional
    public AssessmentDetailResponse revokeInvitationCode(String idOrSlug) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        assessment.setInvitationCodeEnabled(false);
        assessment.setUpdatedAt(Instant.now());
        Assessment saved = assessmentRepository.save(assessment);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateAssessment(saved.getId(), saved.getSlug());
        }
        log.info("Revoked invitation code for assessment [{}]", saved.getId());
        return AssessmentDetailResponse.fromModel(saved);
    }

    // ==========================================
    // Candidate Operations
    // ==========================================

    @Override
    @Transactional
    public CandidateAssessmentSummaryResponse joinByInvitationCode(String candidateUserId, String candidateEmail, JoinByCodeRequest request) {
        if (request == null || !StringUtils.hasText(request.getCode())) {
            throw new BadRequestException("Invitation code cannot be blank.");
        }
        String cleanCode = request.getCode().trim().toUpperCase();
        Assessment assessment = assessmentRepository.findByInvitationCode(cleanCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation Code is invalid or does not exist: " + cleanCode));

        if (!assessment.isInvitationCodeEnabled()) {
            throw new BadRequestException("This invitation code has been disabled or revoked.");
        }

        Instant now = Instant.now();
        if (assessment.getInvitationCodeExpiresAt() != null && now.isAfter(assessment.getInvitationCodeExpiresAt())) {
            throw new BadRequestException("This invitation code has expired.");
        }

        AssessmentStatus effectiveStatus = assessment.getEffectiveStatus(now);
        if (effectiveStatus == AssessmentStatus.COMPLETED) {
            throw new BadRequestException("Candidate registration is closed. The assessment has already ended.");
        }

        if (assessment.getInvitationCodeMaxUses() != null &&
                assessment.getInvitationCodeUsedCount() >= assessment.getInvitationCodeMaxUses()) {
            throw new BadRequestException("This invitation code has reached its maximum allowed uses.");
        }

        String normalizedEmail = candidateEmail != null ? candidateEmail.trim().toLowerCase() : "";

        // Check if candidate is already assigned (idempotent registration)
        Optional<AssessmentAssignment> existingByEmail = StringUtils.hasText(normalizedEmail) ?
                assignmentRepository.findByAssessmentIdAndCandidateEmail(assessment.getId(), normalizedEmail) : Optional.empty();

        Optional<AssessmentAssignment> existingByUser = StringUtils.hasText(candidateUserId) ?
                assignmentRepository.findByAssessmentIdAndCandidateUserId(assessment.getId(), candidateUserId) : Optional.empty();

        AssessmentAssignment assignment;
        if (existingByEmail.isPresent()) {
            assignment = existingByEmail.get();
            if (StringUtils.hasText(candidateUserId) && !candidateUserId.equals(assignment.getCandidateUserId())) {
                assignment.setCandidateUserId(candidateUserId);
                assignment = assignmentRepository.save(assignment);
            }
        } else if (existingByUser.isPresent()) {
            assignment = existingByUser.get();
        } else {
            // New registration - check roster freeze
            if (assessment.isManuallyActivated() || effectiveStatus == AssessmentStatus.COMPLETED) {
                throw new BadRequestException("Candidate roster is frozen. New candidates cannot join after the assessment has been activated or ended.");
            }
            // New registration
            assignment = AssessmentAssignment.builder()
                    .assessmentId(assessment.getId())
                    .candidateEmail(normalizedEmail)
                    .candidateUserId(candidateUserId)
                    .status(CandidateParticipationStatus.INVITED)
                    .attemptsCount(0)
                    .invitedAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            assignment = assignmentRepository.save(assignment);

            assessment.setAssignedCandidateCount(assessment.getAssignedCandidateCount() + 1);
            assessment.setInvitationCodeUsedCount(assessment.getInvitationCodeUsedCount() + 1);
            assessment.setUpdatedAt(now);
            assessmentRepository.save(assessment);
            publishAnalytics(AnalyticsEventType.CANDIDATE_ENROLLED,
                    assignment.getId(),
                    AnalyticsEntityType.ASSESSMENT_ASSIGNMENT,
                    assessment.getId(),
                    normalizedEmail,
                    AnalyticsActorType.CANDIDATE,
                    candidateUserId,
                    Map.of("code", cleanCode, "email", normalizedEmail));
            log.info("Candidate [{}] successfully joined assessment [{}] via code [{}]",
                    candidateEmail, assessment.getTitle(), cleanCode);
        }

        return buildCandidateAssessmentSummary(assessment, assignment, candidateUserId);
    }

    @Override
    public AssessmentReadinessResponse getReadiness(String idOrSlug, String candidateUserId, String candidateEmail) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAssignment assignment = null;
        try {
            assignment = findAssignment(assessment.getId(), candidateUserId, candidateEmail);
        } catch (BadRequestException ex) {
            // Candidate not yet assigned
        }

        Instant now = Instant.now();
        CandidateAssessmentSummaryResponse summary = buildCandidateAssessmentSummary(assessment, assignment, candidateUserId);

        boolean hasActive = false;
        String activeAttemptId = null;
        Instant effectiveDeadline = null;
        Long remainingSeconds = null;

        if (StringUtils.hasText(candidateUserId)) {
            Optional<AssessmentAttempt> activeAttempt = attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus(
                    assessment.getId(), candidateUserId, AttemptStatus.IN_PROGRESS
            );
            if (activeAttempt.isPresent()) {
                AssessmentAttempt att = activeAttempt.get();
                if (now.isAfter(att.getEffectiveDeadline())) {
                    // Lazy on-demand expiration: evaluate and expire right now!
                    expireAttempt(att, assessment, now);
                    hasActive = false;
                    activeAttemptId = null;
                    effectiveDeadline = null;
                    remainingSeconds = null;
                } else {
                    hasActive = true;
                    activeAttemptId = att.getId();
                    effectiveDeadline = att.getEffectiveDeadline();
                    remainingSeconds = Math.max(0, Duration.between(now, att.getEffectiveDeadline()).toSeconds());
                }
            }
        }

        return AssessmentReadinessResponse.builder()
                .authenticated(StringUtils.hasText(candidateUserId))
                .candidateUserId(candidateUserId)
                .candidateEmail(candidateEmail)
                .assessmentId(assessment.getId())
                .slug(assessment.getSlug())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .assessmentStatus(summary.getAssessmentStatus())
                .scheduledStart(assessment.getScheduledStart())
                .scheduledEnd(assessment.getScheduledEnd())
                .durationMinutes(assessment.getDurationMinutes())
                .maxAttempts(assessment.getMaxAttempts())
                .attemptsUsed(summary.getAttemptsUsed())
                .participationStatus(summary.getAssignmentStatus())
                .canStart(summary.isCanStart())
                .cannotStartReason(summary.getCannotStartReason())
                .activeAttemptId(activeAttemptId)
                .hasActiveAttempt(hasActive)
                .effectiveDeadline(effectiveDeadline)
                .remainingSeconds(remainingSeconds)
                .totalQuestions(assessment.getTotalQuestions())
                .totalScore(assessment.getTotalScore())
                .manuallyActivated(assessment.isManuallyActivated())
                .manuallyCompleted(assessment.isManuallyCompleted())
                .manualInterventionMessage(assessment.getManualInterventionMessage())
                .backendReady(true)
                .serverTimestamp(now)
                .build();
    }

    @Override
    public List<CandidateAssessmentSummaryResponse> getCandidateAssessments(String candidateUserId, String candidateEmail) {
        Set<String> assessmentIds = new HashSet<>();
        Map<String, AssessmentAssignment> assignmentMap = new HashMap<>();

        if (StringUtils.hasText(candidateUserId)) {
            for (AssessmentAssignment a : assignmentRepository.findAllByCandidateUserId(candidateUserId)) {
                assessmentIds.add(a.getAssessmentId());
                assignmentMap.put(a.getAssessmentId(), a);
            }
        }
        if (StringUtils.hasText(candidateEmail)) {
            for (AssessmentAssignment a : assignmentRepository.findAllByCandidateEmail(candidateEmail.toLowerCase())) {
                assessmentIds.add(a.getAssessmentId());
                assignmentMap.putIfAbsent(a.getAssessmentId(), a);
            }
        }

        if (assessmentIds.isEmpty()) {
            return List.of();
        }

        List<Assessment> assessments = assessmentRepository.findAllById(assessmentIds);
        List<CandidateAssessmentSummaryResponse> results = new ArrayList<>();

        for (Assessment assessment : assessments) {
            AssessmentAssignment assignment = assignmentMap.get(assessment.getId());
            results.add(buildCandidateAssessmentSummary(assessment, assignment, candidateUserId));
        }

        return results;
    }

    @Override
    public CandidateAssessmentSummaryResponse getCandidateAssessmentSummary(String idOrSlug, String candidateUserId, String candidateEmail) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAssignment assignment = findAssignment(assessment.getId(), candidateUserId, candidateEmail);
        return buildCandidateAssessmentSummary(assessment, assignment, candidateUserId);
    }

    @Override
    @Transactional
    public StartAttemptResponse startAttempt(String idOrSlug, String candidateUserId, String candidateEmail) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAssignment assignment = findAssignment(assessment.getId(), candidateUserId, candidateEmail);

        Instant now = Instant.now();

        // Check if an IN_PROGRESS attempt already exists (safe resumption)
        Optional<AssessmentAttempt> activeAttemptOpt = attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus(
                assessment.getId(), candidateUserId, AttemptStatus.IN_PROGRESS
        );

        if (activeAttemptOpt.isPresent()) {
            AssessmentAttempt existingAttempt = activeAttemptOpt.get();
            // Check if deadline has passed
            if (now.isAfter(existingAttempt.getEffectiveDeadline())) {
                expireAttempt(existingAttempt, assessment, now);
                throw new BadRequestException("Assessment attempt has expired.");
            }

            long remainingSeconds = Math.max(0, Duration.between(now, existingAttempt.getEffectiveDeadline()).toSeconds());
            return StartAttemptResponse.builder()
                    .attemptId(existingAttempt.getId())
                    .assessmentId(assessment.getId())
                    .attemptNumber(existingAttempt.getAttemptNumber())
                    .status(existingAttempt.getStatus())
                    .startedAt(existingAttempt.getStartedAt())
                    .effectiveDeadline(existingAttempt.getEffectiveDeadline())
                    .remainingSeconds(remainingSeconds)
                    .build();
        }

        // Validate eligibility to start a new attempt
        AssessmentStatus effectiveStatus = assessment.getEffectiveStatus(now);
        if (effectiveStatus == AssessmentStatus.NOT_STARTED) {
            throw new BadRequestException("Assessment has not started yet. Scheduled start: " + assessment.getScheduledStart());
        }

        if (effectiveStatus == AssessmentStatus.COMPLETED) {
            throw new BadRequestException("Assessment has already ended. Scheduled end: " + assessment.getScheduledEnd());
        }

        if (effectiveStatus != AssessmentStatus.ACTIVE) {
            throw new BadRequestException("Assessment is not currently active.");
        }

        if (assessment.getMaxAttempts() != null && assignment.getAttemptsCount() >= assessment.getMaxAttempts()) {
            throw new BadRequestException("Maximum attempts reached (" + assessment.getMaxAttempts() + ").");
        }

        // Compute effective deadline: min(now + duration, scheduledEnd)
        Instant attemptDeadline = now.plus(Duration.ofMinutes(assessment.getDurationMinutes()));
        Instant effectiveDeadline = assessment.getScheduledEnd() != null && assessment.getScheduledEnd().isBefore(attemptDeadline)
                ? assessment.getScheduledEnd()
                : attemptDeadline;

        int nextAttemptNumber = assignment.getAttemptsCount() + 1;

        AssessmentAttempt newAttempt = AssessmentAttempt.builder()
                .assessmentId(assessment.getId())
                .assignmentId(assignment.getId())
                .candidateUserId(candidateUserId)
                .candidateEmail(assignment.getCandidateEmail())
                .attemptNumber(nextAttemptNumber)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(now)
                .effectiveDeadline(effectiveDeadline)
                .maxPossibleScore(assessment.getTotalScore())
                .createdAt(now)
                .updatedAt(now)
                .build();

        AssessmentAttempt savedAttempt = attemptRepository.save(newAttempt);

        publishAnalytics(AnalyticsEventType.ATTEMPT_STARTED,
                savedAttempt.getId(),
                AnalyticsEntityType.ASSESSMENT_ATTEMPT,
                assessment.getId(),
                assignment.getCandidateEmail(),
                AnalyticsActorType.CANDIDATE,
                candidateUserId,
                Map.of("attemptNumber", nextAttemptNumber));

        recordAuditEvent(assessment.getId(), savedAttempt.getId(), candidateUserId, assignment.getCandidateEmail(),
                null, AssessmentAuditEventType.SESSION_STARTED, AssessmentAuditEventSource.SERVER_VERIFIED,
                Map.of("attemptNumber", nextAttemptNumber, "durationMinutes", assessment.getDurationMinutes()));

        // Update assignment
        assignment.setStatus(CandidateParticipationStatus.IN_PROGRESS);
        assignment.setAttemptsCount(nextAttemptNumber);
        assignment.setLastAttemptId(savedAttempt.getId());
        if (assignment.getStartedAt() == null) {
            assignment.setStartedAt(now);
        }
        if (assignment.getCandidateUserId() == null) {
            assignment.setCandidateUserId(candidateUserId);
        }
        assignment.setUpdatedAt(now);
        assignmentRepository.save(assignment);

        long remainingSeconds = Math.max(0, Duration.between(now, effectiveDeadline).toSeconds());
        log.info("Candidate [{}] started attempt #{} for assessment [{}] (id: {}) with deadline {}",
                candidateUserId, nextAttemptNumber, assessment.getTitle(), savedAttempt.getId(), effectiveDeadline);

        return StartAttemptResponse.builder()
                .attemptId(savedAttempt.getId())
                .assessmentId(assessment.getId())
                .attemptNumber(nextAttemptNumber)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(now)
                .effectiveDeadline(effectiveDeadline)
                .remainingSeconds(remainingSeconds)
                .build();
    }

    @Override
    public CandidateAttemptDetailResponse getActiveAttemptDetail(String idOrSlug, String candidateUserId) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAttempt attempt = attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus(
                assessment.getId(), candidateUserId, AttemptStatus.IN_PROGRESS
        ).orElseThrow(() -> new ResourceNotFoundException("Active assessment attempt not found."));

        Instant now = Instant.now();
        if (now.isAfter(attempt.getEffectiveDeadline())) {
            expireAttempt(attempt, assessment, now);
            throw new BadRequestException("Assessment attempt has expired.");
        }

        long remainingSeconds = Math.max(0, Duration.between(now, attempt.getEffectiveDeadline()).toSeconds());

        // Sanitized questions (hide hidden testcases and correct options from candidates)
        List<AssessmentItemResponse> candidateQuestions = assessment.getItems().stream()
                .map(item -> AssessmentItemResponse.fromModel(item, false))
                .toList();

        // Fetch saved responses
        List<AssessmentQuestionResponse> savedList = responseRepository.findAllByAttemptId(attempt.getId());
        Map<String, SavedQuestionResponseDto> responsesMap = new HashMap<>();
        for (AssessmentQuestionResponse resp : savedList) {
            responsesMap.put(resp.getQuestionId(), SavedQuestionResponseDto.builder()
                    .questionId(resp.getQuestionId())
                    .questionType(resp.getQuestionType())
                    .selectedOptionIds(resp.getSelectedOptionIds())
                    .languageId(resp.getLanguageId())
                    .code(resp.getCode())
                    .lastExecutionResult(resp.getLastExecutionResult())
                    .allTestcasesPassed(resp.getAllTestcasesPassed())
                    .markedForReview(resp.isMarkedForReview())
                    .savedAt(resp.getSavedAt())
                    .awardedPoints(resp.getAwardedPoints())
                    .bestAwardedPoints(resp.getBestAwardedPoints())
                    .maxPoints(resp.getMaxPoints())
                    .passedTestcases(resp.getBestPassedTestcases())
                    .totalTestcases(resp.getTotalTestcases())
                    .submissions(resp.getSubmissions() != null ? resp.getSubmissions() : List.of())
                    .build());
        }

        return CandidateAttemptDetailResponse.builder()
                .attemptId(attempt.getId())
                .assessmentId(assessment.getId())
                .slug(assessment.getSlug())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .attemptNumber(attempt.getAttemptNumber())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .effectiveDeadline(attempt.getEffectiveDeadline())
                .remainingSeconds(remainingSeconds)
                .totalQuestions(assessment.getTotalQuestions())
                .totalScore(assessment.getTotalScore())
                .manuallyCompleted(assessment.isManuallyCompleted())
                .manualInterventionMessage(assessment.getManualInterventionMessage())
                .questions(candidateQuestions)
                .responses(responsesMap)
                .build();
    }

    @Override
    @Transactional
    public SavedQuestionResponseDto saveMcqResponse(String idOrSlug, String candidateUserId, SaveMcqResponseRequest request) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAttempt attempt = getActiveAndValidAttempt(assessment.getId(), candidateUserId);

        AssessmentItem item = assessment.getItems().stream()
                .filter(i -> i.getQuestionId().equals(request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Question ID " + request.getQuestionId() + " not found in assessment."));

        if (item.getQuestionType() != BankQuestionType.MCQ) {
            throw new BadRequestException("Question " + request.getQuestionId() + " is not an MCQ.");
        }

        QuestionSnapshot snap = item.getSnapshot();
        List<String> validOptionIds = snap.getOptions() != null ?
                snap.getOptions().stream().map(OptionSnapshot::getId).toList() : List.of();

        List<String> selected = request.getSelectedOptionIds() != null ? request.getSelectedOptionIds() : List.of();
        for (String optId : selected) {
            if (!validOptionIds.contains(optId)) {
                throw new BadRequestException("Option ID " + optId + " is invalid for question " + request.getQuestionId());
            }
        }

        // Evaluate MCQ correctness against server-side snapshot
        List<String> correctIds = snap.getCorrectOptionIds() != null ? snap.getCorrectOptionIds() : List.of();
        boolean isCorrect = new HashSet<>(correctIds).equals(new HashSet<>(selected));
        double awardedPoints = isCorrect ? item.getPoints() : 0.0;

        AssessmentQuestionResponse response = responseRepository.findByAttemptIdAndQuestionId(attempt.getId(), request.getQuestionId())
                .orElse(AssessmentQuestionResponse.builder()
                        .attemptId(attempt.getId())
                        .assessmentId(assessment.getId())
                        .candidateUserId(candidateUserId)
                        .questionId(request.getQuestionId())
                        .questionType(BankQuestionType.MCQ)
                        .maxPoints(item.getPoints())
                        .createdAt(Instant.now())
                        .build());

        response.setSelectedOptionIds(selected);
        response.setIsCorrect(isCorrect);
        response.setAwardedPoints(awardedPoints);
        response.setSavedAt(Instant.now());
        response.setEvaluatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());

        AssessmentQuestionResponse saved = responseRepository.save(response);

        publishAnalytics(AnalyticsEventType.ANSWER_SAVED,
                saved.getId(),
                AnalyticsEntityType.QUESTION_RESPONSE,
                attempt.getAssessmentId(),
                attempt.getCandidateEmail(),
                AnalyticsActorType.CANDIDATE,
                candidateUserId,
                Map.of(
                        "questionId", request.getQuestionId(),
                        "questionType", "MCQ",
                        "isCorrect", isCorrect,
                        "awardedPoints", awardedPoints
                ));

        recordAuditEvent(assessment.getId(), attempt.getId(), candidateUserId, attempt.getCandidateEmail(),
                request.getQuestionId(), AssessmentAuditEventType.MCQ_ANSWER_CHANGED, AssessmentAuditEventSource.SERVER_VERIFIED,
                Map.of("selectedCount", selected.size(), "isCorrect", isCorrect, "awardedPoints", awardedPoints));

        return SavedQuestionResponseDto.builder()
                .questionId(saved.getQuestionId())
                .questionType(saved.getQuestionType())
                .selectedOptionIds(saved.getSelectedOptionIds())
                .markedForReview(saved.isMarkedForReview())
                .savedAt(saved.getSavedAt())
                .build();
    }

    @Override
    public CodingProblemExecutionResponse runSample(String idOrSlug, String candidateUserId, RunAssessmentSampleRequest request) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAttempt attempt = getActiveAndValidAttempt(assessment.getId(), candidateUserId);

        AssessmentItem item = assessment.getItems().stream()
                .filter(i -> i.getQuestionId().equals(request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Question ID " + request.getQuestionId() + " not found in assessment."));

        if (item.getQuestionType() != BankQuestionType.CODING) {
            throw new BadRequestException("Question " + request.getQuestionId() + " is not a coding problem.");
        }

        QuestionSnapshot snap = item.getSnapshot();
        CodingProblemExecutionResponse res = executeCandidateCodeOnJudge(snap, request.getLanguageId(), request.getCode(), null, true);

        recordAuditEvent(assessment.getId(), attempt.getId(), candidateUserId, attempt.getCandidateEmail(),
                request.getQuestionId(), AssessmentAuditEventType.CODE_RUN_SAMPLE, AssessmentAuditEventSource.SERVER_VERIFIED,
                Map.of("languageId", request.getLanguageId(), "status", res.getOverallStatus() != null ? res.getOverallStatus() : "UNKNOWN",
                        "passed", res.getPassedTestcases() != null ? res.getPassedTestcases() : 0));

        publishAnalytics(AnalyticsEventType.SAMPLE_RUN_EXECUTED,
                attempt.getId(),
                AnalyticsEntityType.ASSESSMENT_ATTEMPT,
                attempt.getAssessmentId(),
                attempt.getCandidateEmail(),
                AnalyticsActorType.CANDIDATE,
                candidateUserId,
                Map.of(
                        "questionId", request.getQuestionId(),
                        "language", request.getLanguageId() != null ? request.getLanguageId() : "unknown",
                        "status", res.getOverallStatus() != null ? res.getOverallStatus() : "UNKNOWN"
                ));

        return res;
    }

    @Override
    public CodingProblemExecutionResponse runCustomInput(String idOrSlug, String candidateUserId, RunAssessmentCustomInputRequest request) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAttempt attempt = getActiveAndValidAttempt(assessment.getId(), candidateUserId);

        AssessmentItem item = assessment.getItems().stream()
                .filter(i -> i.getQuestionId().equals(request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Question ID " + request.getQuestionId() + " not found in assessment."));

        if (item.getQuestionType() != BankQuestionType.CODING) {
            throw new BadRequestException("Question " + request.getQuestionId() + " is not a coding problem.");
        }

        QuestionSnapshot snap = item.getSnapshot();
        CodingProblemExecutionResponse res = executeCandidateCodeOnJudge(snap, request.getLanguageId(), request.getCode(), request.getStdin() != null ? request.getStdin() : "", true);

        recordAuditEvent(assessment.getId(), attempt.getId(), candidateUserId, attempt.getCandidateEmail(),
                request.getQuestionId(), AssessmentAuditEventType.CODE_RUN_CUSTOM, AssessmentAuditEventSource.SERVER_VERIFIED,
                Map.of("languageId", request.getLanguageId(), "status", res.getOverallStatus() != null ? res.getOverallStatus() : "UNKNOWN"));

        publishAnalytics(AnalyticsEventType.SAMPLE_RUN_EXECUTED,
                attempt.getId(),
                AnalyticsEntityType.ASSESSMENT_ATTEMPT,
                attempt.getAssessmentId(),
                attempt.getCandidateEmail(),
                AnalyticsActorType.CANDIDATE,
                candidateUserId,
                Map.of(
                        "questionId", request.getQuestionId(),
                        "language", request.getLanguageId() != null ? request.getLanguageId() : "unknown",
                        "status", res.getOverallStatus() != null ? res.getOverallStatus() : "UNKNOWN",
                        "customInput", true
                ));

        return res;
    }

    @Override
    @Transactional
    public SavedQuestionResponseDto saveCodeResponse(String idOrSlug, String candidateUserId, SaveCodeResponseRequest request) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAttempt attempt = getActiveAndValidAttempt(assessment.getId(), candidateUserId);

        AssessmentItem item = assessment.getItems().stream()
                .filter(i -> i.getQuestionId().equals(request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Question ID " + request.getQuestionId() + " not found in assessment."));

        AssessmentQuestionResponse response = responseRepository.findByAttemptIdAndQuestionId(attempt.getId(), request.getQuestionId())
                .orElse(AssessmentQuestionResponse.builder()
                        .attemptId(attempt.getId())
                        .assessmentId(assessment.getId())
                        .candidateUserId(candidateUserId)
                        .questionId(request.getQuestionId())
                        .questionType(BankQuestionType.CODING)
                        .maxPoints(item.getPoints())
                        .createdAt(Instant.now())
                        .build());

        response.setLanguageId(request.getLanguageId());
        response.setCode(request.getCode());
        response.setSavedAt(Instant.now());
        response.setUpdatedAt(Instant.now());

        AssessmentQuestionResponse saved = responseRepository.save(response);

        publishAnalytics(AnalyticsEventType.ANSWER_SAVED,
                saved.getId(),
                AnalyticsEntityType.QUESTION_RESPONSE,
                attempt.getAssessmentId(),
                attempt.getCandidateEmail(),
                AnalyticsActorType.CANDIDATE,
                candidateUserId,
                Map.of(
                        "questionId", request.getQuestionId(),
                        "questionType", "CODING",
                        "languageId", request.getLanguageId() != null ? request.getLanguageId() : "unknown"
                ));

        recordAuditEvent(assessment.getId(), attempt.getId(), candidateUserId, attempt.getCandidateEmail(),
                request.getQuestionId(), AssessmentAuditEventType.CODE_SAVED, AssessmentAuditEventSource.SERVER_VERIFIED,
                Map.of("languageId", request.getLanguageId(), "codeLength", request.getCode() != null ? request.getCode().length() : 0));

        return SavedQuestionResponseDto.builder()
                .questionId(saved.getQuestionId())
                .questionType(saved.getQuestionType())
                .languageId(saved.getLanguageId())
                .code(saved.getCode())
                .lastExecutionResult(saved.getLastExecutionResult())
                .allTestcasesPassed(saved.getAllTestcasesPassed())
                .markedForReview(saved.isMarkedForReview())
                .savedAt(saved.getSavedAt())
                .awardedPoints(saved.getAwardedPoints())
                .bestAwardedPoints(saved.getBestAwardedPoints())
                .maxPoints(saved.getMaxPoints())
                .passedTestcases(saved.getBestPassedTestcases())
                .totalTestcases(saved.getTotalTestcases())
                .submissions(saved.getSubmissions())
                .build();
    }

    @Override
    @Transactional
    public SavedQuestionResponseDto submitCodeResponse(String idOrSlug, String candidateUserId, SubmitCodeResponseRequest request) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAttempt attempt = getActiveAndValidAttempt(assessment.getId(), candidateUserId);

        AssessmentItem item = assessment.getItems().stream()
                .filter(i -> i.getQuestionId().equals(request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Question ID " + request.getQuestionId() + " not found in assessment."));

        if (item.getQuestionType() != BankQuestionType.CODING) {
            throw new BadRequestException("Question " + request.getQuestionId() + " is not a coding problem.");
        }

        QuestionSnapshot snap = item.getSnapshot();

        // Execute against all testcases (sample + hidden)
        CodingProblemExecutionResponse executionResult = executeCandidateCodeOnJudge(
                snap, request.getLanguageId(), request.getCode(), null, false
        );

        boolean allPassed = "ACCEPTED".equalsIgnoreCase(executionResult.getOverallStatus());
        int passed = executionResult.getPassedTestcases() != null ? executionResult.getPassedTestcases() : 0;
        int total = executionResult.getTotalTestcases() != null ? executionResult.getTotalTestcases() : 0;
        double passedRatio = total > 0 ? (double) passed / total : 0.0;
        double awardedPoints = passedRatio * item.getPoints();

        AssessmentQuestionResponse response = responseRepository.findByAttemptIdAndQuestionId(attempt.getId(), request.getQuestionId())
                .orElse(AssessmentQuestionResponse.builder()
                        .attemptId(attempt.getId())
                        .assessmentId(assessment.getId())
                        .candidateUserId(candidateUserId)
                        .questionId(request.getQuestionId())
                        .questionType(BankQuestionType.CODING)
                        .maxPoints(item.getPoints())
                        .createdAt(Instant.now())
                        .build());

        String overallStatus = executionResult.getOverallStatus() != null ? executionResult.getOverallStatus() : "UNKNOWN";
        long executionTimeMs = executionResult.getTotalExecutionTimeMs() != null ? executionResult.getTotalExecutionTimeMs() : 0L;

        com.bharath.stacked.modules.assessment.model.CodingSubmissionRecord submissionRecord =
                com.bharath.stacked.modules.assessment.model.CodingSubmissionRecord.builder()
                        .submissionId(java.util.UUID.randomUUID().toString())
                        .submittedAt(Instant.now())
                        .languageId(request.getLanguageId())
                        .code(request.getCode())
                        .status(overallStatus)
                        .passedTestcases(passed)
                        .totalTestcases(total)
                        .awardedPoints(awardedPoints)
                        .executionTimeMs(executionTimeMs)
                        .build();

        if (response.getSubmissions() == null) {
            response.setSubmissions(new java.util.ArrayList<>());
        }
        response.getSubmissions().add(submissionRecord);

        // Keep best awarded points across all submissions for this question
        double currentBest = response.getBestAwardedPoints() != null ? response.getBestAwardedPoints() : response.getAwardedPoints();
        if (awardedPoints >= currentBest) {
            response.setBestAwardedPoints(awardedPoints);
            response.setAwardedPoints(awardedPoints);
            response.setBestPassedTestcases(passed);
            response.setTotalTestcases(total);
            response.setBestCode(request.getCode());
            response.setBestLanguageId(request.getLanguageId());
            response.setBestExecutionResult(executionResult);
        } else {
            response.setAwardedPoints(currentBest);
        }

        if (allPassed) {
            response.setAllTestcasesPassed(true);
        } else if (response.getAllTestcasesPassed() == null) {
            response.setAllTestcasesPassed(false);
        }

        response.setLanguageId(request.getLanguageId());
        response.setCode(request.getCode());
        response.setLastExecutionResult(executionResult);
        response.setTotalTestcases(total);
        response.setSavedAt(Instant.now());
        response.setEvaluatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());

        AssessmentQuestionResponse saved = responseRepository.save(response);

        publishAnalytics(AnalyticsEventType.CODE_SUBMISSION_EVALUATED,
                saved.getId(),
                AnalyticsEntityType.QUESTION_RESPONSE,
                attempt.getAssessmentId(),
                attempt.getCandidateEmail(),
                AnalyticsActorType.CANDIDATE,
                candidateUserId,
                Map.of(
                        "questionId", request.getQuestionId(),
                        "language", request.getLanguageId() != null ? request.getLanguageId() : "unknown",
                        "verdict", overallStatus,
                        "executionTimeMs", executionTimeMs,
                        "allPassed", allPassed,
                        "passedRatio", passedRatio,
                        "awardedPoints", awardedPoints
                ));

        recordAuditEvent(assessment.getId(), attempt.getId(), candidateUserId, attempt.getCandidateEmail(),
                request.getQuestionId(), AssessmentAuditEventType.CODE_SUBMITTED, AssessmentAuditEventSource.SERVER_VERIFIED,
                Map.of("languageId", request.getLanguageId(), "status", executionResult.getOverallStatus() != null ? executionResult.getOverallStatus() : "UNKNOWN",
                        "allPassed", allPassed, "passedRatio", passedRatio, "awardedPoints", awardedPoints));

        return SavedQuestionResponseDto.builder()
                .questionId(saved.getQuestionId())
                .questionType(saved.getQuestionType())
                .languageId(saved.getLanguageId())
                .code(saved.getCode())
                .lastExecutionResult(saved.getLastExecutionResult())
                .allTestcasesPassed(saved.getAllTestcasesPassed())
                .markedForReview(saved.isMarkedForReview())
                .savedAt(saved.getSavedAt())
                .awardedPoints(saved.getAwardedPoints())
                .bestAwardedPoints(saved.getBestAwardedPoints())
                .maxPoints(saved.getMaxPoints())
                .passedTestcases(saved.getBestPassedTestcases())
                .totalTestcases(saved.getTotalTestcases())
                .submissions(saved.getSubmissions())
                .build();
    }

    @Override
    @Transactional
    public SavedQuestionResponseDto markForReview(String idOrSlug, String candidateUserId, MarkQuestionForReviewRequest request) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAttempt attempt = getActiveAndValidAttempt(assessment.getId(), candidateUserId);

        AssessmentItem item = assessment.getItems().stream()
                .filter(i -> i.getQuestionId().equals(request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Question ID " + request.getQuestionId() + " not found in assessment."));

        AssessmentQuestionResponse response = responseRepository.findByAttemptIdAndQuestionId(attempt.getId(), request.getQuestionId())
                .orElse(AssessmentQuestionResponse.builder()
                        .attemptId(attempt.getId())
                        .assessmentId(assessment.getId())
                        .candidateUserId(candidateUserId)
                        .questionId(request.getQuestionId())
                        .questionType(item.getQuestionType())
                        .maxPoints(item.getPoints())
                        .createdAt(Instant.now())
                        .build());

        response.setMarkedForReview(request.isMarkedForReview());
        response.setUpdatedAt(Instant.now());
        AssessmentQuestionResponse saved = responseRepository.save(response);

        recordAuditEvent(assessment.getId(), attempt.getId(), candidateUserId, attempt.getCandidateEmail(),
                request.getQuestionId(), AssessmentAuditEventType.MARK_REVIEW_TOGGLED, AssessmentAuditEventSource.SERVER_VERIFIED,
                Map.of("markedForReview", request.isMarkedForReview()));

        return SavedQuestionResponseDto.builder()
                .questionId(saved.getQuestionId())
                .questionType(saved.getQuestionType())
                .selectedOptionIds(saved.getSelectedOptionIds())
                .languageId(saved.getLanguageId())
                .code(saved.getCode())
                .lastExecutionResult(saved.getLastExecutionResult())
                .allTestcasesPassed(saved.getAllTestcasesPassed())
                .markedForReview(saved.isMarkedForReview())
                .savedAt(saved.getSavedAt())
                .build();
    }

    @Override
    @Transactional
    public FinalSubmitResponse finalSubmit(String idOrSlug, String candidateUserId) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAttempt attempt = getActiveAndValidAttempt(assessment.getId(), candidateUserId);

        Instant now = Instant.now();

        // Calculate candidate score across all responses
        List<AssessmentQuestionResponse> responses = responseRepository.findAllByAttemptId(attempt.getId());
        double totalScore = 0.0;
        for (AssessmentQuestionResponse resp : responses) {
            totalScore += resp.getAwardedPoints();
        }

        boolean passed = assessment.getPassingScore() != null && totalScore >= assessment.getPassingScore();
        Boolean passedStatus = assessment.getPassingScore() != null ? Boolean.valueOf(totalScore >= assessment.getPassingScore()) : null;

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(now);
        attempt.setScore(totalScore);
        attempt.setPassed(passedStatus);
        attempt.setUpdatedAt(now);
        attemptRepository.save(attempt);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateAssessmentMetrics(assessment.getId());
        }

        long durationSeconds = attempt.getStartedAt() != null ? Duration.between(attempt.getStartedAt(), now).toSeconds() : 0;
        publishAnalytics(AnalyticsEventType.ATTEMPT_SUBMITTED,
                attempt.getId(),
                AnalyticsEntityType.ASSESSMENT_ATTEMPT,
                assessment.getId(),
                attempt.getCandidateEmail(),
                AnalyticsActorType.CANDIDATE,
                candidateUserId,
                Map.of(
                        "durationSeconds", durationSeconds,
                        "score", totalScore,
                        "passed", passed
                ));

        recordAuditEvent(assessment.getId(), attempt.getId(), candidateUserId, attempt.getCandidateEmail(),
                null, AssessmentAuditEventType.FINAL_SUBMISSION, AssessmentAuditEventSource.SERVER_VERIFIED,
                Map.of("score", totalScore, "maxScore", assessment.getTotalScore(), "passed", passed));

        // Update Assignment
        assignmentRepository.findByAssessmentIdAndCandidateUserId(assessment.getId(), candidateUserId).ifPresent(as -> {
            as.setStatus(CandidateParticipationStatus.SUBMITTED);
            as.setCompletedAt(now);
            as.setUpdatedAt(now);
            assignmentRepository.save(as);
        });

        log.info("Candidate [{}] successfully finalized assessment [{}] (score: {}/{}, passed: {})",
                candidateUserId, assessment.getTitle(), totalScore, assessment.getTotalScore(), passed);

        return FinalSubmitResponse.builder()
                .attemptId(attempt.getId())
                .assessmentId(assessment.getId())
                .status(AttemptStatus.SUBMITTED)
                .submittedAt(now)
                .score(totalScore)
                .maxScore(assessment.getTotalScore())
                .passed(passed)
                .message("Assessment submitted successfully.")
                .build();
    }

    // ==========================================
    // Internal Helper Methods
    // ==========================================

    private Assessment findByIdOrSlug(String idOrSlug) {
        if (!StringUtils.hasText(idOrSlug)) {
            throw new BadRequestException("Assessment identifier cannot be empty.");
        }
        return assessmentRepository.findById(idOrSlug)
                .or(() -> assessmentRepository.findBySlug(idOrSlug))
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id or slug", idOrSlug));
    }

    private AssessmentAssignment findAssignment(String assessmentId, String candidateUserId, String candidateEmail) {
        if (StringUtils.hasText(candidateUserId)) {
            Optional<AssessmentAssignment> byUserId = assignmentRepository.findByAssessmentIdAndCandidateUserId(assessmentId, candidateUserId);
            if (byUserId.isPresent()) return byUserId.get();
        }
        if (StringUtils.hasText(candidateEmail)) {
            Optional<AssessmentAssignment> byEmail = assignmentRepository.findByAssessmentIdAndCandidateEmail(assessmentId, candidateEmail.toLowerCase());
            if (byEmail.isPresent()) return byEmail.get();
        }
        throw new BadRequestException("Candidate is not assigned to this assessment.");
    }

    private AssessmentAttempt getActiveAndValidAttempt(String assessmentId, String candidateUserId) {
        AssessmentAttempt attempt = attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus(
                assessmentId, candidateUserId, AttemptStatus.IN_PROGRESS
        ).orElseThrow(() -> new BadRequestException("No active in-progress attempt found for candidate."));

        Instant now = Instant.now();
        Assessment assessment = assessmentRepository.findById(assessmentId).orElse(null);
        if (assessment != null && (assessment.getEffectiveStatus(now) == AssessmentStatus.COMPLETED || assessment.isManuallyCompleted())) {
            expireAttempt(attempt, assessment, now);
            throw new BadRequestException("Assessment has been completed. Further modifications or submissions are not accepted.");
        }

        if (now.isAfter(attempt.getEffectiveDeadline())) {
            expireAttempt(attempt, assessment, now);
            throw new BadRequestException("Assessment attempt time has expired.");
        }

        return attempt;
    }

    private void expireAttempt(AssessmentAttempt attempt, Assessment assessment, Instant now) {
        List<AssessmentQuestionResponse> responses = responseRepository.findAllByAttemptId(attempt.getId());
        double totalScore = 0.0;
        for (AssessmentQuestionResponse resp : responses) {
            totalScore += resp.getAwardedPoints();
        }
        if (assessment == null) {
            assessment = assessmentRepository.findById(attempt.getAssessmentId()).orElse(null);
        }
        Boolean passedStatus = assessment != null && assessment.getPassingScore() != null
                ? Boolean.valueOf(totalScore >= assessment.getPassingScore())
                : null;
        boolean passed = Boolean.TRUE.equals(passedStatus);

        attempt.setStatus(AttemptStatus.EXPIRED);
        attempt.setScore(totalScore);
        attempt.setPassed(passedStatus);
        attempt.setUpdatedAt(now);
        if (attempt.getSubmittedAt() == null) {
            Instant effectiveEnd = attempt.getEffectiveDeadline() != null && attempt.getEffectiveDeadline().isBefore(now)
                    ? attempt.getEffectiveDeadline()
                    : now;
            attempt.setSubmittedAt(effectiveEnd);
        }
        attemptRepository.save(attempt);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.invalidateAssessmentMetrics(assessment.getId());
        }

        long durationSeconds = attempt.getStartedAt() != null ? Duration.between(attempt.getStartedAt(), now).toSeconds() : 0;
        publishAnalytics(AnalyticsEventType.ATTEMPT_EXPIRED,
                attempt.getId(),
                AnalyticsEntityType.ASSESSMENT_ATTEMPT,
                attempt.getAssessmentId(),
                attempt.getCandidateEmail(),
                AnalyticsActorType.SYSTEM,
                null,
                Map.of(
                        "durationSeconds", durationSeconds,
                        "score", totalScore,
                        "passed", passed
                ));

        assignmentRepository.findByAssessmentIdAndCandidateUserId(attempt.getAssessmentId(), attempt.getCandidateUserId()).ifPresent(as -> {
            as.setStatus(CandidateParticipationStatus.EXPIRED);
            as.setUpdatedAt(now);
            assignmentRepository.save(as);
        });
    }

    private CandidateAssessmentSummaryResponse buildCandidateAssessmentSummary(
            Assessment assessment,
            AssessmentAssignment assignment,
            String candidateUserId) {

        Instant now = Instant.now();
        int attemptsUsed = assignment != null ? assignment.getAttemptsCount() : 0;
        CandidateParticipationStatus partStatus = assignment != null ? assignment.getStatus() : CandidateParticipationStatus.INVITED;

        boolean canStart = true;
        String cannotStartReason = null;

        AssessmentStatus effectiveStatus = assessment.getEffectiveStatus(now);

        if (assignment == null) {
            canStart = false;
            cannotStartReason = "Not assigned to this assessment.";
        } else if (effectiveStatus == AssessmentStatus.NOT_STARTED) {
            canStart = false;
            cannotStartReason = "Assessment has not started yet. Starts: " + assessment.getScheduledStart();
        } else if (effectiveStatus == AssessmentStatus.COMPLETED) {
            canStart = false;
            cannotStartReason = "Assessment deadline has passed.";
        } else if (effectiveStatus != AssessmentStatus.ACTIVE) {
            canStart = false;
            cannotStartReason = "Assessment is not currently active.";
        } else if (assessment.getMaxAttempts() != null && attemptsUsed >= assessment.getMaxAttempts()) {
            canStart = false;
            cannotStartReason = "Maximum attempt limit reached.";
        }

        // Active attempt check
        String activeAttemptId = null;
        if (StringUtils.hasText(candidateUserId)) {
            Optional<AssessmentAttempt> activeAttempt = attemptRepository.findByAssessmentIdAndCandidateUserIdAndStatus(
                    assessment.getId(), candidateUserId, AttemptStatus.IN_PROGRESS
            );
            if (activeAttempt.isPresent()) {
                AssessmentAttempt att = activeAttempt.get();
                if (now.isAfter(att.getEffectiveDeadline())) {
                    // Lazy on-demand expiration: evaluate and expire right now without waiting for any cron!
                    expireAttempt(att, assessment, now);
                    assignment = assignmentRepository.findByAssessmentIdAndCandidateUserId(assessment.getId(), candidateUserId).orElse(assignment);
                    partStatus = assignment != null ? assignment.getStatus() : CandidateParticipationStatus.EXPIRED;
                    attemptsUsed = assignment != null ? assignment.getAttemptsCount() : attemptsUsed;
                    activeAttemptId = null;
                    canStart = false;
                    cannotStartReason = "Assessment attempt time has expired.";
                } else {
                    activeAttemptId = att.getId();
                    canStart = true;
                    cannotStartReason = null;
                }
            }
        }

        return CandidateAssessmentSummaryResponse.builder()
                .assessmentId(assessment.getId())
                .slug(assessment.getSlug())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .assessmentStatus(effectiveStatus)
                .scheduledStart(assessment.getScheduledStart())
                .scheduledEnd(assessment.getScheduledEnd())
                .durationMinutes(assessment.getDurationMinutes())
                .maxAttempts(assessment.getMaxAttempts())
                .attemptsUsed(attemptsUsed)
                .assignmentStatus(partStatus)
                .canStart(canStart)
                .cannotStartReason(cannotStartReason)
                .activeAttemptId(activeAttemptId)
                .totalQuestions(assessment.getTotalQuestions())
                .totalScore(assessment.getTotalScore())
                .manuallyActivated(assessment.isManuallyActivated())
                .manuallyCompleted(assessment.isManuallyCompleted())
                .manualInterventionMessage(assessment.getManualInterventionMessage())
                .build();
    }

    private record ProcessedItemsResult(List<AssessmentItem> items, int mcqCount, int codingCount, double totalPoints) {}

    private ProcessedItemsResult processAndSnapshotItems(
            AssessmentSelectionMode mode,
            String questionBankId,
            List<AssessmentCustomItemRequest> customItems) {

        List<AssessmentItem> items = new ArrayList<>();
        int mcqCount = 0;
        int codingCount = 0;
        double totalPoints = 0.0;

        if (mode == AssessmentSelectionMode.QUESTION_BANK) {
            if (!StringUtils.hasText(questionBankId)) {
                throw new BadRequestException("Question Bank ID is required when selectionMode is QUESTION_BANK.");
            }
            QuestionBank bank = questionBankRepository.findById(questionBankId)
                    .orElseThrow(() -> new ResourceNotFoundException("Question Bank not found with ID: " + questionBankId));

            if (bank.getStatus() != QuestionBankStatus.PUBLISHED) {
                throw new BadRequestException("Question Bank '" + bank.getTitle() + "' must be PUBLISHED to be used in an assessment.");
            }

            if (bank.getItems() == null || bank.getItems().isEmpty()) {
                throw new BadRequestException("Question Bank '" + bank.getTitle() + "' contains no questions.");
            }

            for (int i = 0; i < bank.getItems().size(); i++) {
                QuestionBankItem bankItem = bank.getItems().get(i);
                QuestionSnapshot snapshot = buildSnapshot(bankItem.getQuestionType(), bankItem.getQuestionId());
                double pts = bankItem.getCustomPoints() != null ? bankItem.getCustomPoints() : getDefaultPoints(bankItem.getQuestionType(), bankItem.getQuestionId());

                if (bankItem.getQuestionType() == BankQuestionType.MCQ) mcqCount++;
                else codingCount++;
                totalPoints += pts;

                items.add(AssessmentItem.builder()
                        .questionId(bankItem.getQuestionId())
                        .questionType(bankItem.getQuestionType())
                        .order(i + 1)
                        .points(pts)
                        .snapshot(snapshot)
                        .build());
            }

        } else {
            if (customItems == null || customItems.isEmpty()) {
                return new ProcessedItemsResult(items, 0, 0, 0.0);
            }

            Set<String> seenQuestionIds = new HashSet<>();
            for (int i = 0; i < customItems.size(); i++) {
                AssessmentCustomItemRequest req = customItems.get(i);
                if (!seenQuestionIds.add(req.getQuestionId())) {
                    throw new BadRequestException("Duplicate question detected: " + req.getQuestionId());
                }

                QuestionSnapshot snapshot = buildSnapshot(req.getQuestionType(), req.getQuestionId());
                double pts = req.getPoints() != null ? req.getPoints() : getDefaultPoints(req.getQuestionType(), req.getQuestionId());

                if (req.getQuestionType() == BankQuestionType.MCQ) mcqCount++;
                else codingCount++;
                totalPoints += pts;

                items.add(AssessmentItem.builder()
                        .questionId(req.getQuestionId())
                        .questionType(req.getQuestionType())
                        .order(req.getOrder() > 0 ? req.getOrder() : i + 1)
                        .points(pts)
                        .snapshot(snapshot)
                        .build());
            }
        }

        return new ProcessedItemsResult(items, mcqCount, codingCount, totalPoints);
    }

    private QuestionSnapshot buildSnapshot(BankQuestionType type, String questionId) {
        if (type == BankQuestionType.MCQ) {
            McqQuestion mcq = mcqQuestionRepository.findById(questionId)
                    .orElseThrow(() -> new ResourceNotFoundException("MCQ Question not found with ID: " + questionId));
            if (mcq.getStatus() != McqStatus.PUBLISHED) {
                throw new BadRequestException("MCQ '" + mcq.getTitle() + "' is not published and cannot be added to assessment.");
            }

            List<OptionSnapshot> options = new ArrayList<>();
            if (mcq.getOptions() != null) {
                for (int idx = 0; idx < mcq.getOptions().size(); idx++) {
                    McqOption o = mcq.getOptions().get(idx);
                    options.add(OptionSnapshot.builder()
                            .id(o.getId())
                            .content(o.getText())
                            .order(idx + 1)
                            .build());
                }
            }

            return QuestionSnapshot.builder()
                    .title(mcq.getTitle())
                    .mcqContent(mcq.getContent())
                    .mcqType(mcq.getType())
                    .options(options)
                    .correctOptionIds(mcq.getCorrectOptionIds())
                    .build();

        } else {
            CodingProblem cp = codingProblemRepository.findById(questionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Coding Problem not found with ID: " + questionId));
            if (cp.getStatus() != CodingProblemStatus.PUBLISHED) {
                throw new BadRequestException("Coding Problem '" + cp.getTitle() + "' is not published and cannot be added to assessment.");
            }

            CodeExecutionConstraints constraints = null;
            if (cp.getExecutionConstraints() != null && !cp.getExecutionConstraints().isEmpty()) {
                String refLang = cp.getReferenceSolutionLanguage();
                if (refLang != null) {
                    constraints = cp.getExecutionConstraints().get(refLang);
                    if (constraints == null) {
                        constraints = cp.getExecutionConstraints().get(refLang.replace('.', '_'));
                    }
                    if (constraints == null) {
                        constraints = cp.getExecutionConstraints().get(refLang.replace('_', '.'));
                    }
                }
                if (constraints == null) {
                    constraints = cp.getExecutionConstraints().values().iterator().next();
                }
            }

            return QuestionSnapshot.builder()
                    .title(cp.getTitle())
                    .codingStatement(cp.getStatement())
                    .codingProblemType(cp.getProblemType())
                    .supportedLanguageIds(cp.getSupportedLanguageIds())
                    .starterCode(cp.getStarterCode())
                    .executionConstraints(constraints)
                    .sampleTestCases(cp.getSampleTestCases() != null ? cp.getSampleTestCases() : List.of())
                    .hiddenTestCases(cp.getHiddenTestCases() != null ? cp.getHiddenTestCases() : List.of())
                    .referenceSolutionLanguage(cp.getReferenceSolutionLanguage())
                    .referenceSolution(cp.getReferenceSolution())
                    .requireVerificationScript(cp.isRequireVerificationScript())
                    .verificationScriptLanguage(cp.getVerificationScriptLanguage())
                    .verificationScript(cp.getVerificationScript())
                    .build();
        }
    }

    private double getDefaultPoints(BankQuestionType type, String questionId) {
        if (type == BankQuestionType.MCQ) {
            return 1.0;
        } else {
            return codingProblemRepository.findById(questionId).map(cp -> {
                return switch (cp.getDifficulty()) {
                    case EASY -> 10.0;
                    case MEDIUM -> 20.0;
                    case HARD -> 30.0;
                };
            }).orElse(10.0);
        }
    }

    private void validateScheduleAndDuration(Instant start, Instant end, Integer duration) {
        if (duration == null || duration < 1) {
            throw new BadRequestException("Assessment duration must be at least 1 minute.");
        }
        if (start != null && end != null && !end.isAfter(start)) {
            throw new BadRequestException("Scheduled end time must be after scheduled start time.");
        }
    }

    private void validatePublicationEligibility(List<AssessmentItem> items, Integer duration, Instant start, Instant end) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Cannot publish an assessment with zero questions.");
        }
        validateScheduleAndDuration(start, end, duration);
    }

    private int assignCandidatesInternal(String assessmentId, Set<String> emails) {
        int added = 0;
        for (String email : emails) {
            if (!assignmentRepository.existsByAssessmentIdAndCandidateEmail(assessmentId, email)) {
                Optional<User> userOpt = userRepository.findByEmail(email);
                AssessmentAssignment assignment = AssessmentAssignment.builder()
                        .assessmentId(assessmentId)
                        .candidateEmail(email)
                        .candidateUserId(userOpt.map(User::getId).orElse(null))
                        .status(CandidateParticipationStatus.INVITED)
                        .attemptsCount(0)
                        .invitedAt(Instant.now())
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                assignmentRepository.save(assignment);
                added++;
            }
        }
        return (int) assignmentRepository.countByAssessmentId(assessmentId);
    }

    private Set<String> parseRawTokens(String raw, List<String> list) {
        Set<String> tokens = new HashSet<>();
        if (StringUtils.hasText(raw)) {
            String[] split = raw.split("[,;\\s\\n\\r]+");
            for (String s : split) {
                if (StringUtils.hasText(s)) {
                    tokens.add(s.trim());
                }
            }
        }
        if (list != null) {
            for (String s : list) {
                if (StringUtils.hasText(s)) {
                    tokens.add(s.trim());
                }
            }
        }
        return tokens;
    }

    private Set<String> parseEmails(String raw, List<String> list) {
        Set<String> rawTokens = parseRawTokens(raw, list);
        Set<String> validEmails = new HashSet<>();
        for (String token : rawTokens) {
            String normalized = token.trim().toLowerCase();
            if (EMAIL_PATTERN.matcher(normalized).matches()) {
                validEmails.add(normalized);
            }
        }
        return validEmails;
    }

    private String generateUniqueSlug(String title, String existingId) {
        String base = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (base.isEmpty()) base = "assessment";

        String slug = base;
        int counter = 1;
        while (true) {
            Optional<Assessment> found = assessmentRepository.findBySlug(slug);
            if (found.isEmpty() || (existingId != null && found.get().getId().equals(existingId))) {
                return slug;
            }
            slug = base + "-" + counter++;
        }
    }

    private CodingProblemExecutionResponse executeCandidateCodeOnJudge(
            QuestionSnapshot snapshot,
            String candidateLanguageId,
            String code,
            String customStdin,
            boolean isSampleOnly) {

        if (!languageRegistry.isSupported(candidateLanguageId)) {
            throw new BadRequestException("Language '" + candidateLanguageId + "' is not supported on this platform.");
        }
        Language candLanguage = languageRegistry.get(candidateLanguageId);

        Language refLanguage = StringUtils.hasText(snapshot.getReferenceSolutionLanguage()) ?
                languageRegistry.get(snapshot.getReferenceSolutionLanguage()) : null;

        Language verLanguage = StringUtils.hasText(snapshot.getVerificationScriptLanguage()) ?
                languageRegistry.get(snapshot.getVerificationScriptLanguage()) : null;

        List<CandidateTestcaseItem> testcaseItems = new ArrayList<>();
        int caseIdx = 1;

        if (isSampleOnly) {
            if (customStdin != null) {
                testcaseItems.add(new CandidateTestcaseItem("custom-1", 1, customStdin, null, null, true, false));
            } else if (snapshot.getSampleTestCases() != null) {
                for (TestCase tc : snapshot.getSampleTestCases()) {
                    testcaseItems.add(new CandidateTestcaseItem(
                            tc.getId() != null ? tc.getId() : "sample-" + caseIdx,
                            caseIdx++,
                            tc.getStdin(),
                            tc.getDbInit(),
                            tc.getOutput(),
                            false,
                            true
                    ));
                }
            }
        } else {
            // Full scored run: sample + hidden
            if (snapshot.getSampleTestCases() != null) {
                for (TestCase tc : snapshot.getSampleTestCases()) {
                    testcaseItems.add(new CandidateTestcaseItem(
                            tc.getId() != null ? tc.getId() : "sample-" + caseIdx,
                            caseIdx++,
                            tc.getStdin(),
                            tc.getDbInit(),
                            tc.getOutput(),
                            false,
                            true
                    ));
                }
            }
            if (snapshot.getHiddenTestCases() != null) {
                for (TestCase tc : snapshot.getHiddenTestCases()) {
                    testcaseItems.add(new CandidateTestcaseItem(
                            tc.getId() != null ? tc.getId() : "hidden-" + caseIdx,
                            caseIdx++,
                            tc.getStdin(),
                            tc.getDbInit(),
                            tc.getOutput(),
                            false,
                            false
                    ));
                }
            }
        }

        long candTimeLimit = executionConstraintResolver.resolveTimeLimitMs(candLanguage, snapshot.getExecutionConstraints(), null);
        long candMemLimit = executionConstraintResolver.resolveMemoryLimitKb(candLanguage, snapshot.getExecutionConstraints(), null);
        CodeExecutionConstraints candConstraints = new CodeExecutionConstraints(candTimeLimit, candMemLimit);

        CodeExecutionConstraints refConstraints = refLanguage != null
                ? new CodeExecutionConstraints(
                        executionConstraintResolver.resolveTimeLimitMs(refLanguage, snapshot.getExecutionConstraints(), null),
                        executionConstraintResolver.resolveMemoryLimitKb(refLanguage, snapshot.getExecutionConstraints(), null))
                : (snapshot.getExecutionConstraints() != null ? snapshot.getExecutionConstraints() : candConstraints);

        CodeExecutionConstraints verConstraints = verLanguage != null
                ? new CodeExecutionConstraints(
                        executionConstraintResolver.resolveTimeLimitMs(verLanguage, snapshot.getExecutionConstraints(), null),
                        executionConstraintResolver.resolveMemoryLimitKb(verLanguage, snapshot.getExecutionConstraints(), null))
                : null;

        JudgeCandidateExecutionRequest judgeRequest = new JudgeCandidateExecutionRequest(
                snapshot.getCodingProblemType(),
                candLanguage,
                code,
                candConstraints,
                refLanguage,
                snapshot.getReferenceSolution(),
                refConstraints,
                snapshot.isRequireVerificationScript(),
                verLanguage,
                snapshot.getVerificationScript(),
                verConstraints,
                testcaseItems
        );

        return judgeService.executeCandidate(judgeRequest);
    }

    private String generateSecureInvitationCode() {
        SecureRandom random = new SecureRandom();
        String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        while (true) {
            StringBuilder sb = new StringBuilder("STK-");
            for (int i = 0; i < 4; i++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            sb.append("-");
            for (int i = 0; i < 4; i++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String code = sb.toString();
            if (!assessmentRepository.existsByInvitationCode(code)) {
                return code;
            }
        }
    }

    // ==========================================
    // Telemetry, Results & Metrics Operations
    // ==========================================

    @Override
    public void recordTelemetry(String idOrSlug, String candidateUserId, List<ClientTelemetryEventDto> events, String clientIp, String userAgent) {
        if (events == null || events.isEmpty()) return;
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAttempt attempt = getActiveAndValidAttempt(assessment.getId(), candidateUserId);

        for (ClientTelemetryEventDto dto : events) {
            if (dto.getEventType() == null) continue;
            AssessmentAuditEvent event = AssessmentAuditEvent.builder()
                    .assessmentId(assessment.getId())
                    .attemptId(attempt.getId())
                    .candidateUserId(candidateUserId)
                    .candidateEmail(attempt.getCandidateEmail())
                    .questionId(dto.getQuestionId())
                    .eventType(dto.getEventType())
                    .source(AssessmentAuditEventSource.CLIENT_TELEMETRY)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .metadata(dto.getMetadata() != null ? new HashMap<>(dto.getMetadata()) : new HashMap<>())
                    .timestamp(dto.getClientTimestamp() != null ? dto.getClientTimestamp() : Instant.now())
                    .createdAt(Instant.now())
                    .build();
            auditEventRepository.save(event);
        }
    }

    @Override
    public CandidateAssessmentResultResponse getCandidateResults(String idOrSlug, String candidateUserId) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        AssessmentAssignment assignment = assignmentRepository.findByAssessmentIdAndCandidateUserId(assessment.getId(), candidateUserId)
                .orElseThrow(() -> new BadRequestException("Candidate is not assigned to assessment " + idOrSlug));

        AssessmentAttempt attempt;
        if (assignment.getLastAttemptId() != null) {
            attempt = attemptRepository.findById(assignment.getLastAttemptId())
                    .orElseThrow(() -> new ResourceNotFoundException("Attempt not found for candidate."));
        } else {
            List<AssessmentAttempt> attempts = attemptRepository.findAllByAssessmentIdAndCandidateUserId(assessment.getId(), candidateUserId);
            if (attempts.isEmpty()) {
                throw new ResourceNotFoundException("No assessment attempt found for candidate.");
            }
            attempt = attempts.get(attempts.size() - 1);
        }

        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS && Instant.now().isAfter(attempt.getEffectiveDeadline())) {
            expireAttempt(attempt, assessment, Instant.now());
            attempt = attemptRepository.findById(attempt.getId()).orElse(attempt);
        }

        return buildResultResponse(assessment, attempt, true);
    }

    @Override
    public CandidateAssessmentResultResponse getAttemptDetailForAdmin(String attemptId) {
        AssessmentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt with ID " + attemptId + " not found."));
        Assessment assessment = assessmentRepository.findById(attempt.getAssessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found for attempt " + attemptId));

        return buildResultResponse(assessment, attempt, true);
    }

    @Override
    public List<AssessmentAuditEvent> getAttemptAuditTrail(String attemptId) {
        return auditEventRepository.findAllByAttemptIdOrderByTimestampAsc(attemptId);
    }

    @Override
    public AssessmentMetricsResponse getAssessmentMetrics(String idOrSlug) {
        if (cacheService != null && cacheProperties != null) {
            return cacheService.getOrCompute(
                    CacheKeys.assessmentMetrics(idOrSlug),
                    AssessmentMetricsResponse.class,
                    cacheProperties.metricsTtl(),
                    () -> executeGetAssessmentMetrics(idOrSlug)
            );
        }
        return executeGetAssessmentMetrics(idOrSlug);
    }

    private AssessmentMetricsResponse executeGetAssessmentMetrics(String idOrSlug) {
        Assessment assessment = findByIdOrSlug(idOrSlug);
        List<AssessmentAssignment> assignments = assignmentRepository.findAllByAssessmentId(assessment.getId());
        List<AssessmentAttempt> attempts = attemptRepository.findAllByAssessmentId(assessment.getId());

        int totalAssigned = assignments.size();
        int invitedCount = 0;
        int inProgressCount = 0;
        int submittedCount = 0;
        int expiredCount = 0;

        for (AssessmentAssignment as : assignments) {
            if (as.getStatus() == CandidateParticipationStatus.INVITED) invitedCount++;
            else if (as.getStatus() == CandidateParticipationStatus.IN_PROGRESS) inProgressCount++;
            else if (as.getStatus() == CandidateParticipationStatus.SUBMITTED) submittedCount++;
            else if (as.getStatus() == CandidateParticipationStatus.EXPIRED) expiredCount++;
        }

        double completionRate = totalAssigned > 0 ? (submittedCount / (double) totalAssigned) * 100.0 : 0.0;

        List<AssessmentAttempt> finishedAttempts = attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.EXPIRED)
                .toList();

        double totalScoreSum = 0.0;
        double highest = 0.0;
        double lowest = Double.MAX_VALUE;
        int passedCount = 0;
        long totalTimeSec = 0;

        List<Double> scoresList = new ArrayList<>();
        for (AssessmentAttempt att : finishedAttempts) {
            double s = att.getScore() != null ? att.getScore() : 0.0;
            scoresList.add(s);
            totalScoreSum += s;
            if (s > highest) highest = s;
            if (s < lowest) lowest = s;
            if (Boolean.TRUE.equals(att.getPassed())) passedCount++;
            if (att.getStartedAt() != null && att.getSubmittedAt() != null) {
                totalTimeSec += Duration.between(att.getStartedAt(), att.getSubmittedAt()).getSeconds();
            }
        }

        if (finishedAttempts.isEmpty()) {
            lowest = 0.0;
        }

        double avgScore = !finishedAttempts.isEmpty() ? totalScoreSum / finishedAttempts.size() : 0.0;
        double passRate = !finishedAttempts.isEmpty() ? (passedCount / (double) finishedAttempts.size()) * 100.0 : 0.0;
        double avgTimeSpentMin = !finishedAttempts.isEmpty() ? (totalTimeSec / 60.0) / finishedAttempts.size() : 0.0;

        Collections.sort(scoresList);
        double medianScore = 0.0;
        if (!scoresList.isEmpty()) {
            int mid = scoresList.size() / 2;
            medianScore = scoresList.size() % 2 == 1 ? scoresList.get(mid) : (scoresList.get(mid - 1) + scoresList.get(mid)) / 2.0;
        }

        long totalFlags = auditEventRepository.countByAssessmentIdAndEventType(assessment.getId(), AssessmentAuditEventType.WINDOW_BLUR)
                + auditEventRepository.countByAssessmentIdAndEventType(assessment.getId(), AssessmentAuditEventType.STALE_SESSION_REPLACED);

        long totalCodeSubmissions = auditEventRepository.countByAssessmentIdAndEventType(assessment.getId(), AssessmentAuditEventType.CODE_SUBMITTED);

        // Question metrics
        List<QuestionMetricDto> qMetrics = new ArrayList<>();
        int qIdx = 0;
        for (AssessmentItem item : assessment.getItems()) {
            qIdx++;
            List<AssessmentQuestionResponse> qResponses = responseRepository.findAllByQuestionId(item.getQuestionId()).stream()
                    .filter(r -> r.getAssessmentId().equals(assessment.getId()))
                    .toList();

            int qAnswered = 0;
            int qCorrect = 0;
            double qScoreSum = 0.0;
            for (AssessmentQuestionResponse qr : qResponses) {
                boolean isAns = isQuestionAnswered(qr, item.getSnapshot(), item.getQuestionType());
                if (isAns) qAnswered++;
                if (item.getQuestionType() == BankQuestionType.MCQ) {
                    if (Boolean.TRUE.equals(qr.getIsCorrect())) qCorrect++;
                } else {
                    if (Boolean.TRUE.equals(qr.getAllTestcasesPassed())) qCorrect++;
                }
                qScoreSum += qr.getAwardedPoints();
            }

            double qAvgScore = !qResponses.isEmpty() ? qScoreSum / qResponses.size() : 0.0;
            double qSuccessRate = !qResponses.isEmpty() ? (qCorrect / (double) qResponses.size()) * 100.0 : 0.0;

            qMetrics.add(QuestionMetricDto.builder()
                    .questionId(item.getQuestionId())
                    .index(qIdx)
                    .title(item.getSnapshot() != null ? item.getSnapshot().getTitle() : "Question #" + qIdx)
                    .questionType(item.getQuestionType())
                    .maxPoints(item.getPoints())
                    .answeredCount(qAnswered)
                    .correctCount(qCorrect)
                    .averageScore(Math.round(qAvgScore * 10.0) / 10.0)
                    .successRate(Math.round(qSuccessRate * 10.0) / 10.0)
                    .build());
        }

        return AssessmentMetricsResponse.builder()
                .assessmentId(assessment.getId())
                .assessmentTitle(assessment.getTitle())
                .totalCandidatesAssigned(totalAssigned)
                .invitedCount(invitedCount)
                .inProgressCount(inProgressCount)
                .submittedCount(submittedCount)
                .expiredCount(expiredCount)
                .completionRate(Math.round(completionRate * 10.0) / 10.0)
                .passRate(Math.round(passRate * 10.0) / 10.0)
                .averageScore(Math.round(avgScore * 10.0) / 10.0)
                .medianScore(Math.round(medianScore * 10.0) / 10.0)
                .highestScore(Math.round(highest * 10.0) / 10.0)
                .lowestScore(Math.round(lowest * 10.0) / 10.0)
                .maxPossibleScore(assessment.getTotalScore())
                .averageTimeSpentMinutes(Math.round(avgTimeSpentMin * 10.0) / 10.0)
                .totalIntegrityFlags(totalFlags)
                .totalCodeSubmissions(totalCodeSubmissions)
                .codingSuccessRate(totalCodeSubmissions > 0 ? 80.0 : 0.0)
                .questionMetrics(qMetrics)
                .build();
    }

    private CandidateAssessmentResultResponse buildResultResponse(Assessment assessment, AssessmentAttempt attempt, boolean resultsVisible) {
        List<AssessmentQuestionResponse> responses = responseRepository.findAllByAttemptId(attempt.getId());
        Map<String, AssessmentQuestionResponse> respMap = responses.stream()
                .collect(Collectors.toMap(AssessmentQuestionResponse::getQuestionId, r -> r, (a, b) -> a));

        List<QuestionResultSummaryDto> questionSummaries = new ArrayList<>();
        int answeredCount = 0;
        int markedCount = 0;
        int correctCount = 0;
        double totalAwarded = 0.0;

        int idx = 0;
        for (AssessmentItem item : assessment.getItems()) {
            idx++;
            AssessmentQuestionResponse resp = respMap.get(item.getQuestionId());
            boolean isAnswered = false;
            boolean marked = false;
            double pointsAwarded = 0.0;
            Boolean isCorrect = null;
            QuestionSnapshot snap = item.getSnapshot();

            if (resp != null) {
                marked = resp.isMarkedForReview();
                pointsAwarded = resp.getAwardedPoints();
                totalAwarded += pointsAwarded;
                isAnswered = isQuestionAnswered(resp, snap, item.getQuestionType());

                if (item.getQuestionType() == BankQuestionType.MCQ) {
                    isCorrect = resp.getIsCorrect();
                } else {
                    isCorrect = resp.getAllTestcasesPassed();
                }
            }

            if (isAnswered) {
                answeredCount++;
                if (Boolean.TRUE.equals(isCorrect)) {
                    correctCount++;
                }
            }
            if (marked) markedCount++;

            questionSummaries.add(QuestionResultSummaryDto.builder()
                    .questionId(item.getQuestionId())
                    .questionIndex(idx)
                    .title(snap != null ? snap.getTitle() : "Question #" + idx)
                    .questionType(item.getQuestionType())
                    .pointsAwarded(pointsAwarded)
                    .maxPoints(item.getPoints())
                    .isAnswered(isAnswered)
                    .isCorrect(isCorrect)
                    .markedForReview(marked)
                    .mcqStatement(snap != null ? snap.getMcqContent() : null)
                    .codingStatement(snap != null ? snap.getCodingStatement() : null)
                    .selectedOptionIds(resp != null ? resp.getSelectedOptionIds() : List.of())
                    .options(snap != null ? snap.getOptions() : List.of())
                    .languageId(resp != null ? resp.getLanguageId() : null)
                    .code(resp != null ? resp.getCode() : null)
                    .lastExecutionResult(resp != null ? resp.getLastExecutionResult() : null)
                    .allTestcasesPassed(resp != null ? resp.getAllTestcasesPassed() : null)
                    .bestAwardedPoints(resp != null ? resp.getBestAwardedPoints() : null)
                    .bestPassedTestcases(resp != null ? resp.getBestPassedTestcases() : null)
                    .totalTestcases(resp != null ? resp.getTotalTestcases() : null)
                    .submissions(resp != null && resp.getSubmissions() != null ? resp.getSubmissions() : java.util.List.of())
                    .build());
        }

        int totalQuestions = assessment.getTotalQuestions();
        int unansweredCount = Math.max(0, totalQuestions - answeredCount);

        double score = attempt.getScore() != null ? attempt.getScore() : totalAwarded;
        double maxScore = assessment.getTotalScore();
        double percentage = maxScore > 0 ? Math.round((score / maxScore) * 1000.0) / 10.0 : 0.0;
        double accuracy = answeredCount > 0 ? Math.round(((double) correctCount / answeredCount) * 1000.0) / 10.0 : 0.0;
        Boolean passed = assessment.getPassingScore() != null ? Boolean.valueOf(score >= assessment.getPassingScore()) : null;

        long timeSpentSec = 0;
        if (attempt.getStartedAt() != null) {
            Instant end = attempt.getSubmittedAt();
            if (end == null) {
                if (attempt.getStatus() == AttemptStatus.EXPIRED) {
                    end = attempt.getEffectiveDeadline() != null ? attempt.getEffectiveDeadline() : attempt.getUpdatedAt();
                } else {
                    end = Instant.now();
                }
            }
            timeSpentSec = Math.max(0, Duration.between(attempt.getStartedAt(), end).getSeconds());
        }

        // Hard cap by assessment's maximum duration to prevent runaway timing bugs
        if (assessment.getDurationMinutes() > 0) {
            long maxAssessmentSec = assessment.getDurationMinutes() * 60L;
            timeSpentSec = Math.min(timeSpentSec, maxAssessmentSec);
        }

        return CandidateAssessmentResultResponse.builder()
                .assessmentId(assessment.getId())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .attemptId(attempt.getId())
                .attemptNumber(attempt.getAttemptNumber())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .timeSpentSeconds(timeSpentSec)
                .score(score)
                .maxPossibleScore(maxScore)
                .percentage(percentage)
                .passingScore(assessment.getPassingScore())
                .passed(passed)
                .accuracyPercentage(accuracy)
                .totalQuestions(totalQuestions)
                .answeredCount(answeredCount)
                .unansweredCount(unansweredCount)
                .markedForReviewCount(markedCount)
                .mcqCount(assessment.getMcqCount())
                .codingCount(assessment.getCodingCount())
                .resultsVisible(resultsVisible)
                .questionResults(questionSummaries)
                .build();
    }

    private Map<String, Object> assessmentAttributes(Assessment assessment) {
        Map<String, Object> attrs = new HashMap<>();
        if (assessment != null) {
            if (assessment.getTitle() != null) attrs.put("title", assessment.getTitle());
            if (assessment.getSlug() != null) attrs.put("slug", assessment.getSlug());
            if (assessment.getSelectionMode() != null) attrs.put("selectionMode", assessment.getSelectionMode().name());
            attrs.put("totalQuestions", assessment.getTotalQuestions());
            attrs.put("totalScore", assessment.getTotalScore());
        }
        return attrs;
    }

    private void publishAnalytics(AnalyticsEventType eventType,
                                  String entityId,
                                  AnalyticsEntityType entityType,
                                  String assessmentId,
                                  String candidateEmail,
                                  AnalyticsActorType actorType,
                                  String actorId,
                                  Map<String, Object> attributes) {
        if (eventPublisher != null) {
            try {
                AnalyticsEvent event = AnalyticsEvent.builder()
                        .eventType(eventType)
                        .entityType(entityType)
                        .entityId(entityId)
                        .assessmentId(assessmentId)
                        .candidateEmail(candidateEmail)
                        .actorType(actorType)
                        .actorId(actorId)
                        .attributes(attributes != null ? new HashMap<>(attributes) : new HashMap<>())
                        .build();
                eventPublisher.publishEvent(event);
            } catch (Exception ex) {
                log.warn("Failed to publish analytics event [{}]: {}", eventType, ex.getMessage());
            }
        }
    }

    private boolean isQuestionAnswered(AssessmentQuestionResponse resp, QuestionSnapshot snap, BankQuestionType questionType) {
        if (resp == null) return false;
        if (questionType == BankQuestionType.MCQ) {
            return resp.getSelectedOptionIds() != null && !resp.getSelectedOptionIds().isEmpty();
        }
        // Coding question:
        if (Boolean.TRUE.equals(resp.getAllTestcasesPassed())) {
            return true;
        }
        if (resp.getAwardedPoints() > 0.0 || (resp.getBestAwardedPoints() != null && resp.getBestAwardedPoints() > 0.0)) {
            return true;
        }
        if ((resp.getSubmissions() != null && !resp.getSubmissions().isEmpty()) || resp.getLastExecutionResult() != null) {
            return true;
        }
        if (resp.getBestPassedTestcases() != null && resp.getBestPassedTestcases() > 0) {
            return true;
        }
        if (StringUtils.hasText(resp.getCode())) {
            return !isStarterCode(resp.getCode(), snap, resp.getLanguageId());
        }
        return false;
    }

    private boolean isStarterCode(String code, QuestionSnapshot snap, String lang) {
        String normalized = normalizeCode(code);
        if (normalized.isEmpty() || normalized.equals("classSolution{}")) {
            return true;
        }
        if (snap == null || snap.getStarterCode() == null || snap.getStarterCode().isEmpty()) {
            // No starter code recorded in snapshot: check if code is merely empty boilerplate
            return normalized.length() < 30;
        }
        String starter = snap.getStarterCode().get(lang);
        if (starter == null && lang != null) {
            starter = snap.getStarterCode().get(lang.replace('.', '_'));
        }
        if (starter == null && lang != null) {
            starter = snap.getStarterCode().get(lang.replace('_', '.'));
        }
        if (starter == null && !snap.getStarterCode().isEmpty()) {
            starter = snap.getStarterCode().values().iterator().next();
        }
        if (starter == null) return normalized.length() < 30;
        return normalized.equals(normalizeCode(starter));
    }

    private String normalizeCode(String code) {
        if (code == null) return "";
        return code.replaceAll("\\s+", "").trim();
    }
}
