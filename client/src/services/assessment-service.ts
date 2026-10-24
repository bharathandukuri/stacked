import API from "@/lib/api-client"
import type { ApiResponse, PageResponse } from "@/types/api"
import type {
  AssessmentDetailResponse,
  AssessmentStatus,
  AssessmentSummaryResponse,
  AttemptReport,
  BulkAssignCandidatesPayload,
  BulkAssignCandidatesResponse,
  CandidateAssessmentSummary,
  CandidateAssignmentReport,
  CandidateAttemptDetail,
  CreateAssessmentPayload,
  FinalSubmitResponse,
  RunAssessmentSamplePayload,
  SaveCodeResponsePayload,
  SaveMcqResponsePayload,
  SavedQuestionResponse,
  StartAttemptResponse,
  SubmitCodeResponsePayload,
  AssessmentValidationResponse,
  QuestionBankPreviewDto,
  AssessmentConnectionEvent,
  CodingProblemExecutionResponse,
  AssessmentReadinessResponse,
  JoinByCodePayload,
  MarkForReviewPayload,
  RunAssessmentCustomInputPayload,
  CandidateAssessmentResultResponse,
  ClientTelemetryEventDto,
  AssessmentMetricsResponse,
  AssessmentAuditEvent,
} from "@/types/assessment"

export interface AssessmentFilterParams {
  search?: string
  status?: AssessmentStatus
  page?: number
  size?: number
  sortBy?: string
  sortDir?: "asc" | "desc"
}

export const assessmentService = {
  // ==========================================
  // Administrator Operations
  // ==========================================

  async createAssessment(
    payload: CreateAssessmentPayload
  ): Promise<AssessmentSummaryResponse> {
    const res = await API.post<ApiResponse<AssessmentSummaryResponse>>(
      "/admin/assessments",
      payload
    )
    return res.data.data
  },

  async getAssessments(
    params?: AssessmentFilterParams
  ): Promise<PageResponse<AssessmentSummaryResponse>> {
    const res = await API.get<ApiResponse<PageResponse<AssessmentSummaryResponse>>>(
      "/admin/assessments",
      { params }
    )
    return res.data.data
  },

  async getAssessment(idOrSlug: string): Promise<AssessmentDetailResponse> {
    const res = await API.get<ApiResponse<AssessmentDetailResponse>>(
      `/admin/assessments/${idOrSlug}`
    )
    return res.data.data
  },

  async validateAssessment(
    payload: CreateAssessmentPayload
  ): Promise<AssessmentValidationResponse> {
    const res = await API.post<ApiResponse<AssessmentValidationResponse>>(
      "/admin/assessments/validate",
      payload
    )
    return res.data.data
  },

  async getQuestionBankPreview(bankId: string): Promise<QuestionBankPreviewDto> {
    const res = await API.get<ApiResponse<QuestionBankPreviewDto>>(
      `/admin/assessments/question-bank-preview/${encodeURIComponent(bankId)}`
    )
    return res.data.data
  },

  async deleteAssessment(idOrSlug: string): Promise<void> {
    await API.delete<ApiResponse<void>>(`/admin/assessments/${idOrSlug}`)
  },

  async activateAssessment(idOrSlug: string): Promise<AssessmentDetailResponse> {
    const res = await API.post<ApiResponse<AssessmentDetailResponse>>(
      `/admin/assessments/${idOrSlug}/activate`
    )
    return res.data.data
  },

  async completeAssessment(idOrSlug: string): Promise<AssessmentDetailResponse> {
    const res = await API.post<ApiResponse<AssessmentDetailResponse>>(
      `/admin/assessments/${idOrSlug}/complete`
    )
    return res.data.data
  },

  async bulkAssignCandidates(
    idOrSlug: string,
    payload: BulkAssignCandidatesPayload
  ): Promise<BulkAssignCandidatesResponse> {
    const res = await API.post<ApiResponse<BulkAssignCandidatesResponse>>(
      `/admin/assessments/${idOrSlug}/candidates`,
      payload
    )
    return res.data.data
  },

  async getCandidateAssignments(
    idOrSlug: string,
    page = 0,
    size = 20
  ): Promise<PageResponse<CandidateAssignmentReport>> {
    const res = await API.get<ApiResponse<PageResponse<CandidateAssignmentReport>>>(
      `/admin/assessments/${idOrSlug}/candidates`,
      { params: { page, size } }
    )
    return res.data.data
  },

  async getAssessmentAttempts(
    idOrSlug: string,
    page = 0,
    size = 20
  ): Promise<PageResponse<AttemptReport>> {
    const res = await API.get<ApiResponse<PageResponse<AttemptReport>>>(
      `/admin/assessments/${idOrSlug}/attempts`,
      { params: { page, size } }
    )
    return res.data.data
  },

  async getAttemptConnectionEvents(
    attemptId: string,
    page = 0,
    size = 50
  ): Promise<PageResponse<AssessmentConnectionEvent>> {
    const res = await API.get<ApiResponse<PageResponse<AssessmentConnectionEvent>>>(
      `/admin/assessments/attempts/${attemptId}/events`,
      { params: { page, size } }
    )
    return res.data.data
  },

  async generateInvitationCode(idOrSlug: string): Promise<AssessmentDetailResponse> {
    const res = await API.post<ApiResponse<AssessmentDetailResponse>>(
      `/admin/assessments/${idOrSlug}/invitation-code`
    )
    return res.data.data
  },

  async revokeInvitationCode(idOrSlug: string): Promise<AssessmentDetailResponse> {
    const res = await API.delete<ApiResponse<AssessmentDetailResponse>>(
      `/admin/assessments/${idOrSlug}/invitation-code`
    )
    return res.data.data
  },

  async getAssessmentMetrics(idOrSlug: string): Promise<AssessmentMetricsResponse> {
    const res = await API.get<ApiResponse<AssessmentMetricsResponse>>(
      `/admin/assessments/${idOrSlug}/metrics`
    )
    return res.data.data
  },

  async getAttemptDetailForAdmin(attemptId: string): Promise<CandidateAssessmentResultResponse> {
    const res = await API.get<ApiResponse<CandidateAssessmentResultResponse>>(
      `/admin/assessments/attempts/${attemptId}/detail`
    )
    return res.data.data
  },

  async getAttemptAuditTrail(
    attemptId: string
  ): Promise<AssessmentAuditEvent[]> {
    const res = await API.get<ApiResponse<AssessmentAuditEvent[]>>(
      `/admin/assessments/attempts/${attemptId}/audit`
    )
    return res.data.data
  },

  // ==========================================
  // Candidate Operations
  // ==========================================

  async joinByCode(payload: JoinByCodePayload): Promise<CandidateAssessmentSummary> {
    const res = await API.post<ApiResponse<CandidateAssessmentSummary>>(
      "/assessments/join-by-code",
      payload
    )
    return res.data.data
  },

  async getMyAssessments(): Promise<CandidateAssessmentSummary[]> {
    const res = await API.get<ApiResponse<CandidateAssessmentSummary[]>>("/assessments")
    return res.data.data
  },

  async getAssessmentSummary(idOrSlug: string): Promise<CandidateAssessmentSummary> {
    const res = await API.get<ApiResponse<CandidateAssessmentSummary>>(
      `/assessments/${idOrSlug}/summary`
    )
    return res.data.data
  },

  async getReadiness(idOrSlug: string): Promise<AssessmentReadinessResponse> {
    const res = await API.get<ApiResponse<AssessmentReadinessResponse>>(
      `/assessments/${idOrSlug}/readiness`
    )
    return res.data.data
  },

  async startAttempt(idOrSlug: string): Promise<StartAttemptResponse> {
    const res = await API.post<ApiResponse<StartAttemptResponse>>(
      `/assessments/${idOrSlug}/start`
    )
    return res.data.data
  },

  async getActiveAttempt(idOrSlug: string): Promise<CandidateAttemptDetail> {
    const res = await API.get<ApiResponse<CandidateAttemptDetail>>(
      `/assessments/${idOrSlug}/attempt`
    )
    return res.data.data
  },

  async saveMcqResponse(
    idOrSlug: string,
    payload: SaveMcqResponsePayload
  ): Promise<SavedQuestionResponse> {
    const res = await API.post<ApiResponse<SavedQuestionResponse>>(
      `/assessments/${idOrSlug}/response/mcq`,
      payload
    )
    return res.data.data
  },

  async runSample(
    idOrSlug: string,
    payload: RunAssessmentSamplePayload
  ): Promise<CodingProblemExecutionResponse> {
    const res = await API.post<ApiResponse<CodingProblemExecutionResponse>>(
      `/assessments/${idOrSlug}/run-sample`,
      payload
    )
    return res.data.data
  },

  async runCustomInput(
    idOrSlug: string,
    payload: RunAssessmentCustomInputPayload
  ): Promise<CodingProblemExecutionResponse> {
    const res = await API.post<ApiResponse<CodingProblemExecutionResponse>>(
      `/assessments/${idOrSlug}/run-custom-input`,
      payload
    )
    return res.data.data
  },

  async saveCodeResponse(
    idOrSlug: string,
    payload: SaveCodeResponsePayload
  ): Promise<SavedQuestionResponse> {
    const res = await API.post<ApiResponse<SavedQuestionResponse>>(
      `/assessments/${idOrSlug}/response/code`,
      payload
    )
    return res.data.data
  },

  async submitCodeResponse(
    idOrSlug: string,
    payload: SubmitCodeResponsePayload
  ): Promise<SavedQuestionResponse> {
    const res = await API.post<ApiResponse<SavedQuestionResponse>>(
      `/assessments/${idOrSlug}/submit-code`,
      payload
    )
    return res.data.data
  },

  async markForReview(
    idOrSlug: string,
    payload: MarkForReviewPayload
  ): Promise<SavedQuestionResponse> {
    const res = await API.post<ApiResponse<SavedQuestionResponse>>(
      `/assessments/${idOrSlug}/response/mark-for-review`,
      payload
    )
    return res.data.data
  },

  async finalSubmit(idOrSlug: string): Promise<FinalSubmitResponse> {
    const res = await API.post<ApiResponse<FinalSubmitResponse>>(
      `/assessments/${idOrSlug}/submit`
    )
    return res.data.data
  },

  async getCandidateResults(idOrSlug: string): Promise<CandidateAssessmentResultResponse> {
    const res = await API.get<ApiResponse<CandidateAssessmentResultResponse>>(
      `/assessments/${idOrSlug}/results`
    )
    return res.data.data
  },

  async recordTelemetry(
    idOrSlug: string,
    events: ClientTelemetryEventDto[]
  ): Promise<void> {
    await API.post<ApiResponse<void>>(
      `/assessments/${idOrSlug}/telemetry`,
      events
    )
  },
}
