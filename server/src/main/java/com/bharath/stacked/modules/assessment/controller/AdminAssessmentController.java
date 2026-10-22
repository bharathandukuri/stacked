package com.bharath.stacked.modules.assessment.controller;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.assessment.dto.*;
import com.bharath.stacked.modules.assessment.model.AssessmentAuditEvent;
import com.bharath.stacked.modules.assessment.model.AssessmentConnectionEvent;
import com.bharath.stacked.modules.assessment.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/assessments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<AssessmentSummaryResponse>> createAssessment(
            @Valid @RequestBody CreateAssessmentRequest request,
            @AuthenticationPrincipal String adminId) {
        AssessmentSummaryResponse response = assessmentService.createAssessment(request, adminId);
        return ApiResponse.created(response, "Assessment created successfully");
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<AssessmentValidationResponse>> validateAssessment(
            @RequestBody CreateAssessmentRequest request) {
        AssessmentValidationResponse response = assessmentService.validateAssessmentConfig(request);
        return ApiResponse.success(response, "Assessment pre-creation validation completed");
    }

    @GetMapping("/question-bank-preview/{bankId}")
    public ResponseEntity<ApiResponse<QuestionBankPreviewDto>> getQuestionBankPreview(
            @PathVariable String bankId) {
        QuestionBankPreviewDto response = assessmentService.getQuestionBankPreview(bankId);
        return ApiResponse.success(response, "Question bank preview retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AssessmentSummaryResponse>>> getAssessments(
            @ModelAttribute AssessmentFilterRequest filter) {
        PageResponse<AssessmentSummaryResponse> response = assessmentService.getAssessments(filter);
        return ApiResponse.success(response, "Assessments retrieved successfully");
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<ApiResponse<AssessmentDetailResponse>> getAssessment(
            @PathVariable String idOrSlug) {
        AssessmentDetailResponse response = assessmentService.getAssessment(idOrSlug);
        return ApiResponse.success(response, "Assessment details retrieved successfully");
    }

    @DeleteMapping("/{idOrSlug}")
    public ResponseEntity<ApiResponse<Void>> deleteAssessment(
            @PathVariable String idOrSlug) {
        assessmentService.deleteAssessment(idOrSlug);
        return ApiResponse.success(null, "Assessment deleted successfully");
    }

    @PostMapping("/{idOrSlug}/complete")
    public ResponseEntity<ApiResponse<AssessmentDetailResponse>> completeAssessment(
            @PathVariable String idOrSlug) {
        AssessmentDetailResponse response = assessmentService.completeAssessment(idOrSlug);
        return ApiResponse.success(response, "Assessment marked as completed successfully");
    }

    @PostMapping("/{idOrSlug}/candidates")
    public ResponseEntity<ApiResponse<BulkAssignCandidatesResponse>> bulkAssignCandidates(
            @PathVariable String idOrSlug,
            @Valid @RequestBody BulkAssignCandidatesRequest request) {
        BulkAssignCandidatesResponse response = assessmentService.bulkAssignCandidates(idOrSlug, request);
        return ApiResponse.success(response, "Candidates processed successfully");
    }

    @GetMapping("/{idOrSlug}/candidates")
    public ResponseEntity<ApiResponse<PageResponse<CandidateAssignmentReportResponse>>> getCandidateAssignments(
            @PathVariable String idOrSlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<CandidateAssignmentReportResponse> response = assessmentService.getCandidateAssignments(idOrSlug, pageable);
        return ApiResponse.success(response, "Candidate assignments retrieved successfully");
    }

    @GetMapping("/{idOrSlug}/attempts")
    public ResponseEntity<ApiResponse<PageResponse<AttemptReportResponse>>> getAssessmentAttempts(
            @PathVariable String idOrSlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        PageResponse<AttemptReportResponse> response = assessmentService.getAssessmentAttempts(idOrSlug, pageable);
        return ApiResponse.success(response, "Assessment attempts retrieved successfully");
    }

    @GetMapping("/attempts/{attemptId}/events")
    public ResponseEntity<ApiResponse<PageResponse<AssessmentConnectionEvent>>> getAttemptConnectionEvents(
            @PathVariable String attemptId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        PageResponse<AssessmentConnectionEvent> response = assessmentService.getAttemptConnectionEvents(attemptId, pageable);
        return ApiResponse.success(response, "Attempt connection events retrieved successfully");
    }

    @PostMapping("/{idOrSlug}/invitation-code")
    public ResponseEntity<ApiResponse<AssessmentDetailResponse>> generateInvitationCode(
            @PathVariable String idOrSlug) {
        AssessmentDetailResponse response = assessmentService.generateInvitationCode(idOrSlug);
        return ApiResponse.success(response, "Invitation code generated successfully");
    }

    @DeleteMapping("/{idOrSlug}/invitation-code")
    public ResponseEntity<ApiResponse<AssessmentDetailResponse>> revokeInvitationCode(
            @PathVariable String idOrSlug) {
        AssessmentDetailResponse response = assessmentService.revokeInvitationCode(idOrSlug);
        return ApiResponse.success(response, "Invitation code revoked successfully");
    }

    @GetMapping("/{idOrSlug}/metrics")
    public ResponseEntity<ApiResponse<AssessmentMetricsResponse>> getAssessmentMetrics(
            @PathVariable String idOrSlug) {
        AssessmentMetricsResponse response = assessmentService.getAssessmentMetrics(idOrSlug);
        return ApiResponse.success(response, "Assessment metrics retrieved successfully");
    }

    @GetMapping("/attempts/{attemptId}/detail")
    public ResponseEntity<ApiResponse<CandidateAssessmentResultResponse>> getAttemptDetailForAdmin(
            @PathVariable String attemptId) {
        CandidateAssessmentResultResponse response = assessmentService.getAttemptDetailForAdmin(attemptId);
        return ApiResponse.success(response, "Attempt detail retrieved successfully");
    }

    @GetMapping("/attempts/{attemptId}/audit")
    public ResponseEntity<ApiResponse<List<AssessmentAuditEvent>>> getAttemptAuditTrail(
            @PathVariable String attemptId) {
        List<AssessmentAuditEvent> response = assessmentService.getAttemptAuditTrail(attemptId);
        return ApiResponse.success(response, "Attempt audit trail retrieved successfully");
    }
}

