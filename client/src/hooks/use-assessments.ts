import axios from "axios"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  assessmentService,
  type AssessmentFilterParams,
} from "@/services/assessment-service"
import type {
  BulkAssignCandidatesPayload,
  CandidateAttemptDetail,
  CreateAssessmentPayload,
  JoinByCodePayload,
  MarkForReviewPayload,
  RunAssessmentCustomInputPayload,
  RunAssessmentSamplePayload,
  SaveCodeResponsePayload,
  SaveMcqResponsePayload,
  SubmitCodeResponsePayload,
  ClientTelemetryEventDto,
} from "@/types/assessment"
import { toast } from "@/components/ui/toast"

function getErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined
    if (data?.message) return data.message
  } else if (error instanceof Error) {
    return error.message
  }
  return fallback
}

export const assessmentQueryKeys = {
  all: ["assessments"] as const,
  lists: () => [...assessmentQueryKeys.all, "list"] as const,
  list: (params?: AssessmentFilterParams) =>
    [...assessmentQueryKeys.lists(), params] as const,
  details: () => [...assessmentQueryKeys.all, "detail"] as const,
  detail: (idOrSlug: string) =>
    [...assessmentQueryKeys.details(), idOrSlug] as const,
  candidates: (idOrSlug: string, page: number, size: number) =>
    [...assessmentQueryKeys.detail(idOrSlug), "candidates", { page, size }] as const,
  attempts: (idOrSlug: string, page: number, size: number) =>
    [...assessmentQueryKeys.detail(idOrSlug), "attempts", { page, size }] as const,
  events: (attemptId: string, page: number, size: number) =>
    ["attempt-events", attemptId, { page, size }] as const,
  myAssessments: () => ["my-assessments"] as const,
  candidateSummary: (idOrSlug: string) =>
    ["candidate-assessment-summary", idOrSlug] as const,
  readiness: (idOrSlug: string) =>
    ["candidate-assessment-readiness", idOrSlug] as const,
  activeAttempt: (idOrSlug: string) =>
    ["candidate-active-attempt", idOrSlug] as const,
  candidateResults: (idOrSlug: string) =>
    ["candidate-assessment-results", idOrSlug] as const,
  metrics: (idOrSlug: string) =>
    [...assessmentQueryKeys.detail(idOrSlug), "metrics"] as const,
  attemptDetail: (attemptId: string) =>
    ["attempt-detail", attemptId] as const,
  attemptAudit: (attemptId: string) =>
    ["attempt-audit", attemptId] as const,
}

export function useAssessments(params?: AssessmentFilterParams) {
  return useQuery({
    queryKey: assessmentQueryKeys.list(params),
    queryFn: () => assessmentService.getAssessments(params),
  })
}

export function useAssessment(idOrSlug: string) {
  return useQuery({
    queryKey: assessmentQueryKeys.detail(idOrSlug),
    queryFn: () => assessmentService.getAssessment(idOrSlug),
    enabled: Boolean(idOrSlug),
  })
}

export function useCreateAssessment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CreateAssessmentPayload) =>
      assessmentService.createAssessment(payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: assessmentQueryKeys.lists() })
      toast.add({
        title: "Assessment Created",
        description: `Assessment '${data.title}' created successfully!`,
        type: "success",
      })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Creation Failed",
        description: getErrorMessage(error, "Failed to create assessment."),
        type: "error",
      })
    },
  })
}

export function useValidateAssessment() {
  return useMutation({
    mutationFn: (payload: CreateAssessmentPayload) =>
      assessmentService.validateAssessment(payload),
  })
}

export function useQuestionBankPreview(bankId?: string) {
  return useQuery({
    queryKey: ["question-bank-preview", bankId],
    queryFn: () => assessmentService.getQuestionBankPreview(bankId!),
    enabled: Boolean(bankId && bankId.trim().length > 0),
    staleTime: 1000 * 30, // 30 seconds
  })
}

export function useDeleteAssessment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (idOrSlug: string) => assessmentService.deleteAssessment(idOrSlug),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assessmentQueryKeys.lists() })
      toast.add({
        title: "Assessment Deleted",
        description: "Assessment deleted successfully.",
        type: "success",
      })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Delete Failed",
        description: getErrorMessage(error, "Failed to delete assessment."),
        type: "error",
      })
    },
  })
}

export function useCompleteAssessment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (idOrSlug: string) => assessmentService.completeAssessment(idOrSlug),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: assessmentQueryKeys.lists() })
      queryClient.invalidateQueries({
        queryKey: assessmentQueryKeys.detail(data.id),
      })
      toast.add({
        title: "Assessment Completed",
        description: `Assessment '${data.title}' has been successfully closed and marked as COMPLETED.`,
        type: "success",
      })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Completion Failed",
        description: getErrorMessage(error, "Failed to complete assessment."),
        type: "error",
      })
    },
  })
}

export function useBulkAssignCandidates(idOrSlug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: BulkAssignCandidatesPayload) =>
      assessmentService.bulkAssignCandidates(idOrSlug, payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({
        queryKey: assessmentQueryKeys.detail(idOrSlug),
      })
      queryClient.invalidateQueries({
        queryKey: assessmentQueryKeys.lists(),
      })
      toast.add({
        title: "Candidates Assigned",
        description: `Assigned ${data.successfullyAssigned} candidate(s) (${data.duplicatesSkipped} duplicates skipped).`,
        type: "success",
      })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Assignment Failed",
        description: getErrorMessage(error, "Failed to assign candidates."),
        type: "error",
      })
    },
  })
}

export function useCandidateAssignments(idOrSlug: string, page = 0, size = 20) {
  return useQuery({
    queryKey: assessmentQueryKeys.candidates(idOrSlug, page, size),
    queryFn: () => assessmentService.getCandidateAssignments(idOrSlug, page, size),
    enabled: Boolean(idOrSlug),
  })
}

export function useAssessmentAttempts(idOrSlug: string, page = 0, size = 20) {
  return useQuery({
    queryKey: assessmentQueryKeys.attempts(idOrSlug, page, size),
    queryFn: () => assessmentService.getAssessmentAttempts(idOrSlug, page, size),
    enabled: Boolean(idOrSlug),
  })
}

export function useAttemptConnectionEvents(attemptId: string, page = 0, size = 50) {
  return useQuery({
    queryKey: assessmentQueryKeys.events(attemptId, page, size),
    queryFn: () =>
      assessmentService.getAttemptConnectionEvents(attemptId, page, size),
    enabled: Boolean(attemptId),
  })
}

export function useGenerateInvitationCode(idOrSlug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => assessmentService.generateInvitationCode(idOrSlug),
    onSuccess: (data) => {
      queryClient.setQueryData(assessmentQueryKeys.detail(idOrSlug), data)
      queryClient.invalidateQueries({ queryKey: assessmentQueryKeys.lists() })
      toast.add({
        title: "Invitation Code Generated",
        description: `Code ${data.invitationCode} is now active.`,
        type: "success",
      })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Code Generation Failed",
        description: getErrorMessage(error, "Failed to generate code."),
        type: "error",
      })
    },
  })
}

export function useRevokeInvitationCode(idOrSlug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => assessmentService.revokeInvitationCode(idOrSlug),
    onSuccess: (data) => {
      queryClient.setQueryData(assessmentQueryKeys.detail(idOrSlug), data)
      queryClient.invalidateQueries({ queryKey: assessmentQueryKeys.lists() })
      toast.add({
        title: "Invitation Code Revoked",
        description: "Invitation code has been disabled.",
        type: "success",
      })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Revoke Failed",
        description: getErrorMessage(error, "Failed to revoke code."),
        type: "error",
      })
    },
  })
}

// ==========================================
// Candidate Hooks
// ==========================================

export function useJoinAssessmentByCode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: JoinByCodePayload) => assessmentService.joinByCode(payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: assessmentQueryKeys.myAssessments() })
      toast.add({
        title: "Joined Assessment",
        description: `Successfully joined ${data.title}!`,
        type: "success",
      })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Failed to Join",
        description: getErrorMessage(error, "Invalid or expired invitation code."),
        type: "error",
      })
    },
  })
}

export function useMyAssessments() {
  return useQuery({
    queryKey: assessmentQueryKeys.myAssessments(),
    queryFn: () => assessmentService.getMyAssessments(),
  })
}

export function useCandidateAssessmentSummary(
  idOrSlug: string,
  options?: { enabled?: boolean }
) {
  return useQuery({
    queryKey: assessmentQueryKeys.candidateSummary(idOrSlug),
    queryFn: () => assessmentService.getAssessmentSummary(idOrSlug),
    enabled: Boolean(idOrSlug) && (options?.enabled ?? true),
    retry: (failureCount, error: unknown) => {
      if (axios.isAxiosError(error) && (error.response?.status === 401 || error.response?.status === 403)) return false
      return failureCount < 2
    },
  })
}

export function useAssessmentReadiness(
  idOrSlug: string,
  options?: { enabled?: boolean }
) {
  const isEnabled = Boolean(idOrSlug) && (options?.enabled ?? true)
  return useQuery({
    queryKey: assessmentQueryKeys.readiness(idOrSlug),
    queryFn: () => assessmentService.getReadiness(idOrSlug),
    enabled: isEnabled,
    refetchInterval: isEnabled ? 10000 : false,
    retry: (failureCount, error: unknown) => {
      if (axios.isAxiosError(error) && (error.response?.status === 401 || error.response?.status === 403)) return false
      return failureCount < 2
    },
  })
}

export function useActiveAttempt(
  idOrSlug: string,
  options?: { enabled?: boolean }
) {
  return useQuery({
    queryKey: assessmentQueryKeys.activeAttempt(idOrSlug),
    queryFn: () => assessmentService.getActiveAttempt(idOrSlug),
    enabled: Boolean(idOrSlug) && (options?.enabled ?? true),
    refetchOnWindowFocus: false,
    retry: (failureCount, error: unknown) => {
      if (axios.isAxiosError(error) && (error.response?.status === 401 || error.response?.status === 403)) return false
      return failureCount < 2
    },
  })
}

export function useStartAttempt(idOrSlug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => assessmentService.startAttempt(idOrSlug),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: assessmentQueryKeys.candidateSummary(idOrSlug),
      })
      queryClient.invalidateQueries({
        queryKey: assessmentQueryKeys.activeAttempt(idOrSlug),
      })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Start Failed",
        description: getErrorMessage(error, "Failed to start attempt."),
        type: "error",
      })
    },
  })
}

export function useSaveMcqResponse(idOrSlug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: SaveMcqResponsePayload) =>
      assessmentService.saveMcqResponse(idOrSlug, payload),
    onSuccess: (saved) => {
      queryClient.setQueryData(
        assessmentQueryKeys.activeAttempt(idOrSlug),
        (old: CandidateAttemptDetail | undefined) => {
          if (!old) return old
          return {
            ...old,
            responses: {
              ...old.responses,
              [saved.questionId]: saved,
            },
          }
        }
      )
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Save Failed",
        description: getErrorMessage(error, "Failed to save answer."),
        type: "error",
      })
    },
  })
}

export function useRunSample(idOrSlug: string) {
  return useMutation({
    mutationFn: (payload: RunAssessmentSamplePayload) =>
      assessmentService.runSample(idOrSlug, payload),
  })
}

export function useRunCustomInput(idOrSlug: string) {
  return useMutation({
    mutationFn: (payload: RunAssessmentCustomInputPayload) =>
      assessmentService.runCustomInput(idOrSlug, payload),
  })
}

export function useMarkQuestionForReview(idOrSlug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: MarkForReviewPayload) =>
      assessmentService.markForReview(idOrSlug, payload),
    onSuccess: (saved) => {
      queryClient.setQueryData(
        assessmentQueryKeys.activeAttempt(idOrSlug),
        (old: CandidateAttemptDetail | undefined) => {
          if (!old) return old
          return {
            ...old,
            responses: {
              ...old.responses,
              [saved.questionId]: {
                ...old.responses?.[saved.questionId],
                markedForReview: saved.markedForReview,
              },
            },
          }
        }
      )
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Update Failed",
        description: getErrorMessage(error, "Failed to mark question."),
        type: "error",
      })
    },
  })
}

export function useSaveCodeResponse(idOrSlug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: SaveCodeResponsePayload) =>
      assessmentService.saveCodeResponse(idOrSlug, payload),
    onSuccess: (saved) => {
      queryClient.setQueryData(
        assessmentQueryKeys.activeAttempt(idOrSlug),
        (old: CandidateAttemptDetail | undefined) => {
          if (!old) return old
          return {
            ...old,
            responses: {
              ...old.responses,
              [saved.questionId]: saved,
            },
          }
        }
      )
    },
  })
}

export function useSubmitCodeResponse(idOrSlug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: SubmitCodeResponsePayload) =>
      assessmentService.submitCodeResponse(idOrSlug, payload),
    onSuccess: (saved) => {
      queryClient.setQueryData(
        assessmentQueryKeys.activeAttempt(idOrSlug),
        (old: CandidateAttemptDetail | undefined) => {
          if (!old) return old
          return {
            ...old,
            responses: {
              ...old.responses,
              [saved.questionId]: saved,
            },
          }
        }
      )
      toast.add({
        title: "Code Evaluated",
        description: "Code evaluated and submitted successfully.",
        type: "success",
      })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Evaluation Failed",
        description: getErrorMessage(error, "Code submission failed."),
        type: "error",
      })
    },
  })
}

export function useFinalSubmitAssessment(idOrSlug: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => assessmentService.finalSubmit(idOrSlug),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: assessmentQueryKeys.candidateSummary(idOrSlug),
      })
      queryClient.invalidateQueries({
        queryKey: assessmentQueryKeys.activeAttempt(idOrSlug),
      })
      queryClient.invalidateQueries({
        queryKey: assessmentQueryKeys.candidateResults(idOrSlug),
      })
      queryClient.invalidateQueries({
        queryKey: assessmentQueryKeys.myAssessments(),
      })
      toast.add({
        title: "Assessment Submitted",
        description: "Assessment submitted successfully!",
        type: "success",
      })
    },
    onError: (error: unknown) => {
      toast.add({
        title: "Submission Failed",
        description: getErrorMessage(error, "Final submission failed."),
        type: "error",
      })
    },
  })
}

export function useCandidateResults(idOrSlug: string, enabled = true) {
  return useQuery({
    queryKey: assessmentQueryKeys.candidateResults(idOrSlug),
    queryFn: () => assessmentService.getCandidateResults(idOrSlug),
    enabled: Boolean(idOrSlug) && enabled,
  })
}

export function useRecordTelemetry(idOrSlug: string) {
  return useMutation({
    mutationFn: (events: ClientTelemetryEventDto[]) =>
      assessmentService.recordTelemetry(idOrSlug, events),
  })
}

export function useAssessmentMetrics(idOrSlug: string) {
  return useQuery({
    queryKey: assessmentQueryKeys.metrics(idOrSlug),
    queryFn: () => assessmentService.getAssessmentMetrics(idOrSlug),
    enabled: Boolean(idOrSlug),
  })
}

export function useAttemptDetailForAdmin(attemptId: string) {
  return useQuery({
    queryKey: assessmentQueryKeys.attemptDetail(attemptId),
    queryFn: () => assessmentService.getAttemptDetailForAdmin(attemptId),
    enabled: Boolean(attemptId),
  })
}

export function useAttemptAuditTrail(attemptId: string) {
  return useQuery({
    queryKey: assessmentQueryKeys.attemptAudit(attemptId),
    queryFn: () => assessmentService.getAttemptAuditTrail(attemptId),
    enabled: Boolean(attemptId),
  })
}
