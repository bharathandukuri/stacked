package com.bharath.stacked.modules.coding_problem.controller;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.coding_problem.dto.request.CodingProblemFilterRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.CreateCodingProblemRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.RunCodingProblemPreviewRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.RunTestcaseGeneratorRequest;
import com.bharath.stacked.modules.coding_problem.dto.request.UpdateCodingProblemStatusRequest;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemSummaryResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.PreviewRunResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.SlugAvailabilityResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.SupportedLanguageResponse;
import com.bharath.stacked.modules.coding_problem.dto.response.TestcaseGeneratorRunResponse;
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
public class CodingProblemController {

    private final CodingProblemService codingProblemService;

    public CodingProblemController(CodingProblemService codingProblemService) {
        this.codingProblemService = codingProblemService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CodingProblemSummaryResponse>>> getCodingProblems(
            @ModelAttribute CodingProblemFilterRequest filter) {
        PageResponse<CodingProblemSummaryResponse> response = codingProblemService.findAll(filter);
        return ApiResponse.success(response, "Coding problems retrieved successfully");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CodingProblem>> createCodingProblem(
            @Valid @RequestBody CreateCodingProblemRequest createCodingProblemRequest) {
        CodingProblem codingProblem = codingProblemService.create(createCodingProblemRequest);
        return ApiResponse.created(codingProblem, "Coding Problem Created");
    }

    @PostMapping({"/run-preview", "/test-run"})
    public ResponseEntity<ApiResponse<PreviewRunResponse>> runPreview(
            @Valid @RequestBody RunCodingProblemPreviewRequest request) {
        PreviewRunResponse response = codingProblemService.runPreview(request);
        return ApiResponse.success(response, "Preview testcases executed successfully");
    }

    @PostMapping({"/run-generator", "/generate-testcases"})
    public ResponseEntity<ApiResponse<TestcaseGeneratorRunResponse>> runGenerator(
            @Valid @RequestBody RunTestcaseGeneratorRequest request) {
        TestcaseGeneratorRunResponse response = codingProblemService.runGenerator(request);
        return ApiResponse.success(response, "Generator script executed successfully");
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<ApiResponse<CodingProblem>> getCodingProblem(
            @PathVariable("idOrSlug") String idOrSlug) {
        CodingProblem codingProblem = codingProblemService.findByIdOrSlug(idOrSlug);
        return ApiResponse.success(codingProblem, "Coding problem retrieved successfully");
    }

    @PutMapping("/{idOrSlug}")
    public ResponseEntity<ApiResponse<CodingProblem>> updateCodingProblem(
            @PathVariable("idOrSlug") String idOrSlug,
            @Valid @RequestBody com.bharath.stacked.modules.coding_problem.dto.request.UpdateCodingProblemRequest request) {
        CodingProblem codingProblem = codingProblemService.update(idOrSlug, request);
        return ApiResponse.success(codingProblem, "Coding problem updated successfully");
    }

    @DeleteMapping("/{idOrSlug}")
    public ResponseEntity<ApiResponse<Void>> deleteCodingProblem(
            @PathVariable("idOrSlug") String idOrSlug) {
        codingProblemService.delete(idOrSlug);
        return ApiResponse.success(null, "Coding problem deleted successfully");
    }

    @GetMapping("/check-slug")
    public ResponseEntity<ApiResponse<SlugAvailabilityResponse>> checkSlugAvailability(
            @RequestParam("slug") String slug,
            @RequestParam(value = "excludeId", required = false) String excludeId) {
        boolean available = (excludeId != null && !excludeId.isBlank())
                ? codingProblemService.isSlugAvailable(slug, excludeId)
                : codingProblemService.isSlugAvailable(slug);
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

    @PatchMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<CodingProblem>> archiveProblem(
            @PathVariable("id") String id) {
        CodingProblem codingProblem = codingProblemService.archive(id);
        return ApiResponse.success(codingProblem, "Coding problem archived successfully");
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CodingProblem>> updateProblemStatus(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateCodingProblemStatusRequest request) {
        CodingProblem codingProblem = codingProblemService.updateStatus(id, request.getStatus());
        return ApiResponse.success(codingProblem, "Coding problem status updated successfully");
    }
}
