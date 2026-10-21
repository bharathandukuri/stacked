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

    @GetMapping("/{idOrSlug}")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankDetailResponse>> getQuestionBank(
            @PathVariable @NonNull String idOrSlug) {
        QuestionBankDetailResponse response = questionBankService.getQuestionBank(idOrSlug);
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

    @PutMapping("/{idOrSlug}")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankSummaryResponse>> updateQuestionBank(
            @PathVariable @NonNull String idOrSlug,
            @Valid @RequestBody @NonNull UpdateQuestionBankRequest request) {
        QuestionBankSummaryResponse response = questionBankService.updateQuestionBank(idOrSlug, request);
        return ApiResponse.success(response, "Question bank updated successfully");
    }

    @DeleteMapping("/{idOrSlug}")
    @NonNull
    public ResponseEntity<ApiResponse<Void>> deleteQuestionBank(
            @PathVariable @NonNull String idOrSlug) {
        questionBankService.deleteQuestionBank(idOrSlug);
        return ApiResponse.success(null, "Question bank deleted successfully");
    }

    @PatchMapping("/{idOrSlug}/status")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankSummaryResponse>> updateQuestionBankStatus(
            @PathVariable @NonNull String idOrSlug,
            @Valid @RequestBody @NonNull UpdateQuestionBankStatusRequest request) {
        QuestionBankSummaryResponse response = questionBankService.updateStatus(idOrSlug, request.getStatus());
        return ApiResponse.success(response, "Question bank status updated successfully");
    }

    @PatchMapping("/{idOrSlug}/publish")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankSummaryResponse>> publishQuestionBank(
            @PathVariable @NonNull String idOrSlug) {
        QuestionBankSummaryResponse response = questionBankService.publish(idOrSlug);
        return ApiResponse.success(response, "Question bank published successfully");
    }

    @PatchMapping("/{idOrSlug}/unpublish")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankSummaryResponse>> unpublishQuestionBank(
            @PathVariable @NonNull String idOrSlug) {
        QuestionBankSummaryResponse response = questionBankService.unpublish(idOrSlug);
        return ApiResponse.success(response, "Question bank unpublished successfully");
    }

    @PatchMapping("/{idOrSlug}/archive")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankSummaryResponse>> archiveQuestionBank(
            @PathVariable @NonNull String idOrSlug) {
        QuestionBankSummaryResponse response = questionBankService.archive(idOrSlug);
        return ApiResponse.success(response, "Question bank archived successfully");
    }

    @PostMapping("/{idOrSlug}/duplicate")
    @NonNull
    public ResponseEntity<ApiResponse<QuestionBankSummaryResponse>> duplicateQuestionBank(
            @PathVariable @NonNull String idOrSlug,
            @Nullable Authentication authentication) {
        String authorId = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : null;
        QuestionBankSummaryResponse response = questionBankService.duplicateQuestionBank(idOrSlug, authorId);
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
