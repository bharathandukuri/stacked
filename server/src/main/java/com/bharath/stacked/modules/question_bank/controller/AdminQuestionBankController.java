package com.bharath.stacked.modules.question_bank.controller;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.question_bank.dto.*;
import com.bharath.stacked.modules.question_bank.service.QuestionBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/question-banks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestionBankController {

    private final QuestionBankService questionBankService;

    @GetMapping
    @NonNull
    public ResponseEntity<ApiResponse<PageResponse<QuestionBankSummaryResponse>>> listQuestionBanks(
            @ModelAttribute QuestionBankFilterRequest filter) {
        PageResponse<QuestionBankSummaryResponse> response = questionBankService.getQuestionBanks(filter);
        return ApiResponse.success(response, "Question banks retrieved successfully");
    }

    @GetMapping("/{id}")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankDetailResponse>> getQuestionBank(
            @PathVariable @NonNull String id) {
        QuestionBankDetailResponse response = questionBankService.getQuestionBank(id);
        return ApiResponse.success(response, "Question bank details retrieved successfully");
    }

    @PostMapping
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankSummaryResponse>> createQuestionBank(
            @Valid @RequestBody @NonNull CreateQuestionBankRequest request,
            @Nullable Authentication authentication) {
        String authorId = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : null;
        QuestionBankSummaryResponse response = questionBankService.createQuestionBank(request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Question bank created successfully").getBody());
    }

    @PutMapping("/{id}")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankSummaryResponse>> updateQuestionBank(
            @PathVariable @NonNull String id,
            @Valid @RequestBody @NonNull UpdateQuestionBankRequest request) {
        QuestionBankSummaryResponse response = questionBankService.updateQuestionBank(id, request);
        return ApiResponse.success(response, "Question bank updated successfully");
    }

    @DeleteMapping("/{id}")
    @NonNull
    public ResponseEntity<ApiResponse<Void>> deleteQuestionBank(
            @PathVariable @NonNull String id) {
        questionBankService.deleteQuestionBank(id);
        return ApiResponse.success(null, "Question bank deleted successfully");
    }

    @GetMapping("/{id}/in-use")
    @NonNull
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> checkInUse(
            @PathVariable @NonNull String id) {
        boolean inUse = questionBankService.isInUse(id);
        return ApiResponse.success(java.util.Map.of("inUse", inUse), "Usage status retrieved successfully");
    }

    @PatchMapping("/{id}/status")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankSummaryResponse>> updateQuestionBankStatus(
            @PathVariable @NonNull String id,
            @Valid @RequestBody @NonNull UpdateQuestionBankStatusRequest request) {
        QuestionBankSummaryResponse response = questionBankService.updateStatus(id, request.getStatus());
        return ApiResponse.success(response, "Question bank status updated successfully");
    }

    @PatchMapping("/{id}/publish")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankSummaryResponse>> publishQuestionBank(
            @PathVariable @NonNull String id) {
        QuestionBankSummaryResponse response = questionBankService.publish(id);
        return ApiResponse.success(response, "Question bank published successfully");
    }

    @PatchMapping("/{id}/unpublish")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankSummaryResponse>> unpublishQuestionBank(
            @PathVariable @NonNull String id) {
        QuestionBankSummaryResponse response = questionBankService.unpublish(id);
        return ApiResponse.success(response, "Question bank unpublished successfully");
    }

    @PostMapping("/{id}/duplicate")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankSummaryResponse>> duplicateQuestionBank(
            @PathVariable @NonNull String id,
            @Nullable Authentication authentication) {
        String authorId = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : null;
        QuestionBankSummaryResponse response = questionBankService.duplicateQuestionBank(id, authorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Question bank duplicated successfully").getBody());
    }

    @GetMapping("/categories")
    @NonNull
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = questionBankService.getDistinctCategories();
        return ApiResponse.success(categories, "Question bank categories retrieved successfully");
    }

    @GetMapping("/stats")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankStatsResponse>> getStats() {
        QuestionBankStatsResponse stats = questionBankService.getStats();
        return ApiResponse.success(stats, "Question bank statistics retrieved successfully");
    }
}
