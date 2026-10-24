import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import axios from "axios"
import { toast } from "@/components/ui/toast"
import { questionBankService } from "@/services/question-bank-service"
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

export const questionBankKeys = {
  all: ["question-banks"] as const,
  banks: () => [...questionBankKeys.all, "banks"] as const,
  bankList: (filters: QuestionBankFilterParams) =>
    [...questionBankKeys.banks(), "list", filters] as const,
  bankDetail: (id: string) =>
    [...questionBankKeys.banks(), "detail", id] as const,
  stats: () => [...questionBankKeys.all, "stats"] as const,
  categories: () => [...questionBankKeys.all, "categories"] as const,
}

/**
 * Hook to retrieve paginated and filtered Question Banks
 */
export function useQuestionBanks(filters: QuestionBankFilterParams = {}) {
  return useQuery<PaginatedResponse<QuestionBankSummary>>({
    queryKey: questionBankKeys.bankList(filters),
    queryFn: () => questionBankService.getQuestionBanks(filters),
    staleTime: 30 * 1000,
  })
}

/**
 * Hook to retrieve a single Question Bank by ID
 */
export function useQuestionBank(id?: string) {
  return useQuery<QuestionBankDetail>({
    queryKey: questionBankKeys.bankDetail(id || ""),
    queryFn: () => questionBankService.getQuestionBank(id!),
    enabled: Boolean(id),
  })
}

/**
 * Hook to check if a Question Bank is in use
 */
export function useQuestionBankInUse(id?: string) {
  return useQuery<boolean>({
    queryKey: [...questionBankKeys.banks(), "in-use", id],
    queryFn: () => questionBankService.isInUse(id!),
    enabled: Boolean(id),
  })
}

/**
 * Hook to retrieve Question Bank Stats
 */
export function useQuestionBankStats() {
  return useQuery<QuestionBankStats>({
    queryKey: questionBankKeys.stats(),
    queryFn: () => questionBankService.getStats(),
    staleTime: 60 * 1000,
  })
}

/**
 * Hook to retrieve distinct Question Bank categories
 */
export function useQuestionBankCategories() {
  return useQuery<string[]>({
    queryKey: questionBankKeys.categories(),
    queryFn: () => questionBankService.getCategories(),
    staleTime: 60 * 1000,
  })
}

/**
 * Hook to create a new Question Bank
 */
export function useCreateQuestionBank() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CreateQuestionBankPayload) =>
      questionBankService.createQuestionBank(payload),
    onSuccess: (created) => {
      toast.add({
        title: "Question Bank Created",
        description: `Question Bank "${created.title}" has been created successfully.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.banks() })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.stats() })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.categories() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Create Question Bank",
        description: extractErrorMessage(error, "Could not create Question Bank."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to update an existing Question Bank
 */
export function useUpdateQuestionBank() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      id,
      payload,
    }: {
      id: string
      payload: UpdateQuestionBankPayload
    }) => questionBankService.updateQuestionBank(id, payload),
    onSuccess: (updated) => {
      toast.add({
        title: "Question Bank Updated",
        description: `Question Bank "${updated.title}" has been updated successfully.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.banks() })
      queryClient.invalidateQueries({
        queryKey: questionBankKeys.bankDetail(updated.id),
      })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.stats() })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.categories() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Update Question Bank",
        description: extractErrorMessage(error, "Could not update Question Bank."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to duplicate an existing Question Bank
 */
export function useDuplicateQuestionBank() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) =>
      questionBankService.duplicateQuestionBank(id),
    onSuccess: (duplicated) => {
      toast.add({
        title: "Question Bank Duplicated",
        description: `Duplicated as "${duplicated.title}".`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.banks() })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.stats() })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.categories() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Duplicate Question Bank",
        description: extractErrorMessage(error, "Could not duplicate Question Bank."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to delete an existing Question Bank
 */
export function useDeleteQuestionBank() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => questionBankService.deleteQuestionBank(id),
    onSuccess: () => {
      toast.add({
        title: "Question Bank Deleted",
        description: "Question Bank has been deleted successfully.",
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.banks() })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.stats() })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.categories() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Delete Question Bank",
        description: extractErrorMessage(error, "Could not delete Question Bank."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to update Question Bank status (DRAFT, PUBLISHED)
 */
export function useUpdateQuestionBankStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      id,
      status,
    }: {
      id: string
      status: QuestionBankStatus
    }) => questionBankService.updateStatus(id, status),
    onSuccess: (updated) => {
      toast.add({
        title: "Status Updated",
        description: `Question Bank "${updated.title}" is now ${updated.status.toLowerCase()}.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.banks() })
      queryClient.invalidateQueries({
        queryKey: questionBankKeys.bankDetail(updated.id),
      })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.stats() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Update Status",
        description: extractErrorMessage(error, "Could not update Question Bank status."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to publish a Question Bank
 */
export function usePublishQuestionBank() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => questionBankService.publish(id),
    onSuccess: (updated) => {
      toast.add({
        title: "Question Bank Published",
        description: `Question Bank "${updated.title}" is now published.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.banks() })
      queryClient.invalidateQueries({
        queryKey: questionBankKeys.bankDetail(updated.id),
      })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.stats() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Publish Failed",
        description: extractErrorMessage(error, "Could not publish Question Bank."),
        type: "error",
      })
    },
  })
}

/**
 * Hook to unpublish a Question Bank (restore to DRAFT)
 */
export function useUnpublishQuestionBank() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => questionBankService.unpublish(id),
    onSuccess: (updated) => {
      toast.add({
        title: "Question Bank Unpublished",
        description: `Question Bank "${updated.title}" is now saved as draft.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.banks() })
      queryClient.invalidateQueries({
        queryKey: questionBankKeys.bankDetail(updated.id),
      })
      queryClient.invalidateQueries({ queryKey: questionBankKeys.stats() })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Unpublish Failed",
        description: extractErrorMessage(error, "Could not unpublish Question Bank."),
        type: "error",
      })
    },
  })
}

