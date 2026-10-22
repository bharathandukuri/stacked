import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import axios from "axios"
import { toast } from "@/components/ui/toast"
import { mcqService } from "@/services/mcq-service"
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
} from "@/types/mcq"

function extractErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined
    if (data?.message) return data.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return fallback
}

export const mcqKeys = {
  all: ["mcq"] as const,
  questions: () => [...mcqKeys.all, "questions"] as const,
  questionList: (filters: McqFilterParams) =>
    [...mcqKeys.questions(), "list", filters] as const,
  questionDetail: (idOrSlug: string) =>
    [...mcqKeys.questions(), "detail", idOrSlug] as const,
  stats: () => [...mcqKeys.all, "stats"] as const,
  topics: () => [...mcqKeys.all, "topics"] as const,
}

/**
 * Hook to retrieve paginated and filtered MCQ questions
 */
export function useMcqQuestions(filters: McqFilterParams = {}) {
  return useQuery<PaginatedResponse<McqQuestionItem>>({
    queryKey: mcqKeys.questionList(filters),
    queryFn: () => mcqService.getQuestions(filters),
    staleTime: 30 * 1000,
  })
}

/**
 * Hook to retrieve a single MCQ question
 */
export function useMcqQuestion(idOrSlug?: string) {
  return useQuery<McqQuestionDetail>({
    queryKey: mcqKeys.questionDetail(idOrSlug || ""),
    queryFn: () => mcqService.getQuestion(idOrSlug!),
    enabled: Boolean(idOrSlug),
  })
}

/**
 * Hook to create a new MCQ question
 */
export function useCreateMcqQuestion() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CreateMcqPayload) => mcqService.createQuestion(payload),
    onSuccess: (created) => {
      toast.add({
        title: "MCQ Created",
        description: `Question "${created.title}" has been created successfully.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questions() })
      queryClient.invalidateQueries({ queryKey: mcqKeys.stats() })
      queryClient.invalidateQueries({ queryKey: mcqKeys.topics() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Create MCQ",
        description: extractErrorMessage(error, "Could not create MCQ question."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to update an existing MCQ question
 */
export function useUpdateMcqQuestion() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      idOrSlug,
      payload,
    }: {
      idOrSlug: string
      payload: UpdateMcqPayload
    }) => mcqService.updateQuestion(idOrSlug, payload),
    onSuccess: (updated) => {
      toast.add({
        title: "MCQ Updated",
        description: `Question "${updated.title}" has been updated successfully.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questions() })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questionDetail(updated.id) })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questionDetail(updated.slug) })
      queryClient.invalidateQueries({ queryKey: mcqKeys.stats() })
      queryClient.invalidateQueries({ queryKey: mcqKeys.topics() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Update MCQ",
        description: extractErrorMessage(error, "Could not update MCQ question."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to delete an existing MCQ question
 */
export function useDeleteMcqQuestion() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (idOrSlug: string) => mcqService.deleteQuestion(idOrSlug),
    onSuccess: () => {
      toast.add({
        title: "MCQ Deleted",
        description: "Question has been deleted successfully.",
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questions() })
      queryClient.invalidateQueries({ queryKey: mcqKeys.stats() })
      queryClient.invalidateQueries({ queryKey: mcqKeys.topics() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Delete MCQ",
        description: extractErrorMessage(error, "Could not delete MCQ question."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to update MCQ question status (DRAFT, PUBLISHED)
 */
export function useUpdateMcqStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      idOrSlug,
      status,
    }: {
      idOrSlug: string
      status: McqStatus
    }) => mcqService.updateStatus(idOrSlug, status),
    onSuccess: (updated) => {
      toast.add({
        title: "Status Updated",
        description: `MCQ "${updated.title}" is now ${updated.status.toLowerCase()}.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questions() })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questionDetail(updated.id) })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questionDetail(updated.slug) })
      queryClient.invalidateQueries({ queryKey: mcqKeys.stats() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Update Status",
        description: extractErrorMessage(error, "Could not update MCQ status."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to publish an MCQ question
 */
export function usePublishMcq() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (idOrSlug: string) => mcqService.publishQuestion(idOrSlug),
    onSuccess: (updated) => {
      toast.add({
        title: "MCQ Published",
        description: `Question "${updated.title}" is now published.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questions() })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questionDetail(updated.id) })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questionDetail(updated.slug) })
      queryClient.invalidateQueries({ queryKey: mcqKeys.stats() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Publish Failed",
        description: extractErrorMessage(error, "Could not publish MCQ question."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to unpublish an MCQ question
 */
export function useUnpublishMcq() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (idOrSlug: string) => mcqService.unpublishQuestion(idOrSlug),
    onSuccess: (updated) => {
      toast.add({
        title: "MCQ Unpublished",
        description: `Question "${updated.title}" is now saved as draft.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questions() })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questionDetail(updated.id) })
      queryClient.invalidateQueries({ queryKey: mcqKeys.questionDetail(updated.slug) })
      queryClient.invalidateQueries({ queryKey: mcqKeys.stats() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Unpublish Failed",
        description: extractErrorMessage(error, "Could not unpublish MCQ question."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to retrieve MCQ overview metrics
 */
export function useMcqStats() {
  return useQuery<McqStats>({
    queryKey: mcqKeys.stats(),
    queryFn: () => mcqService.getStats(),
    staleTime: 60 * 1000,
  })
}

/**
 * Hook to retrieve all MCQ topics
 */
export function useMcqTopics() {
  return useQuery<McqTopicItem[]>({
    queryKey: mcqKeys.topics(),
    queryFn: () => mcqService.getTopics(),
    staleTime: 60 * 1000,
  })
}

/**
 * Hook to create a new MCQ topic
 */
export function useCreateMcqTopic() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CreateMcqTopicPayload) => mcqService.createTopic(payload),
    onSuccess: (created) => {
      toast.add({
        title: "Topic Created",
        description: `Topic "${created.name}" has been created.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: mcqKeys.topics() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Create Topic",
        description: extractErrorMessage(error, "Could not create topic."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to delete an MCQ topic
 */
export function useDeleteMcqTopic() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => mcqService.deleteTopic(id),
    onSuccess: () => {
      toast.add({
        title: "Topic Deleted",
        description: "Topic has been removed.",
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: mcqKeys.topics() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Delete Topic",
        description: extractErrorMessage(error, "Could not delete topic."),
        type: "error",
      })
    },
  })
}

