import API from "@/lib/api-client"
import type { ApiResponse } from "@/types/api"
import type { PaginatedResponse } from "@/services/coding-problem-service"
import type {
  CreateMcqPayload,
  CreateMcqTopicPayload,
  McqFilterParams,
  McqQuestionDetail,
  McqQuestionItem,
  McqStats,
  McqStatus,
  McqTopicItem,
  UpdateMcqPayload,
  UpdateMcqTopicPayload,
} from "@/types/mcq"

export const mcqService = {
  /**
   * List paginated and filtered MCQ questions
   */
  async getQuestions(
    params: McqFilterParams = {}
  ): Promise<PaginatedResponse<McqQuestionItem>> {
    const cleanParams: Record<string, string | number> = {}

    if (params.search?.trim()) cleanParams.search = params.search.trim()
    if (params.topic?.trim()) cleanParams.topic = params.topic.trim()
    if (params.difficulty) cleanParams.difficulty = params.difficulty
    if (params.type) cleanParams.type = params.type
    if (params.status) cleanParams.status = params.status
    if (params.page !== undefined) cleanParams.page = params.page
    if (params.size !== undefined) cleanParams.size = params.size
    if (params.sortBy) cleanParams.sortBy = params.sortBy
    if (params.sortDirection) cleanParams.sortDirection = params.sortDirection

    const response = await API.get<ApiResponse<PaginatedResponse<McqQuestionItem>>>(
      "/admin/mcq/questions",
      { params: cleanParams }
    )
    return response.data.data
  },

  /**
   * Get single MCQ question by ID
   */
  async getQuestion(id: string): Promise<McqQuestionDetail> {
    const response = await API.get<ApiResponse<McqQuestionDetail>>(
      `/admin/mcq/questions/${encodeURIComponent(id)}`
    )
    return response.data.data
  },

  /**
   * Create a new MCQ question
   */
  async createQuestion(payload: CreateMcqPayload): Promise<McqQuestionDetail> {
    const response = await API.post<ApiResponse<McqQuestionDetail>>(
      "/admin/mcq/questions",
      payload
    )
    return response.data.data
  },

  /**
   * Update an existing MCQ question
   */
  async updateQuestion(
    id: string,
    payload: UpdateMcqPayload
  ): Promise<McqQuestionDetail> {
    const response = await API.put<ApiResponse<McqQuestionDetail>>(
      `/admin/mcq/questions/${encodeURIComponent(id)}`,
      payload
    )
    return response.data.data
  },

  /**
   * Delete an MCQ question
   */
  async deleteQuestion(id: string): Promise<void> {
    await API.delete<ApiResponse<void>>(
      `/admin/mcq/questions/${encodeURIComponent(id)}`
    )
  },

  /**
   * Update question status
   */
  async updateStatus(
    id: string,
    status: McqStatus
  ): Promise<McqQuestionDetail> {
    const response = await API.patch<ApiResponse<McqQuestionDetail>>(
      `/admin/mcq/questions/${encodeURIComponent(id)}/status`,
      { status }
    )
    return response.data.data
  },

  /**
   * Publish a question
   */
  async publishQuestion(id: string): Promise<McqQuestionDetail> {
    const response = await API.patch<ApiResponse<McqQuestionDetail>>(
      `/admin/mcq/questions/${encodeURIComponent(id)}/publish`
    )
    return response.data.data
  },

  /**
   * Unpublish a question (restore to DRAFT)
   */
  async unpublishQuestion(id: string): Promise<McqQuestionDetail> {
    const response = await API.patch<ApiResponse<McqQuestionDetail>>(
      `/admin/mcq/questions/${encodeURIComponent(id)}/unpublish`
    )
    return response.data.data
  },

  /**
   * Check if a question is currently in use
   */
  async isInUse(id: string): Promise<boolean> {
    const response = await API.get<ApiResponse<{ inUse: boolean }>>(
      `/admin/mcq/questions/${encodeURIComponent(id)}/in-use`
    )
    return response.data.data.inUse
  },

  /**
   * Get MCQ overview stats
   */
  async getStats(): Promise<McqStats> {
    const response = await API.get<ApiResponse<McqStats>>("/admin/mcq/stats")
    return response.data.data
  },

  /**
   * List all MCQ topics
   */
  async getTopics(): Promise<McqTopicItem[]> {
    const response = await API.get<ApiResponse<McqTopicItem[]>>(
      "/admin/mcq/topics"
    )
    return response.data.data
  },

  /**
   * Create a new MCQ topic
   */
  async createTopic(payload: CreateMcqTopicPayload): Promise<McqTopicItem> {
    const response = await API.post<ApiResponse<McqTopicItem>>(
      "/admin/mcq/topics",
      payload
    )
    return response.data.data
  },

  /**
   * Update an MCQ topic
   */
  async updateTopic(
    id: string,
    payload: UpdateMcqTopicPayload
  ): Promise<McqTopicItem> {
    const response = await API.put<ApiResponse<McqTopicItem>>(
      `/admin/mcq/topics/${encodeURIComponent(id)}`,
      payload
    )
    return response.data.data
  },

  /**
   * Delete an MCQ topic
   */
  async deleteTopic(id: string): Promise<void> {
    await API.delete<ApiResponse<void>>(
      `/admin/mcq/topics/${encodeURIComponent(id)}`
    )
  },
}

