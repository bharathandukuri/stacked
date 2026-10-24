package com.bharath.stacked.modules.coding_problem.service.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.CustomTestcaseRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.ExecutionConstraintRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.RunCodingProblemPreviewRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.RunSampleTestcasesRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.RunTestcaseGeneratorRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.SubmitCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.UpdateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.PreviewRunResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.TestcaseGeneratorRunResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.CandidateTestcaseItem;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.InitializedTestcases;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.JudgeCandidateExecutionRequest;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.JudgeInitializationRequest;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.JudgePreviewRequest;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.model.TestCase;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.coding_problem.service.CodingProblemService;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.common.event.QuestionDeletedEvent;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.assessment.model.Assessment;
import com.bharath.stacked.modules.assessment.repository.AssessmentQuestionResponseRepository;
import com.bharath.stacked.modules.assessment.repository.AssessmentRepository;
import com.bharath.stacked.modules.coding_problem.dto.request.CodingProblemFilterRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemSummaryResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.SupportedLanguageResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemDifficulty;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService;
import com.bharath.stacked.modules.coding_problem.service.CodingProblemService;
import com.bharath.stacked.modules.execution.resolver.ExecutionConstraintResolver;
import com.bharath.stacked.modules.file.service.FileService;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.LanguageType;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import com.bharath.stacked.modules.question_bank.model.QuestionBank;
import com.bharath.stacked.modules.question_bank.repository.QuestionBankRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CodingProblemServiceImpl implements CodingProblemService {

    private final CodingProblemRepository codingProblemRepository;
    private final MongoTemplate mongoTemplate;
    private final LanguageRegistry languageRegistry;
    private final ExecutionConstraintResolver executionConstraintResolver;
    private final CodingProblemJudgeService codingProblemJudgeService;
    private final FileService fileService;
    private final ApplicationEventPublisher eventPublisher;
    private final QuestionBankRepository questionBankRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionResponseRepository assessmentQuestionResponseRepository;

    public CodingProblemServiceImpl(CodingProblemRepository codingProblemRepository,
                                  MongoTemplate mongoTemplate,
                                  LanguageRegistry languageRegistry,
                                  ExecutionConstraintResolver executionConstraintResolver,
                                  CodingProblemJudgeService codingProblemJudgeService,
                                  FileService fileService,
                                  ApplicationEventPublisher eventPublisher,
                                  QuestionBankRepository questionBankRepository,
                                  AssessmentRepository assessmentRepository) {
        this(codingProblemRepository, mongoTemplate, languageRegistry, executionConstraintResolver,
                codingProblemJudgeService, fileService, eventPublisher, questionBankRepository,
                assessmentRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CodingProblemServiceImpl(CodingProblemRepository codingProblemRepository,
                                  MongoTemplate mongoTemplate,
                                  LanguageRegistry languageRegistry,
                                  ExecutionConstraintResolver executionConstraintResolver,
                                  CodingProblemJudgeService codingProblemJudgeService,
                                  FileService fileService,
                                  ApplicationEventPublisher eventPublisher,
                                  QuestionBankRepository questionBankRepository,
                                  AssessmentRepository assessmentRepository,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false)
                                  AssessmentQuestionResponseRepository assessmentQuestionResponseRepository) {
        this.codingProblemRepository = codingProblemRepository;
        this.mongoTemplate = mongoTemplate;
        this.languageRegistry = languageRegistry;
        this.executionConstraintResolver = executionConstraintResolver;
        this.codingProblemJudgeService = codingProblemJudgeService;
        this.fileService = fileService;
        this.eventPublisher = eventPublisher;
        this.questionBankRepository = questionBankRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionResponseRepository = assessmentQuestionResponseRepository;
    }

    @Override
    public PageResponse<CodingProblemSummaryResponse> findAll(CodingProblemFilterRequest filter) {
        if (filter == null) {
            filter = new CodingProblemFilterRequest();
        }

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (filter.getStatus() != null) {
            criteriaList.add(Criteria.where("status").is(filter.getStatus()));
        }

        if (filter.getProblemType() != null) {
            criteriaList.add(Criteria.where("problemType").is(filter.getProblemType()));
        }

        if (filter.getDifficulty() != null) {
            criteriaList.add(Criteria.where("difficulty").is(filter.getDifficulty()));
        }

        if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
            String sanitizedSearch = Pattern.quote(filter.getSearch().trim());
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(sanitizedSearch, "i"),
                    Criteria.where("slug").regex(sanitizedSearch, "i")
            ));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long totalElements = mongoTemplate.count(query, CodingProblem.class);

        int page = Math.max(0, filter.getPage());
        int size = filter.getSize() <= 0 ? 20 : Math.min(filter.getSize(), 100);

        String sortBy = filter.getSortBy();
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        query.with(PageRequest.of(page, size, Sort.by(direction, sortBy)));

        List<CodingProblem> problems = mongoTemplate.find(query, CodingProblem.class);
        List<CodingProblemSummaryResponse> items = problems.stream()
                .map(CodingProblemSummaryResponse::fromEntity)
                .toList();

        return PageResponse.of(items, page, size, totalElements);
    }

    @Override
    public CodingProblem findByIdOrSlug(String idOrSlug) {
        if (idOrSlug == null || idOrSlug.isBlank()) {
            throw new BadRequestException("Coding problem identifier cannot be empty.");
        }
        return codingProblemRepository.findById(idOrSlug)
                .or(() -> codingProblemRepository.findBySlug(idOrSlug))
                .orElseThrow(() -> new ResourceNotFoundException("CodingProblem", "id or slug", idOrSlug));
    }

    private record ValidatedProblemSetup(
            Language refLanguage,
            Language verLanguage,
            Map<String, CodeExecutionConstraints> resolvedConstraints,
            InitializedTestcases initializedTestcases,
            Set<String> imageFileIds
    ) {}

    private ValidatedProblemSetup validateAndPrepareProblem(
            CodingProblemType problemType,
            List<String> supportedLanguageIds,
            String referenceSolutionLanguage,
            String referenceSolution,
            Map<String, String> starterCode,
            boolean requireVerificationScript,
            String verificationScriptLanguage,
            String verificationScript,
            Map<String, ExecutionConstraintRequest> executionConstraints,
            List<com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest> sampleTestcases,
            List<com.bharath.stacked.modules.coding_problem.dto.request.TestcaseRequest> hiddenTestcases,
            String statement,
            List<String> hints,
            List<String> rawImageFileIds
    ) {
        // 1. Validate supported languages and problem type compatibility
        if (problemType == CodingProblemType.DATABASE && supportedLanguageIds.size() != 1) {
            throw new BadRequestException("A DATABASE problem must support exactly one database language.");
        }

        for (String langId : supportedLanguageIds) {
            if (!languageRegistry.isSupported(langId)) {
                throw new BadRequestException("Language '" + langId + "' is not supported by the platform.");
            }
            Language lang = languageRegistry.get(langId);
            if (problemType == CodingProblemType.DATABASE && lang.type() != LanguageType.DATABASE) {
                throw new BadRequestException("Language '" + langId + "' is not a database language for a DATABASE problem.");
            }
            if (problemType == CodingProblemType.GENERIC && lang.type() == LanguageType.DATABASE) {
                throw new BadRequestException("Database language '" + langId + "' cannot be used for a GENERIC problem.");
            }
        }

        // 2. Validate reference solution language and source code
        if (referenceSolutionLanguage == null || referenceSolutionLanguage.isBlank()) {
            throw new BadRequestException("Reference solution language is required.");
        }
        if (referenceSolution == null || referenceSolution.isBlank()) {
            throw new BadRequestException("Reference solution cannot be empty.");
        }
        if (!supportedLanguageIds.contains(referenceSolutionLanguage)) {
            throw new BadRequestException("Reference solution language '" + referenceSolutionLanguage
                    + "' must be included in supportedLanguageIds.");
        }
        Language refLanguage = languageRegistry.get(referenceSolutionLanguage);

        // 3. Validate starter code mapping
        if (starterCode != null) {
            for (String langId : starterCode.keySet()) {
                if (!languageRegistry.isSupported(langId) || !supportedLanguageIds.contains(langId)) {
                    throw new BadRequestException("Starter code provided for unsupported language: '" + langId + "'.");
                }
            }
        }

        // 4. Validate verification script if required
        Language verLanguage = null;
        if (requireVerificationScript) {
            if (verificationScriptLanguage == null || verificationScriptLanguage.isBlank()) {
                throw new BadRequestException("Verification script language is required.");
            }
            if (verificationScript == null || verificationScript.isBlank()) {
                throw new BadRequestException("Verification script is required.");
            }
            if (!languageRegistry.isSupported(verificationScriptLanguage)) {
                throw new BadRequestException("Verification script language '" + verificationScriptLanguage
                        + "' is not supported.");
            }
            verLanguage = languageRegistry.get(verificationScriptLanguage);
            if (problemType == CodingProblemType.GENERIC && verLanguage.type() == LanguageType.DATABASE) {
                throw new BadRequestException("Database language '" + verificationScriptLanguage
                        + "' cannot be used as verification script for a GENERIC problem.");
            }
            if (problemType == CodingProblemType.DATABASE) {
                if (verLanguage.type() != LanguageType.DATABASE) {
                    throw new BadRequestException("Verification script for a DATABASE problem must be a database language.");
                }
                if (!verificationScriptLanguage.equalsIgnoreCase(referenceSolutionLanguage)) {
                    throw new BadRequestException("Verification script language '" + verificationScriptLanguage
                            + "' must match reference solution language '" + referenceSolutionLanguage
                            + "' for a DATABASE problem.");
                }
            }
        }

        // 5. Validate and resolve per-language execution constraints
        Map<String, CodeExecutionConstraints> resolvedConstraints = new HashMap<>();

        if (executionConstraints != null) {
            for (Map.Entry<String, ExecutionConstraintRequest> entry : executionConstraints.entrySet()) {
                String langId = entry.getKey();
                if (!languageRegistry.isSupported(langId)) {
                    throw new BadRequestException("Execution constraints provided for unsupported language: '" + langId + "'.");
                }
                boolean inSupported = supportedLanguageIds.contains(langId);
                boolean isVerLang = requireVerificationScript && langId.equals(verificationScriptLanguage);
                if (!inSupported && !isVerLang) {
                    throw new BadRequestException("Execution constraints provided for language '" + langId + "' not in supportedLanguageIds.");
                }
            }
        }

        for (String langId : supportedLanguageIds) {
            Language lang = languageRegistry.get(langId);
            ExecutionConstraintRequest userReq = executionConstraints != null
                    ? executionConstraints.get(langId)
                    : null;
            CodeExecutionConstraints userConstraints = userReq != null ? userReq.toCodeExecutionConstraints() : null;

            long timeLimitMs = executionConstraintResolver.resolveTimeLimitMs(lang, userConstraints, null);
            long memoryLimitKb = executionConstraintResolver.resolveMemoryLimitKb(lang, userConstraints, null);
            resolvedConstraints.put(langId, new CodeExecutionConstraints(timeLimitMs, memoryLimitKb));
        }

        if (requireVerificationScript && !resolvedConstraints.containsKey(verificationScriptLanguage)) {
            Language lang = languageRegistry.get(verificationScriptLanguage);
            ExecutionConstraintRequest userReq = executionConstraints != null
                    ? executionConstraints.get(verificationScriptLanguage)
                    : null;
            CodeExecutionConstraints userConstraints = userReq != null ? userReq.toCodeExecutionConstraints() : null;

            long timeLimitMs = executionConstraintResolver.resolveTimeLimitMs(lang, userConstraints, null);
            long memoryLimitKb = executionConstraintResolver.resolveMemoryLimitKb(lang, userConstraints, null);
            resolvedConstraints.put(verificationScriptLanguage, new CodeExecutionConstraints(timeLimitMs, memoryLimitKb));
        }

        CodeExecutionConstraints refConstraints = resolvedConstraints.get(referenceSolutionLanguage);
        CodeExecutionConstraints verConstraints = requireVerificationScript
                ? resolvedConstraints.get(verificationScriptLanguage)
                : null;

        // 6. Initialize expected outputs via dedicated judge service
        JudgeInitializationRequest judgeRequest = new JudgeInitializationRequest(
                problemType,
                refLanguage,
                referenceSolution,
                refConstraints,
                requireVerificationScript,
                verLanguage,
                verificationScript,
                verConstraints,
                sampleTestcases,
                hiddenTestcases
        );

        InitializedTestcases initializedTestcases = codingProblemJudgeService.initializeExpectedOutputs(judgeRequest);

        // 7. Collect image file IDs from statement and hints
        Set<String> imageFileIds = new HashSet<>();
        Pattern filePattern = Pattern.compile("(?:/api/files/|/files/)([a-zA-Z0-9_-]{8,64})(?:/content)?");
        if (statement != null) {
            Matcher matcher = filePattern.matcher(statement);
            while (matcher.find()) {
                imageFileIds.add(matcher.group(1));
            }
        }
        if (hints != null) {
            for (String hint : hints) {
                if (hint != null) {
                    Matcher matcher = filePattern.matcher(hint);
                    while (matcher.find()) {
                        imageFileIds.add(matcher.group(1));
                    }
                }
            }
        }
        if (rawImageFileIds != null) {
            for (String rawId : rawImageFileIds) {
                if (rawId != null && !rawId.isBlank()) {
                    // Only retain if it actually exists in statement or hints,
                    // or if both statement and hints are empty (e.g. in minimal testing scenarios)
                    boolean referencedInText = (statement != null && statement.contains(rawId))
                            || (hints != null && hints.stream().anyMatch(h -> h != null && h.contains(rawId)));
                    if (referencedInText || (statement == null && (hints == null || hints.isEmpty()))) {
                        imageFileIds.add(rawId);
                    }
                }
            }
        }

        return new ValidatedProblemSetup(refLanguage, verLanguage, resolvedConstraints, initializedTestcases, imageFileIds);
    }

    private void promoteImages(Set<String> imageFileIds, String slug) {
        for (String fileId : imageFileIds) {
            try {
                fileService.moveToPermanent(fileId);
                log.info("Promoted image [{}] to permanent storage for problem [{}]", fileId, slug);
            } catch (Exception ex) {
                log.warn("Failed to promote image [{}] to permanent storage: {}", fileId, ex.getMessage());
            }
        }
    }

    private void cleanRemovedImages(Collection<String> oldImageIds, Set<String> newImageIds, String problemId) {
        if (oldImageIds == null || oldImageIds.isEmpty()) {
            return;
        }
        Set<String> removedIds = new HashSet<>(oldImageIds);
        if (newImageIds != null) {
            removedIds.removeAll(newImageIds);
        }
        for (String fileId : removedIds) {
            try {
                if (codingProblemRepository.isImageUsedByOtherProblem(fileId, problemId)) {
                    log.info("Image [{}] retained because it is used by another problem", fileId);
                    continue;
                }
                fileService.deleteFile(fileId);
                log.info("Deleted orphaned image [{}] removed from problem [{}]", fileId, problemId);
            } catch (Exception ex) {
                log.warn("Failed to delete removed image [{}] for problem [{}]: {}", fileId, problemId, ex.getMessage());
            }
        }
    }

    @Transactional
    @Override
    public CodingProblem create(CreateCodingProblemRequest request) {
        // 1. Validate uniqueness of slug
        if (codingProblemRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new ConflictException("CodingProblem", "slug", request.getSlug());
        }

        ValidatedProblemSetup setup = validateAndPrepareProblem(
                request.getProblemType(),
                request.getSupportedLanguageIds(),
                request.getReferenceSolutionLanguage(),
                request.getReferenceSolution(),
                request.getStarterCode(),
                request.isRequireVerificationScript(),
                request.getVerificationScriptLanguage(),
                request.getVerificationScript(),
                request.getExecutionConstraints(),
                request.getSampleTestcases(),
                request.getHiddenTestcases(),
                request.getStatement(),
                request.getHints(),
                request.getImageFileIds()
        );

        promoteImages(setup.imageFileIds(), request.getSlug());

        CodingProblem codingProblem = CodingProblem.builder()
                .title(request.getTitle())
                .slug(request.getSlug())
                .status(CodingProblemStatus.UNPUBLISHED)
                .problemType(request.getProblemType())
                .difficulty(request.getDifficulty())
                .supportedLanguageIds(request.getSupportedLanguageIds())
                .statement(request.getStatement())
                .topics(request.getTopics())
                .hints(request.getHints())
                .starterCode(request.getStarterCode())
                .referenceSolutionLanguage(request.getReferenceSolutionLanguage())
                .referenceSolution(request.getReferenceSolution())
                .requireVerificationScript(request.isRequireVerificationScript())
                .verificationScriptLanguage(request.getVerificationScriptLanguage())
                .verificationScript(request.getVerificationScript())
                .sampleTestCases(setup.initializedTestcases().sampleTestCases())
                .hiddenTestCases(setup.initializedTestcases().hiddenTestCases())
                .executionConstraints(setup.resolvedConstraints())
                .generatorLanguage(request.getGeneratorLanguage())
                .generatorScript(request.getGeneratorScript())
                .generatorStdin(request.getGeneratorStdin())
                .generatorDelimiter(request.getGeneratorDelimiter())
                .imageFileIds(new ArrayList<>(setup.imageFileIds()))
                .build();

        return codingProblemRepository.save(codingProblem);
    }

    @Transactional
    @Override
    public CodingProblem update(String idOrSlug, UpdateCodingProblemRequest request) {
        CodingProblem existing = findByIdOrSlug(idOrSlug);

        if (existing.getStatus() == CodingProblemStatus.PUBLISHED) {
            throw new ConflictException("Cannot edit coding problem '" + existing.getTitle() + "' because it is currently PUBLISHED. Unpublish to DRAFT first.");
        }

        // Validate slug uniqueness against other problems
        if (!existing.getSlug().equals(request.getSlug())) {
            Optional<CodingProblem> duplicate = codingProblemRepository.findBySlug(request.getSlug());
            if (duplicate.isPresent() && !duplicate.get().getId().equals(existing.getId())) {
                throw new ConflictException("CodingProblem", "slug", request.getSlug());
            }
        }

        List<String> oldImageIds = existing.getImageFileIds();

        ValidatedProblemSetup setup = validateAndPrepareProblem(
                request.getProblemType(),
                request.getSupportedLanguageIds(),
                request.getReferenceSolutionLanguage(),
                request.getReferenceSolution(),
                request.getStarterCode(),
                request.isRequireVerificationScript(),
                request.getVerificationScriptLanguage(),
                request.getVerificationScript(),
                request.getExecutionConstraints(),
                request.getSampleTestcases(),
                request.getHiddenTestcases(),
                request.getStatement(),
                request.getHints(),
                request.getImageFileIds()
        );

        promoteImages(setup.imageFileIds(), request.getSlug());
        cleanRemovedImages(oldImageIds, setup.imageFileIds(), existing.getId());

        existing.setTitle(request.getTitle());
        existing.setSlug(request.getSlug());
        existing.setProblemType(request.getProblemType());
        existing.setDifficulty(request.getDifficulty());
        existing.setSupportedLanguageIds(request.getSupportedLanguageIds());
        existing.setStatement(request.getStatement());
        existing.setTopics(request.getTopics());
        existing.setHints(request.getHints());
        existing.setStarterCode(request.getStarterCode());
        existing.setReferenceSolutionLanguage(request.getReferenceSolutionLanguage());
        existing.setReferenceSolution(request.getReferenceSolution());
        existing.setRequireVerificationScript(request.isRequireVerificationScript());
        existing.setVerificationScriptLanguage(request.isRequireVerificationScript() ? request.getVerificationScriptLanguage() : null);
        existing.setVerificationScript(request.isRequireVerificationScript() ? request.getVerificationScript() : null);
        existing.setSampleTestCases(setup.initializedTestcases().sampleTestCases());
        existing.setHiddenTestCases(setup.initializedTestcases().hiddenTestCases());
        existing.setExecutionConstraints(setup.resolvedConstraints());
        existing.setGeneratorLanguage(request.getGeneratorLanguage());
        existing.setGeneratorScript(request.getGeneratorScript());
        existing.setGeneratorStdin(request.getGeneratorStdin());
        existing.setGeneratorDelimiter(request.getGeneratorDelimiter());
        existing.setImageFileIds(new ArrayList<>(setup.imageFileIds()));

        return codingProblemRepository.save(existing);
    }

    @Override
    public CodingProblem getProblemDetails(String idOrSlug) {
        return findByIdOrSlug(idOrSlug);
    }

    @Transactional
    @Override
    public void delete(String idOrSlug) {
        CodingProblem existing = findByIdOrSlug(idOrSlug);

        if (isInUse(existing.getId())) {
            List<QuestionBank> banks = questionBankRepository.findAllByItemsQuestionId(existing.getId());
            List<Assessment> assessments = assessmentRepository.findAllByItemsQuestionId(existing.getId());
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
            throw new ConflictException("Cannot delete coding problem '" + existing.getTitle() + "' because it is currently in use by: " + String.join("; ", usages) + ".");
        }

        cleanRemovedImages(existing.getImageFileIds(), Collections.emptySet(), existing.getId());
        codingProblemRepository.delete(existing);
        eventPublisher.publishEvent(new QuestionDeletedEvent(existing.getId()));
        log.info("Deleted coding problem [{}] ({})", existing.getId(), existing.getSlug());
    }

    @Override
    public PreviewRunResponse runPreview(RunCodingProblemPreviewRequest request) {
        if (request == null) {
            throw new BadRequestException("Run preview request cannot be null.");
        }
        if (request.getProblemType() == null) {
            throw new BadRequestException("Problem type is required.");
        }
        if (request.getReferenceSolutionLanguage() == null || request.getReferenceSolutionLanguage().isBlank()) {
            throw new BadRequestException("Reference solution language is required.");
        }
        if (request.getReferenceSolution() == null || request.getReferenceSolution().isBlank()) {
            throw new BadRequestException("Reference solution cannot be empty.");
        }
        if (!languageRegistry.isSupported(request.getReferenceSolutionLanguage())) {
            throw new BadRequestException("Reference solution language '" + request.getReferenceSolutionLanguage() + "' is not supported.");
        }

        Language refLanguage = languageRegistry.get(request.getReferenceSolutionLanguage());
        if (request.getProblemType() == CodingProblemType.DATABASE && refLanguage.type() != LanguageType.DATABASE) {
            throw new BadRequestException("Reference solution language must be a database language for a DATABASE problem.");
        }
        if (request.getProblemType() == CodingProblemType.GENERIC && refLanguage.type() == LanguageType.DATABASE) {
            throw new BadRequestException("Database language cannot be used as reference solution for a GENERIC problem.");
        }

        // Validate verification script if enabled
        Language verLanguage = null;
        CodeExecutionConstraints verConstraints = null;
        if (request.isRequireVerificationScript()) {
            if (request.getVerificationScriptLanguage() == null || request.getVerificationScriptLanguage().isBlank()) {
                throw new BadRequestException("Verification script language is required when verification script is enabled.");
            }
            if (request.getVerificationScript() == null || request.getVerificationScript().isBlank()) {
                throw new BadRequestException("Verification script cannot be empty when verification script is enabled.");
            }
            if (!languageRegistry.isSupported(request.getVerificationScriptLanguage())) {
                throw new BadRequestException("Verification script language '" + request.getVerificationScriptLanguage() + "' is not supported.");
            }
            verLanguage = languageRegistry.get(request.getVerificationScriptLanguage());

            if (request.getProblemType() == CodingProblemType.DATABASE) {
                if (verLanguage.type() != LanguageType.DATABASE) {
                    throw new BadRequestException("Verification script for a DATABASE problem must be a database language.");
                }
                if (!request.getVerificationScriptLanguage().equalsIgnoreCase(request.getReferenceSolutionLanguage())) {
                    throw new BadRequestException("Verification script language must match reference solution language for a DATABASE problem.");
                }
            }
            if (request.getProblemType() == CodingProblemType.GENERIC && verLanguage.type() == LanguageType.DATABASE) {
                throw new BadRequestException("Database language cannot be used as verification script for a GENERIC problem.");
            }

            ExecutionConstraintRequest userVerReq = request.getExecutionConstraints() != null
                    ? request.getExecutionConstraints().get(request.getVerificationScriptLanguage())
                    : null;
            CodeExecutionConstraints userVerConstraints = userVerReq != null ? userVerReq.toCodeExecutionConstraints() : null;
            long verTimeLimitMs = executionConstraintResolver.resolveTimeLimitMs(verLanguage, userVerConstraints, null);
            long verMemoryLimitKb = executionConstraintResolver.resolveMemoryLimitKb(verLanguage, userVerConstraints, null);
            verConstraints = new CodeExecutionConstraints(verTimeLimitMs, verMemoryLimitKb);
        }

        // Resolve reference solution constraints
        ExecutionConstraintRequest userRefReq = request.getExecutionConstraints() != null
                ? request.getExecutionConstraints().get(request.getReferenceSolutionLanguage())
                : null;
        CodeExecutionConstraints userRefConstraints = userRefReq != null ? userRefReq.toCodeExecutionConstraints() : null;
        long refTimeLimitMs = executionConstraintResolver.resolveTimeLimitMs(refLanguage, userRefConstraints, null);
        long refMemoryLimitKb = executionConstraintResolver.resolveMemoryLimitKb(refLanguage, userRefConstraints, null);
        CodeExecutionConstraints refConstraints = new CodeExecutionConstraints(refTimeLimitMs, refMemoryLimitKb);

        if (request.getTestcases() == null || request.getTestcases().isEmpty()) {
            throw new BadRequestException("At least one testcase is required to run.");
        }

        JudgePreviewRequest judgeRequest = new JudgePreviewRequest(
                request.getProblemType(),
                refLanguage,
                request.getReferenceSolution(),
                refConstraints,
                request.isRequireVerificationScript(),
                verLanguage,
                request.getVerificationScript(),
                verConstraints,
                request.getTestcases()
        );

        return codingProblemJudgeService.previewExecution(judgeRequest);
    }

    @Override
    public CodingProblemExecutionResponse runSampleTestcases(String idOrSlug, RunSampleTestcasesRequest request) {
        if (idOrSlug == null || idOrSlug.isBlank()) {
            throw new BadRequestException("Problem identifier cannot be blank.");
        }
        if (request == null) {
            throw new BadRequestException("Request body cannot be null.");
        }
        if (request.getLanguageId() == null || request.getLanguageId().isBlank()) {
            throw new BadRequestException("Language ID is required.");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("Code cannot be empty.");
        }

        CodingProblem problem = findByIdOrSlug(idOrSlug);
        ResolvedExecutionSetup setup = resolveCandidateExecutionSetup(problem, request.getLanguageId());

        List<CandidateTestcaseItem> testcases = new ArrayList<>();
        int caseIdx = 1;
        if (problem.getSampleTestCases() != null) {
            for (TestCase tc : problem.getSampleTestCases()) {
                testcases.add(new CandidateTestcaseItem(
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

        if (request.getCustomTestcases() != null) {
            for (CustomTestcaseRequest custom : request.getCustomTestcases()) {
                if (custom != null) {
                    testcases.add(new CandidateTestcaseItem(
                            custom.getId() != null ? custom.getId() : "custom-" + caseIdx,
                            caseIdx++,
                            custom.getStdin(),
                            custom.getDbInit(),
                            null, // expectedOutput dynamically computed via reference solution
                            true,
                            false
                    ));
                }
            }
        }

        if (testcases.isEmpty()) {
            throw new BadRequestException("No testcases available to run.");
        }

        JudgeCandidateExecutionRequest judgeRequest = new JudgeCandidateExecutionRequest(
                problem.getProblemType(),
                setup.candLanguage(),
                request.getCode(),
                setup.candConstraints(),
                setup.refLanguage(),
                problem.getReferenceSolution(),
                setup.refConstraints(),
                problem.isRequireVerificationScript(),
                setup.verLanguage(),
                problem.getVerificationScript(),
                setup.verConstraints(),
                testcases
        );

        return codingProblemJudgeService.executeCandidate(judgeRequest);
    }

    @Override
    public CodingProblemExecutionResponse submitSolution(String idOrSlug, SubmitCodingProblemRequest request) {
        if (idOrSlug == null || idOrSlug.isBlank()) {
            throw new BadRequestException("Problem identifier cannot be blank.");
        }
        if (request == null) {
            throw new BadRequestException("Request body cannot be null.");
        }
        if (request.getLanguageId() == null || request.getLanguageId().isBlank()) {
            throw new BadRequestException("Language ID is required.");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("Code cannot be empty.");
        }

        CodingProblem problem = findByIdOrSlug(idOrSlug);
        ResolvedExecutionSetup setup = resolveCandidateExecutionSetup(problem, request.getLanguageId());

        List<CandidateTestcaseItem> testcases = new ArrayList<>();
        int caseIdx = 1;
        if (problem.getSampleTestCases() != null) {
            for (TestCase tc : problem.getSampleTestCases()) {
                testcases.add(new CandidateTestcaseItem(
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
        if (problem.getHiddenTestCases() != null) {
            for (TestCase tc : problem.getHiddenTestCases()) {
                testcases.add(new CandidateTestcaseItem(
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

        if (testcases.isEmpty()) {
            throw new BadRequestException("Coding problem has no testcases configured.");
        }

        JudgeCandidateExecutionRequest judgeRequest = new JudgeCandidateExecutionRequest(
                problem.getProblemType(),
                setup.candLanguage(),
                request.getCode(),
                setup.candConstraints(),
                setup.refLanguage(),
                problem.getReferenceSolution(),
                setup.refConstraints(),
                problem.isRequireVerificationScript(),
                setup.verLanguage(),
                problem.getVerificationScript(),
                setup.verConstraints(),
                testcases
        );

        return codingProblemJudgeService.executeCandidate(judgeRequest);
    }

    private record ResolvedExecutionSetup(
            Language candLanguage,
            CodeExecutionConstraints candConstraints,
            Language refLanguage,
            CodeExecutionConstraints refConstraints,
            Language verLanguage,
            CodeExecutionConstraints verConstraints
    ) {}

    private ResolvedExecutionSetup resolveCandidateExecutionSetup(CodingProblem problem, String candidateLanguageId) {
        if (!problem.getSupportedLanguageIds().contains(candidateLanguageId)) {
            throw new BadRequestException("Language '" + candidateLanguageId + "' is not supported by this problem.");
        }
        if (!languageRegistry.isSupported(candidateLanguageId)) {
            throw new BadRequestException("Language '" + candidateLanguageId + "' is not registered on platform.");
        }

        Language candLanguage = languageRegistry.get(candidateLanguageId);
        if (problem.getProblemType() == CodingProblemType.DATABASE && candLanguage.type() != LanguageType.DATABASE) {
            throw new BadRequestException("Language '" + candidateLanguageId + "' is not a database language.");
        }
        if (problem.getProblemType() == CodingProblemType.GENERIC && candLanguage.type() == LanguageType.DATABASE) {
            throw new BadRequestException("Database language '" + candidateLanguageId + "' cannot be used for generic problems.");
        }

        // Candidate constraints
        CodeExecutionConstraints candUserConstraints = problem.getExecutionConstraints() != null
                ? problem.getExecutionConstraints().get(candidateLanguageId)
                : null;
        long candTimeLimit = executionConstraintResolver.resolveTimeLimitMs(candLanguage, candUserConstraints, null);
        long candMemLimit = executionConstraintResolver.resolveMemoryLimitKb(candLanguage, candUserConstraints, null);
        CodeExecutionConstraints candConstraints = new CodeExecutionConstraints(candTimeLimit, candMemLimit);

        // Reference language & constraints
        if (!languageRegistry.isSupported(problem.getReferenceSolutionLanguage())) {
            throw new BadRequestException("Reference solution language '" + problem.getReferenceSolutionLanguage() + "' is not registered.");
        }
        Language refLanguage = languageRegistry.get(problem.getReferenceSolutionLanguage());
        CodeExecutionConstraints refUserConstraints = problem.getExecutionConstraints() != null
                ? problem.getExecutionConstraints().get(problem.getReferenceSolutionLanguage())
                : null;
        long refTimeLimit = executionConstraintResolver.resolveTimeLimitMs(refLanguage, refUserConstraints, null);
        long refMemLimit = executionConstraintResolver.resolveMemoryLimitKb(refLanguage, refUserConstraints, null);
        CodeExecutionConstraints refConstraints = new CodeExecutionConstraints(refTimeLimit, refMemLimit);

        // Verification language & constraints
        Language verLanguage = null;
        CodeExecutionConstraints verConstraints = null;
        if (problem.isRequireVerificationScript()) {
            if (problem.getVerificationScriptLanguage() == null || problem.getVerificationScriptLanguage().isBlank()) {
                throw new BadRequestException("Problem requires verification script but script language is missing.");
            }
            if (!languageRegistry.isSupported(problem.getVerificationScriptLanguage())) {
                throw new BadRequestException("Verification script language '" + problem.getVerificationScriptLanguage() + "' is not registered.");
            }
            verLanguage = languageRegistry.get(problem.getVerificationScriptLanguage());
            CodeExecutionConstraints verUserConstraints = problem.getExecutionConstraints() != null
                    ? problem.getExecutionConstraints().get(problem.getVerificationScriptLanguage())
                    : null;
            long verTimeLimit = executionConstraintResolver.resolveTimeLimitMs(verLanguage, verUserConstraints, null);
            long verMemLimit = executionConstraintResolver.resolveMemoryLimitKb(verLanguage, verUserConstraints, null);
            verConstraints = new CodeExecutionConstraints(verTimeLimit, verMemLimit);
        }

        return new ResolvedExecutionSetup(candLanguage, candConstraints, refLanguage, refConstraints, verLanguage, verConstraints);
    }

    @Override
    public TestcaseGeneratorRunResponse runGenerator(RunTestcaseGeneratorRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body cannot be null.");
        }
        if (request.getLanguage() == null || request.getLanguage().isBlank()) {
            throw new BadRequestException("Generator language is required.");
        }
        if (!languageRegistry.isSupported(request.getLanguage())) {
            throw new BadRequestException("Language '" + request.getLanguage() + "' is not supported by the platform.");
        }
        Language lang = languageRegistry.get(request.getLanguage());
        if (lang.type() == LanguageType.DATABASE) {
            throw new BadRequestException("Database languages cannot be used for testcase generation.");
        }
        if (request.getScript() == null || request.getScript().isBlank()) {
            throw new BadRequestException("Generator script cannot be empty.");
        }

        ExecutionConstraintRequest userReq = request.getConstraints();
        CodeExecutionConstraints userConstraints = userReq != null ? userReq.toCodeExecutionConstraints() : null;
        long timeLimitMs = executionConstraintResolver.resolveTimeLimitMs(lang, userConstraints, null);
        long memoryLimitKb = executionConstraintResolver.resolveMemoryLimitKb(lang, userConstraints, null);
        CodeExecutionConstraints constraints = new CodeExecutionConstraints(timeLimitMs, memoryLimitKb);

        return codingProblemJudgeService.executeGenerator(
                lang,
                request.getScript(),
                request.getStdin(),
                constraints
        );
    }

    @Override
    public boolean isSlugAvailable(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        return !codingProblemRepository.existsBySlug(slug.trim());
    }

    @Override
    public boolean isSlugAvailable(String slug, String excludeId) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        if (excludeId == null || excludeId.isBlank()) {
            return isSlugAvailable(slug);
        }
        String trimmedSlug = slug.trim();
        return codingProblemRepository.findBySlug(trimmedSlug)
                .map(existing -> existing.getId().equals(excludeId) || existing.getSlug().equals(excludeId))
                .orElse(true);
    }

    @Transactional
    @Override
    public CodingProblem updateStatus(String idOrSlug, CodingProblemStatus status) {
        if (idOrSlug == null || idOrSlug.isBlank()) {
            throw new BadRequestException("Coding problem identifier cannot be empty.");
        }
        if (status == null) {
            throw new BadRequestException("Status cannot be null.");
        }

        CodingProblem codingProblem = codingProblemRepository.findById(idOrSlug)
                .or(() -> codingProblemRepository.findBySlug(idOrSlug))
                .orElseThrow(() -> new ResourceNotFoundException("CodingProblem", "id or slug", idOrSlug));

        CodingProblemStatus currentStatus = codingProblem.getStatus();
        if (currentStatus == status) {
            return codingProblem;
        }

        if ((status == CodingProblemStatus.DRAFT || status == CodingProblemStatus.UNPUBLISHED)
                && currentStatus == CodingProblemStatus.PUBLISHED) {
            if (isInUse(codingProblem.getId())) {
                List<QuestionBank> banks = questionBankRepository.findAllByItemsQuestionId(codingProblem.getId());
                List<Assessment> assessments = assessmentRepository.findAllByItemsQuestionId(codingProblem.getId());
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
                throw new ConflictException("Cannot move coding problem '" + codingProblem.getTitle() + "' back to draft/unpublished because it is currently in use by: " + String.join("; ", usages) + ".");
            }
        }

        if (status == CodingProblemStatus.PUBLISHED) {
            if (codingProblem.getTitle() == null || codingProblem.getTitle().isBlank()) {
                throw new BadRequestException("Cannot publish coding problem without a title.");
            }
            if (codingProblem.getStatement() == null || codingProblem.getStatement().isBlank()) {
                throw new BadRequestException("Cannot publish coding problem without a problem statement.");
            }
            boolean hasSample = codingProblem.getSampleTestCases() != null && !codingProblem.getSampleTestCases().isEmpty();
            boolean hasHidden = codingProblem.getHiddenTestCases() != null && !codingProblem.getHiddenTestCases().isEmpty();
            if (!hasSample && !hasHidden) {
                throw new BadRequestException("Cannot publish coding problem without at least one testcase.");
            }
        }

        codingProblem.setStatus(status);
        codingProblem.setUpdatedAt(Instant.now());
        return codingProblemRepository.save(codingProblem);
    }

    @Override
    public boolean isInUse(String idOrSlug) {
        if (!org.springframework.util.StringUtils.hasText(idOrSlug)) {
            return false;
        }
        CodingProblem problem = codingProblemRepository.findById(idOrSlug)
                .or(() -> codingProblemRepository.findBySlug(idOrSlug))
                .orElse(null);
        if (problem == null) {
            return false;
        }
        String problemId = problem.getId();
        boolean inBanks = questionBankRepository.existsByItemsQuestionId(problemId);
        boolean inAssessments = assessmentRepository.existsByItemsQuestionId(problemId);
        boolean inResponses = assessmentQuestionResponseRepository != null
                && assessmentQuestionResponseRepository.existsByQuestionId(problemId);
        return inBanks || inAssessments || inResponses;
    }

    @Transactional
    @Override
    public CodingProblem publish(String idOrSlug) {
        return updateStatus(idOrSlug, CodingProblemStatus.PUBLISHED);
    }

    @Transactional
    @Override
    public CodingProblem unpublish(String idOrSlug) {
        return updateStatus(idOrSlug, CodingProblemStatus.UNPUBLISHED);
    }

    @Override
    public List<SupportedLanguageResponse> getSupportedLanguages(CodingProblemType problemType) {
        List<Language> languages = languageRegistry.getAll();

        return languages.stream()
                .filter(lang -> {
                    if (problemType == null) {
                        return true;
                    }
                    if (problemType == CodingProblemType.DATABASE) {
                        return lang.type() == LanguageType.DATABASE;
                    } else if (problemType == CodingProblemType.GENERIC) {
                        return lang.type() != LanguageType.DATABASE;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(Language::name, String.CASE_INSENSITIVE_ORDER))
                .map(lang -> new SupportedLanguageResponse(
                        lang.id(),
                        lang.name(),
                        lang.type(),
                        lang.fileExtension(),
                        languageRegistry.resolveReferenceSolutionFileName(lang),
                        executionConstraintResolver.resolveTimeLimitMs(lang, null, null),
                        executionConstraintResolver.resolveMemoryLimitKb(lang, null, null)
                ))
                .toList();
    }
}
