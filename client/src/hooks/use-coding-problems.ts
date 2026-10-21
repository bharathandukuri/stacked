import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import axios from "axios"
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
 * Hook to update a problem's status (PUBLISHED, UNPUBLISHED, DRAFT, or ARCHIVED).
 */
export function useUpdateProblemStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      idOrSlug,
      status,
    }: {
      idOrSlug: string
      status: "PUBLISHED" | "UNPUBLISHED" | "DRAFT" | "ARCHIVED"
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
    onError: (error: unknown) => {
      let message = "Could not update status."
      if (axios.isAxiosError(error)) {
        const data = error.response?.data as { message?: string } | undefined
        if (data?.message) message = data.message
      } else if (error instanceof Error) {
        message = error.message
      }
      toast.add({
        title: "Failed to Update Status",
        description: message,
        type: "error",
      })
    },
  })
}

/**
 * Hook to publish a coding problem.
 */
export function usePublishCodingProblem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (idOrSlug: string) => codingProblemService.publishProblem(idOrSlug),
    onSuccess: (updated) => {
      toast.add({
        title: "Problem Published",
        description: `Problem "${updated.title}" is now published.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: codingProblemKeys.lists() })
      queryClient.invalidateQueries({ queryKey: codingProblemKeys.detail(updated.id) })
      queryClient.invalidateQueries({ queryKey: codingProblemKeys.detail(updated.slug) })
    },
    onError: (error: unknown) => {
      let message = "Could not publish problem."
      if (axios.isAxiosError(error)) {
        const data = error.response?.data as { message?: string } | undefined
        if (data?.message) message = data.message
      } else if (error instanceof Error) {
        message = error.message
      }
      toast.add({
        title: "Publish Failed",
        description: message,
        type: "error",
      })
    },
  })
}

/**
 * Hook to unpublish a coding problem.
 */
export function useUnpublishCodingProblem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (idOrSlug: string) => codingProblemService.unpublishProblem(idOrSlug),
    onSuccess: (updated) => {
      toast.add({
        title: "Problem Unpublished",
        description: `Problem "${updated.title}" is now unpublished.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: codingProblemKeys.lists() })
      queryClient.invalidateQueries({ queryKey: codingProblemKeys.detail(updated.id) })
      queryClient.invalidateQueries({ queryKey: codingProblemKeys.detail(updated.slug) })
    },
    onError: (error: unknown) => {
      let message = "Could not unpublish problem."
      if (axios.isAxiosError(error)) {
        const data = error.response?.data as { message?: string } | undefined
        if (data?.message) message = data.message
      } else if (error instanceof Error) {
        message = error.message
      }
      toast.add({
        title: "Unpublish Failed",
        description: message,
        type: "error",
      })
    },
  })
}

/**
 * Hook to archive a coding problem.
 */
export function useArchiveCodingProblem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (idOrSlug: string) => codingProblemService.archiveProblem(idOrSlug),
    onSuccess: (updated) => {
      toast.add({
        title: "Problem Archived",
        description: `Problem "${updated.title}" has been archived.`,
        type: "success",
      })
      queryClient.invalidateQueries({ queryKey: codingProblemKeys.lists() })
      queryClient.invalidateQueries({ queryKey: codingProblemKeys.detail(updated.id) })
      queryClient.invalidateQueries({ queryKey: codingProblemKeys.detail(updated.slug) })
    },
    onError: (error: unknown) => {
      let message = "Could not archive problem."
      if (axios.isAxiosError(error)) {
        const data = error.response?.data as { message?: string } | undefined
        if (data?.message) message = data.message
      } else if (error instanceof Error) {
        message = error.message
      }
      toast.add({
        title: "Archive Failed",
        description: message,
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

