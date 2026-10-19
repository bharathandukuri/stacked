package com.bharath.stacked.modules.coding_problem.controller;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.UpdateCodingProblemStatusRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.SlugAvailabilityResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.SupportedLanguageResponse;
import com.bharath.stacked.modules.coding_problem.enums.CodingProblemType;
import com.bharath.stacked.modules.coding_problem.model.CodingProblem;
import com.bharath.stacked.modules.coding_problem.service.CodingProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coding-problem")
@RequiredArgsConstructor
public class CodingProblemController {

    private final CodingProblemService codingProblemService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<CodingProblem>> createCodingProblem(
            @Valid @RequestBody CreateCodingProblemRequest createCodingProblemRequest) {
        CodingProblem codingProblem = codingProblemService.create(createCodingProblemRequest);
        return ApiResponse.created(codingProblem, "Coding Problem Created");
    }

    @GetMapping("/check-slug")
    public ResponseEntity<ApiResponse<SlugAvailabilityResponse>> checkSlugAvailability(
            @RequestParam("slug") String slug) {
        boolean available = codingProblemService.isSlugAvailable(slug);
        return ApiResponse.success(new SlugAvailabilityResponse(slug, available));
    }

    @GetMapping({"/supported-languages", "/languages"})
    public ResponseEntity<ApiResponse<List<SupportedLanguageResponse>>> getSupportedLanguages(
            @RequestParam(value = "problemType", required = false) CodingProblemType problemType) {
        List<SupportedLanguageResponse> languages = codingProblemService.getSupportedLanguages(problemType);
        return ApiResponse.success(languages, "Supported languages retrieved successfully");
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<CodingProblem>> publishProblem(
            @PathVariable("id") String id) {
        CodingProblem codingProblem = codingProblemService.publish(id);
        return ApiResponse.success(codingProblem, "Coding problem published successfully");
    }

    @PatchMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<CodingProblem>> unpublishProblem(
            @PathVariable("id") String id) {
        CodingProblem codingProblem = codingProblemService.unpublish(id);
        return ApiResponse.success(codingProblem, "Coding problem unpublished successfully");
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CodingProblem>> updateProblemStatus(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateCodingProblemStatusRequest request) {
        CodingProblem codingProblem = codingProblemService.updateStatus(id, request.getStatus());
        return ApiResponse.success(codingProblem, "Coding problem status updated successfully");
    }
}
