import API from "@/lib/api-client"
import type { ApiResponse } from "@/types/api"
import type { PaginatedResponse } from "@/services/coding-problem-service"
import type {
  CreateQuestionBankPayload,
  QuestionBankDetail,
  QuestionBankFilterParams,
  QuestionBankStats,
  QuestionBankStatus,
  QuestionBankSummary,
  UpdateQuestionBankPayload,
} from "@/types/question-bank"

export const questionBankService = {
  /**
   * List paginated and filtered Question Banks
   */
  async getQuestionBanks(
    params: QuestionBankFilterParams = {}
  ): Promise<PaginatedResponse<QuestionBankSummary>> {
    const cleanParams: Record<string, string | number> = {}

    if (params.search?.trim()) cleanParams.search = params.search.trim()
    if (params.category?.trim()) cleanParams.category = params.category.trim()
    if (params.status) cleanParams.status = params.status
    if (params.page !== undefined) cleanParams.page = params.page
    if (params.size !== undefined) cleanParams.size = params.size
    if (params.sortBy) cleanParams.sortBy = params.sortBy
    if (params.sortDirection) cleanParams.sortDirection = params.sortDirection

    const response = await API.get<ApiResponse<PaginatedResponse<QuestionBankSummary>>>(
      "/admin/question-banks",
      { params: cleanParams }
    )
    return response.data.data
  },

  /**
   * Get single Question Bank by ID or Slug (with hydrated questions)
   */
  async getQuestionBank(idOrSlug: string): Promise<QuestionBankDetail> {
    const response = await API.get<ApiResponse<QuestionBankDetail>>(
      `/admin/question-banks/${encodeURIComponent(idOrSlug)}`
    )
    return response.data.data
  },

  /**
   * Create a new Question Bank
   */
  async createQuestionBank(payload: CreateQuestionBankPayload): Promise<QuestionBankSummary> {
    const response = await API.post<ApiResponse<QuestionBankSummary>>(
      "/admin/question-banks",
      payload
    )
    return response.data.data
  },

  /**
   * Update an existing Question Bank
   */
  async updateQuestionBank(
    idOrSlug: string,
    payload: UpdateQuestionBankPayload
  ): Promise<QuestionBankSummary> {
    const response = await API.put<ApiResponse<QuestionBankSummary>>(
      `/admin/question-banks/${encodeURIComponent(idOrSlug)}`,
      payload
    )
    return response.data.data
  },

  /**
   * Delete a Question Bank
   */
  async deleteQuestionBank(idOrSlug: string): Promise<void> {
    await API.delete(`/admin/question-banks/${encodeURIComponent(idOrSlug)}`)
  },

  /**
   * Update question bank status
   */
  async updateStatus(
    idOrSlug: string,
    status: QuestionBankStatus
  ): Promise<QuestionBankSummary> {
    const response = await API.patch<ApiResponse<QuestionBankSummary>>(
      `/admin/question-banks/${encodeURIComponent(idOrSlug)}/status`,
      { status }
    )
    return response.data.data
  },

  /**
   * Publish a question bank
   */
  async publish(idOrSlug: string): Promise<QuestionBankSummary> {
    const response = await API.patch<ApiResponse<QuestionBankSummary>>(
      `/admin/question-banks/${encodeURIComponent(idOrSlug)}/publish`
    )
    return response.data.data
  },

  /**
   * Unpublish a question bank (restore to DRAFT)
   */
  async unpublish(idOrSlug: string): Promise<QuestionBankSummary> {
    const response = await API.patch<ApiResponse<QuestionBankSummary>>(
      `/admin/question-banks/${encodeURIComponent(idOrSlug)}/unpublish`
    )
    return response.data.data
  },

  /**
   * Archive a question bank
   */
  async archive(idOrSlug: string): Promise<QuestionBankSummary> {
    const response = await API.patch<ApiResponse<QuestionBankSummary>>(
      `/admin/question-banks/${encodeURIComponent(idOrSlug)}/archive`
    )
    return response.data.data
  },

  /**
   * Duplicate a Question Bank
   */
  async duplicateQuestionBank(idOrSlug: string): Promise<QuestionBankSummary> {
    const response = await API.post<ApiResponse<QuestionBankSummary>>(
      `/admin/question-banks/${encodeURIComponent(idOrSlug)}/duplicate`
    )
    return response.data.data
  },

  /**
   * Get distinct categories
   */
  async getCategories(): Promise<string[]> {
    const response = await API.get<ApiResponse<string[]>>("/admin/question-banks/categories")
    return response.data.data
  },

  /**
   * Get high-level stats
   */
  async getStats(): Promise<QuestionBankStats> {
    const response = await API.get<ApiResponse<QuestionBankStats>>("/admin/question-banks/stats")
    return response.data.data
  },
}

