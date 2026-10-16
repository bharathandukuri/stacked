package com.bharath.stacked.modules.coding.service;

import com.bharath.stacked.modules.coding.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding.model.CodingProblem;

public interface CodingProblemService {

    CodingProblem create(CreateCodingProblemRequest request);
}
