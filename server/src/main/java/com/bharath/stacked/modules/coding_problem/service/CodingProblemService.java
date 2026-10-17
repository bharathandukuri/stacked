package com.bharath.stacked.modules.coding_problem.service;

import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;

public interface CodingProblemService {

    CodingProblem create(CreateCodingProblemRequest request);
}
