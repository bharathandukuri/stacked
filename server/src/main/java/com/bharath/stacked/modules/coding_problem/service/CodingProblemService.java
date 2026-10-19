package com.bharath.stacked.modules.coding_problem.service;

import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.SupportedLanguageResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemStatus;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;

import java.util.List;

public interface CodingProblemService {

    CodingProblem create(CreateCodingProblemRequest request);

    boolean isSlugAvailable(String slug);

    CodingProblem updateStatus(String idOrSlug, CodingProblemStatus status);

    CodingProblem publish(String idOrSlug);

    CodingProblem unpublish(String idOrSlug);

    List<SupportedLanguageResponse> getSupportedLanguages(CodingProblemType problemType);

    default List<SupportedLanguageResponse> getSupportedLanguages() {
        return getSupportedLanguages(null);
    }
}
