import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  codingProblemService,
  type CodingProblem,
  type CodingProblemFilterParams,
  type CodingProblemSummary,
  type PaginatedResponse,
} from "@/services/coding-problem-service"
import { toast } from "@/components/ui/toast"

export const codingProblemKeys = {
  all: ["coding-problems"] as const,
  lists: () => [...codingProblemKeys.all, "list"] as const,
  list: (filters: CodingProblemFilterParams) =>
    [...codingProblemKeys.lists(), filters] as const,
  details: () => [...codingProblemKeys.all, "detail"] as const,
  detail: (idOrSlug: string) =>
    [...codingProblemKeys.details(), idOrSlug] as const,
}

/**
 * Hook to fetch paginated and filtered coding problems.
 */
export function useCodingProblems(filters: CodingProblemFilterParams = {}) {
  return useQuery<PaginatedResponse<CodingProblemSummary>>({
    queryKey: codingProblemKeys.list(filters),
    queryFn: () => codingProblemService.getCodingProblems(filters),
    placeholderData: (previousData) => previousData,
  })
}

/**
 * Hook to fetch complete coding problem details by ID or Slug.
 */
export function useCodingProblem(idOrSlug?: string) {
  return useQuery<CodingProblem>({
    queryKey: codingProblemKeys.detail(idOrSlug || ""),
    queryFn: () => codingProblemService.getCodingProblem(idOrSlug!),
    enabled: Boolean(idOrSlug && idOrSlug.trim().length > 0),
  })
}

/**
 * Hook to update a problem's status (PUBLISHED or UNPUBLISHED).
 */
export function useUpdateProblemStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      idOrSlug,
      status,
    }: {
      idOrSlug: string
      status: "PUBLISHED" | "UNPUBLISHED"
    }) => codingProblemService.updateStatus(idOrSlug, status),
    onSuccess: (updated) => {
      toast.add({
        title: "Status Updated",
        description: `Problem "${updated.title}" is now ${updated.status.toLowerCase()}.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: codingProblemKeys.lists() })
      queryClient.invalidateQueries({
        queryKey: codingProblemKeys.detail(updated.id),
      })
      queryClient.invalidateQueries({
        queryKey: codingProblemKeys.detail(updated.slug),
      })
    },
    onError: (error: any) => {
      toast.add({
        title: "Failed to Update Status",
        description:
          error?.response?.data?.message ||
          error?.message ||
          "Could not update status.",
        type: "error",
      })
    },
  })
}

/**
 * Hook to update an existing coding problem.
 */
export function useUpdateCodingProblem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      idOrSlug,
      payload,
    }: {
      idOrSlug: string
      payload: unknown
    }) => codingProblemService.updateCodingProblem(idOrSlug, payload),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: codingProblemKeys.lists() })
      queryClient.invalidateQueries({
        queryKey: codingProblemKeys.detail(updated.id),
      })
      queryClient.invalidateQueries({
        queryKey: codingProblemKeys.detail(updated.slug),
      })
    },
  })
}

/**
 * Hook to delete an existing coding problem.
 */
export function useDeleteCodingProblem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (idOrSlug: string) =>
      codingProblemService.deleteCodingProblem(idOrSlug),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: codingProblemKeys.all })
    },
  })
}

