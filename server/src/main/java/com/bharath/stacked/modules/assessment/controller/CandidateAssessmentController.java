package com.bharath.stacked.modules.assessment.controller;

import com.bharath.stacked.common.api.ApiResponse;
import com.bharath.stacked.exception.BadRequestException;
import com.bharath.stacked.exception.UnauthorizedException;
import com.bharath.stacked.modules.assessment.dto.*;
import com.bharath.stacked.modules.assessment.service.AssessmentService;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import com.bharath.stacked.modules.security.model.User;
import com.bharath.stacked.modules.security.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CandidateAssessmentController {

    private final AssessmentService assessmentService;
    private final UserRepository userRepository;

    private String resolveUserId(java.security.Principal principal) {
        if (principal != null && StringUtils.hasText(principal.getName())) {
            return principal.getName();
        }
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            return auth.getPrincipal().toString();
        }
        throw new UnauthorizedException("Unauthenticated request.");
    }

    private String getCandidateEmail(String candidateUserId) {
        return userRepository.findById(candidateUserId)
                .map(User::getEmail)
                .orElseThrow(() -> new BadRequestException("Candidate account not found."));
    }

    @PostMapping("/join-by-code")
    public ResponseEntity<ApiResponse<CandidateAssessmentSummaryResponse>> joinByCode(
            @Valid @RequestBody JoinByCodeRequest request,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        String email = getCandidateEmail(candidateUserId);
        CandidateAssessmentSummaryResponse response = assessmentService.joinByInvitationCode(candidateUserId, email, request);
        return ApiResponse.success(response, "Successfully joined assessment with invitation code");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CandidateAssessmentSummaryResponse>>> getMyAssessments(
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        String email = getCandidateEmail(candidateUserId);
        List<CandidateAssessmentSummaryResponse> list = assessmentService.getCandidateAssessments(candidateUserId, email);
        return ApiResponse.success(list, "Assessments retrieved successfully");
    }

    @GetMapping("/{idOrSlug}/summary")
    public ResponseEntity<ApiResponse<CandidateAssessmentSummaryResponse>> getAssessmentSummary(
            @PathVariable String idOrSlug,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        String email = getCandidateEmail(candidateUserId);
        CandidateAssessmentSummaryResponse response = assessmentService.getCandidateAssessmentSummary(idOrSlug, candidateUserId, email);
        return ApiResponse.success(response, "Assessment summary retrieved successfully");
    }

    @GetMapping("/{idOrSlug}/readiness")
    public ResponseEntity<ApiResponse<AssessmentReadinessResponse>> getReadiness(
            @PathVariable String idOrSlug,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        String email = getCandidateEmail(candidateUserId);
        AssessmentReadinessResponse response = assessmentService.getReadiness(idOrSlug, candidateUserId, email);
        return ApiResponse.success(response, "Assessment readiness evaluated");
    }

    @PostMapping("/{idOrSlug}/start")
    public ResponseEntity<ApiResponse<StartAttemptResponse>> startAttempt(
            @PathVariable String idOrSlug,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        String email = getCandidateEmail(candidateUserId);
        StartAttemptResponse response = assessmentService.startAttempt(idOrSlug, candidateUserId, email);
        return ApiResponse.success(response, "Assessment attempt started");
    }

    @GetMapping("/{idOrSlug}/attempt")
    public ResponseEntity<ApiResponse<CandidateAttemptDetailResponse>> getActiveAttempt(
            @PathVariable String idOrSlug,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        CandidateAttemptDetailResponse response = assessmentService.getActiveAttemptDetail(idOrSlug, candidateUserId);
        return ApiResponse.success(response, "Active attempt retrieved");
    }

    @PostMapping("/{idOrSlug}/response/mcq")
    public ResponseEntity<ApiResponse<SavedQuestionResponseDto>> saveMcqResponse(
            @PathVariable String idOrSlug,
            @Valid @RequestBody SaveMcqResponseRequest request,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        SavedQuestionResponseDto response = assessmentService.saveMcqResponse(idOrSlug, candidateUserId, request);
        return ApiResponse.success(response, "MCQ response saved");
    }

    @PostMapping("/{idOrSlug}/run-sample")
    public ResponseEntity<ApiResponse<CodingProblemExecutionResponse>> runSample(
            @PathVariable String idOrSlug,
            @Valid @RequestBody RunAssessmentSampleRequest request,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        CodingProblemExecutionResponse response = assessmentService.runSample(idOrSlug, candidateUserId, request);
        return ApiResponse.success(response, "Sample execution completed");
    }

    @PostMapping("/{idOrSlug}/run-custom-input")
    public ResponseEntity<ApiResponse<CodingProblemExecutionResponse>> runCustomInput(
            @PathVariable String idOrSlug,
            @Valid @RequestBody RunAssessmentCustomInputRequest request,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        CodingProblemExecutionResponse response = assessmentService.runCustomInput(idOrSlug, candidateUserId, request);
        return ApiResponse.success(response, "Custom input execution completed");
    }

    @PostMapping("/{idOrSlug}/response/code")
    public ResponseEntity<ApiResponse<SavedQuestionResponseDto>> saveCodeResponse(
            @PathVariable String idOrSlug,
            @Valid @RequestBody SaveCodeResponseRequest request,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        SavedQuestionResponseDto response = assessmentService.saveCodeResponse(idOrSlug, candidateUserId, request);
        return ApiResponse.success(response, "Code saved");
    }

    @PostMapping("/{idOrSlug}/submit-code")
    public ResponseEntity<ApiResponse<SavedQuestionResponseDto>> submitCodeResponse(
            @PathVariable String idOrSlug,
            @Valid @RequestBody SubmitCodeResponseRequest request,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        SavedQuestionResponseDto response = assessmentService.submitCodeResponse(idOrSlug, candidateUserId, request);
        return ApiResponse.success(response, "Code evaluated and submitted");
    }

    @PostMapping("/{idOrSlug}/response/mark-for-review")
    public ResponseEntity<ApiResponse<SavedQuestionResponseDto>> markForReview(
            @PathVariable String idOrSlug,
            @Valid @RequestBody MarkQuestionForReviewRequest request,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        SavedQuestionResponseDto response = assessmentService.markForReview(idOrSlug, candidateUserId, request);
        return ApiResponse.success(response, "Question review mark updated");
    }

    @PostMapping("/{idOrSlug}/submit")
    public ResponseEntity<ApiResponse<FinalSubmitResponse>> finalSubmit(
            @PathVariable String idOrSlug,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        FinalSubmitResponse response = assessmentService.finalSubmit(idOrSlug, candidateUserId);
        return ApiResponse.success(response, "Assessment submitted successfully");
    }

    @GetMapping("/{idOrSlug}/results")
    public ResponseEntity<ApiResponse<CandidateAssessmentResultResponse>> getCandidateResults(
            @PathVariable String idOrSlug,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        CandidateAssessmentResultResponse response = assessmentService.getCandidateResults(idOrSlug, candidateUserId);
        return ApiResponse.success(response, "Candidate assessment results retrieved successfully");
    }

    @PostMapping("/{idOrSlug}/telemetry")
    public ResponseEntity<ApiResponse<Void>> recordTelemetry(
            @PathVariable String idOrSlug,
            @RequestBody List<ClientTelemetryEventDto> events,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            java.security.Principal principal) {
        String candidateUserId = resolveUserId(principal);
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        assessmentService.recordTelemetry(idOrSlug, candidateUserId, events, clientIp, userAgent);
        return ApiResponse.success(null, "Telemetry recorded");
    }
}

