package com.bharath.stacked.modules.coding_problem.service;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.coding_problem.dto.request.CodingProblemFilterRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.RunCodingProblemPreviewRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.RunSampleTestcasesRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.SubmitCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.UpdateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemSummaryResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.PreviewRunResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.SupportedLanguageResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;

import com.bharath.stacked.modules.coding_problem.dto.request.RunTestcaseGeneratorRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.TestcaseGeneratorRunResponse;

import java.util.List;

public interface CodingProblemService {

    PageResponse<CodingProblemSummaryResponse> findAll(CodingProblemFilterRequest filter);

    CodingProblem findByIdOrSlug(String idOrSlug);

    default CodingProblem getProblemDetails(String idOrSlug) {
        return findByIdOrSlug(idOrSlug);
    }

    void delete(String idOrSlug);

    CodingProblem create(CreateCodingProblemRequest request);

    CodingProblem update(String idOrSlug, UpdateCodingProblemRequest request);

    PreviewRunResponse runPreview(RunCodingProblemPreviewRequest request);

    CodingProblemExecutionResponse runSampleTestcases(String idOrSlug, RunSampleTestcasesRequest request);

    CodingProblemExecutionResponse submitSolution(String idOrSlug, SubmitCodingProblemRequest request);

    TestcaseGeneratorRunResponse runGenerator(RunTestcaseGeneratorRequest request);

    boolean isSlugAvailable(String slug, String excludeId);

    default boolean isSlugAvailable(String slug) {
        return isSlugAvailable(slug, null);
    }

    CodingProblem updateStatus(String idOrSlug, CodingProblemStatus status);

    CodingProblem publish(String idOrSlug);

    CodingProblem unpublish(String idOrSlug);

    List<SupportedLanguageResponse> getSupportedLanguages(CodingProblemType problemType);

    default List<SupportedLanguageResponse> getSupportedLanguages() {
        return getSupportedLanguages(null);
    }
}
