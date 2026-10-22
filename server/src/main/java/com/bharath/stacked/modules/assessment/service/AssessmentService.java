package com.bharath.stacked.modules.assessment.service;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.assessment.dto.*;
import com.bharath.stacked.modules.assessment.enums.AssessmentStatus;
import com.bharath.stacked.modules.assessment.model.AssessmentConnectionEvent;
import com.bharath.stacked.modules.coding_problem.dto.response.CodingProblemExecutionResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AssessmentService {

    // ==========================================
    // Administrator Operations
    // ==========================================

    AssessmentSummaryResponse createAssessment(CreateAssessmentRequest request, String authorId);

    AssessmentValidationResponse validateAssessmentConfig(CreateAssessmentRequest request);

    QuestionBankPreviewDto getQuestionBankPreview(String bankId);

    PageResponse<AssessmentSummaryResponse> getAssessments(AssessmentFilterRequest filter);

    AssessmentDetailResponse getAssessment(String idOrSlug);

    void deleteAssessment(String idOrSlug);

    AssessmentDetailResponse completeAssessment(String idOrSlug);

    BulkAssignCandidatesResponse bulkAssignCandidates(String idOrSlug, BulkAssignCandidatesRequest request);

    PageResponse<CandidateAssignmentReportResponse> getCandidateAssignments(String idOrSlug, Pageable pageable);

    PageResponse<AttemptReportResponse> getAssessmentAttempts(String idOrSlug, Pageable pageable);

    PageResponse<AssessmentConnectionEvent> getAttemptConnectionEvents(String attemptId, Pageable pageable);

    AssessmentDetailResponse generateInvitationCode(String idOrSlug);

    AssessmentDetailResponse revokeInvitationCode(String idOrSlug);

    // ==========================================
    // Candidate Operations
    // ==========================================

    CandidateAssessmentSummaryResponse joinByInvitationCode(String candidateUserId, String candidateEmail, JoinByCodeRequest request);

    AssessmentReadinessResponse getReadiness(String idOrSlug, String candidateUserId, String candidateEmail);

    List<CandidateAssessmentSummaryResponse> getCandidateAssessments(String candidateUserId, String candidateEmail);

    CandidateAssessmentSummaryResponse getCandidateAssessmentSummary(String idOrSlug, String candidateUserId, String candidateEmail);

    StartAttemptResponse startAttempt(String idOrSlug, String candidateUserId, String candidateEmail);

    CandidateAttemptDetailResponse getActiveAttemptDetail(String idOrSlug, String candidateUserId);

    SavedQuestionResponseDto saveMcqResponse(String idOrSlug, String candidateUserId, SaveMcqResponseRequest request);

    CodingProblemExecutionResponse runSample(String idOrSlug, String candidateUserId, RunAssessmentSampleRequest request);

    CodingProblemExecutionResponse runCustomInput(String idOrSlug, String candidateUserId, RunAssessmentCustomInputRequest request);

    SavedQuestionResponseDto saveCodeResponse(String idOrSlug, String candidateUserId, SaveCodeResponseRequest request);

    SavedQuestionResponseDto submitCodeResponse(String idOrSlug, String candidateUserId, SubmitCodeResponseRequest request);

    SavedQuestionResponseDto markForReview(String idOrSlug, String candidateUserId, MarkQuestionForReviewRequest request);

    FinalSubmitResponse finalSubmit(String idOrSlug, String candidateUserId);

    // ==========================================
    // Audit & Results Operations
    // ==========================================

    void recordTelemetry(String idOrSlug, String candidateUserId, List<ClientTelemetryEventDto> events, String clientIp, String userAgent);

    CandidateAssessmentResultResponse getCandidateResults(String idOrSlug, String candidateUserId);

    CandidateAssessmentResultResponse getAttemptDetailForAdmin(String attemptId);

    AssessmentMetricsResponse getAssessmentMetrics(String idOrSlug);

    List<com.bharath.stacked.modules.assessment.model.AssessmentAuditEvent> getAttemptAuditTrail(String attemptId);
}

