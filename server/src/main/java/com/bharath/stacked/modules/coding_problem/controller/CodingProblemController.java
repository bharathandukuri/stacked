package com.bharath.stacked.modules.coding_problem.controller;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.service.CodingProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coding-problem")
@RequiredArgsConstructor
public class CodingProblemController {

    private final CodingProblemService codingProblemService;

    @PostMapping
    public ResponseEntity<?> createCodingProblem(@Valid @RequestBody CreateCodingProblemRequest createCodingProblemRequest) {
        CodingProblem codingProblem = codingProblemService.create(createCodingProblemRequest);
        return ApiResponse.created(codingProblem, "Coding Problem Created");
    }
}
