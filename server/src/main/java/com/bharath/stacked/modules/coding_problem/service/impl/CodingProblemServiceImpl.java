package com.bharath.stacked.modules.coding_problem.service.impl;

import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.ConflictException;
import com.bharath.stacked.exception.ResourceNotFoundException;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.ExecutionConstraintRequest;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.InitializedTestcases;
import com.bharath.stacked.modules.coding_problem.judge.CodingProblemJudgeService.JudgeInitializationRequest;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.repository.CodingProblemRepository;
import com.bharath.stacked.modules.coding_problem.service.CodingProblemService;
import com.bharath.stacked.modules.execution.dto.CodeExecutionConstraints;
import com.bharath.stacked.modules.execution.resolver.ExecutionConstraintResolver;
import com.bharath.stacked.modules.language.Language;
import com.bharath.stacked.modules.language.LanguageType;
import com.bharath.stacked.modules.coding_problem.dto.response.SupportedLanguageResponse;
import com.bharath.stacked.modules.language.registry.LanguageRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodingProblemServiceImpl implements CodingProblemService {

    private final CodingProblemRepository codingProblemRepository;
    private final LanguageRegistry languageRegistry;
    private final ExecutionConstraintResolver executionConstraintResolver;
    private final CodingProblemJudgeService codingProblemJudgeService;

    @Transactional
    @Override
    public CodingProblem create(CreateCodingProblemRequest request) {
        // 1. Validate uniqueness of slug
        if (codingProblemRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new ConflictException("CodingProblem", "slug", request.getSlug());
        }

        // 2. Validate supported languages and problem type compatibility
        if (request.getProblemType() == CodingProblemType.DATABASE && request.getSupportedLanguageIds().size() != 1) {
            throw new BadRequestException("A DATABASE problem must support exactly one database language.");
        }

        for (String langId : request.getSupportedLanguageIds()) {
            if (!languageRegistry.isSupported(langId)) {
                throw new BadRequestException("Language '" + langId + "' is not supported by the platform.");
            }
            Language lang = languageRegistry.get(langId);
            if (request.getProblemType() == CodingProblemType.DATABASE && lang.type() != LanguageType.DATABASE) {
                throw new BadRequestException("Language '" + langId + "' is not a database language for a DATABASE problem.");
            }
            if (request.getProblemType() == CodingProblemType.GENERIC && lang.type() == LanguageType.DATABASE) {
                throw new BadRequestException("Database language '" + langId + "' cannot be used for a GENERIC problem.");
            }
        }

        // 3. Validate reference solution language and source code
        if (request.getReferenceSolutionLanguage() == null || request.getReferenceSolutionLanguage().isBlank()) {
            throw new BadRequestException("Reference solution language is required.");
        }
        if (request.getReferenceSolution() == null || request.getReferenceSolution().isBlank()) {
            throw new BadRequestException("Reference solution cannot be empty.");
        }
        if (!request.getSupportedLanguageIds().contains(request.getReferenceSolutionLanguage())) {
            throw new BadRequestException("Reference solution language '" + request.getReferenceSolutionLanguage()
                    + "' must be included in supportedLanguageIds.");
        }
        Language refLanguage = languageRegistry.get(request.getReferenceSolutionLanguage());

        // 4. Validate starter code mapping
        if (request.getStarterCode() != null) {
            for (String langId : request.getStarterCode().keySet()) {
                if (!languageRegistry.isSupported(langId) || !request.getSupportedLanguageIds().contains(langId)) {
                    throw new BadRequestException("Starter code provided for unsupported language: '" + langId + "'.");
                }
            }
        }

        // 5. Validate verification script if required
        Language verLanguage = null;
        if (request.isRequireVerificationScript()) {
            if (request.getVerificationScriptLanguage() == null || request.getVerificationScriptLanguage().isBlank()) {
                throw new BadRequestException("Verification script language is required.");
            }
            if (request.getVerificationScript() == null || request.getVerificationScript().isBlank()) {
                throw new BadRequestException("Verification script is required.");
            }
            if (!languageRegistry.isSupported(request.getVerificationScriptLanguage())) {
                throw new BadRequestException("Verification script language '" + request.getVerificationScriptLanguage()
                        + "' is not supported.");
            }
            verLanguage = languageRegistry.get(request.getVerificationScriptLanguage());
            if (request.getProblemType() == CodingProblemType.GENERIC && verLanguage.type() == LanguageType.DATABASE) {
                throw new BadRequestException("Database language '" + request.getVerificationScriptLanguage()
                        + "' cannot be used as verification script for a GENERIC problem.");
            }
            if (request.getProblemType() == CodingProblemType.DATABASE) {
                if (verLanguage.type() != LanguageType.DATABASE) {
                    throw new BadRequestException("Verification script for a DATABASE problem must be a database language.");
                }
                if (!request.getVerificationScriptLanguage().equalsIgnoreCase(request.getReferenceSolutionLanguage())) {
                    throw new BadRequestException("Verification script language '" + request.getVerificationScriptLanguage()
                            + "' must match reference solution language '" + request.getReferenceSolutionLanguage()
                            + "' for a DATABASE problem.");
                }
            }
        }

        // 6. Validate and resolve per-language execution constraints
        Map<String, CodeExecutionConstraints> resolvedConstraints = new HashMap<>();

        if (request.getExecutionConstraints() != null) {
            for (Map.Entry<String, ExecutionConstraintRequest> entry : request.getExecutionConstraints().entrySet()) {
                String langId = entry.getKey();
                if (!languageRegistry.isSupported(langId)) {
                    throw new BadRequestException("Execution constraints provided for unsupported language: '" + langId + "'.");
                }
                boolean inSupported = request.getSupportedLanguageIds().contains(langId);
                boolean isVerLang = request.isRequireVerificationScript() && langId.equals(request.getVerificationScriptLanguage());
                if (!inSupported && !isVerLang) {
                    throw new BadRequestException("Execution constraints provided for language '" + langId + "' not in supportedLanguageIds.");
                }
            }
        }

        for (String langId : request.getSupportedLanguageIds()) {
            Language lang = languageRegistry.get(langId);
            ExecutionConstraintRequest userReq = request.getExecutionConstraints() != null
                    ? request.getExecutionConstraints().get(langId)
                    : null;
            CodeExecutionConstraints userConstraints = userReq != null ? userReq.toCodeExecutionConstraints() : null;

            long timeLimitMs = executionConstraintResolver.resolveTimeLimitMs(lang, userConstraints, null);
            long memoryLimitKb = executionConstraintResolver.resolveMemoryLimitKb(lang, userConstraints, null);
            resolvedConstraints.put(langId, new CodeExecutionConstraints(timeLimitMs, memoryLimitKb));
        }

        if (request.isRequireVerificationScript() && !resolvedConstraints.containsKey(request.getVerificationScriptLanguage())) {
            Language lang = languageRegistry.get(request.getVerificationScriptLanguage());
            ExecutionConstraintRequest userReq = request.getExecutionConstraints() != null
                    ? request.getExecutionConstraints().get(request.getVerificationScriptLanguage())
                    : null;
            CodeExecutionConstraints userConstraints = userReq != null ? userReq.toCodeExecutionConstraints() : null;

            long timeLimitMs = executionConstraintResolver.resolveTimeLimitMs(lang, userConstraints, null);
            long memoryLimitKb = executionConstraintResolver.resolveMemoryLimitKb(lang, userConstraints, null);
            resolvedConstraints.put(request.getVerificationScriptLanguage(), new CodeExecutionConstraints(timeLimitMs, memoryLimitKb));
        }

        CodeExecutionConstraints refConstraints = resolvedConstraints.get(request.getReferenceSolutionLanguage());
        CodeExecutionConstraints verConstraints = request.isRequireVerificationScript()
                ? resolvedConstraints.get(request.getVerificationScriptLanguage())
                : null;

        // 7. Initialize expected outputs via dedicated judge service
        JudgeInitializationRequest judgeRequest = new JudgeInitializationRequest(
                request.getProblemType(),
                refLanguage,
                request.getReferenceSolution(),
                refConstraints,
                request.isRequireVerificationScript(),
                verLanguage,
                request.getVerificationScript(),
                verConstraints,
                request.getSampleTestcases(),
                request.getHiddenTestcases()
        );

        InitializedTestcases initializedTestcases = codingProblemJudgeService.initializeExpectedOutputs(judgeRequest);

        // 8. Build entity and persist
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
                .sampleTestCases(initializedTestcases.sampleTestCases())
                .hiddenTestCases(initializedTestcases.hiddenTestCases())
                .executionConstraints(resolvedConstraints)
                .build();

        return codingProblemRepository.save(codingProblem);
    }

    @Override
    public boolean isSlugAvailable(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        return !codingProblemRepository.existsBySlug(slug.trim());
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

        codingProblem.setStatus(status);
        return codingProblemRepository.save(codingProblem);
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
