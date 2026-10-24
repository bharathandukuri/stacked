export type { PageResponse } from "@/types/api"

export type AnalyticsEventType =
  | "CODING_PROBLEM_CREATED"
  | "CODING_PROBLEM_UPDATED"
  | "CODING_PROBLEM_PUBLISHED"
  | "CODING_PROBLEM_UNPUBLISHED"
  | "CODING_PROBLEM_DELETED"
  | "MCQ_CREATED"
  | "MCQ_UPDATED"
  | "MCQ_PUBLISHED"
  | "MCQ_UNPUBLISHED"
  | "MCQ_DELETED"
  | "QUESTION_BANK_CREATED"
  | "QUESTION_BANK_UPDATED"
  | "QUESTION_BANK_PUBLISHED"
  | "QUESTION_BANK_UNPUBLISHED"
  | "QUESTION_BANK_DELETED"
  | "ASSESSMENT_CREATED"
  | "ASSESSMENT_ACTIVATED"
  | "ASSESSMENT_COMPLETED"
  | "ASSESSMENT_DELETED"
  | "CANDIDATE_INVITED"
  | "CANDIDATE_ENROLLED"
  | "ATTEMPT_STARTED"
  | "ANSWER_SAVED"
  | "ATTEMPT_SUBMITTED"
  | "ATTEMPT_EXPIRED"
  | "CODE_SUBMISSION_EVALUATED"
  | "SAMPLE_RUN_EXECUTED"

export type AnalyticsEntityType =
  | "CODING_PROBLEM"
  | "MCQ"
  | "QUESTION_BANK"
  | "ASSESSMENT"
  | "ASSESSMENT_ASSIGNMENT"
  | "ASSESSMENT_ATTEMPT"
  | "QUESTION_RESPONSE"
  | "ATTEMPT"
  | "SUBMISSION"
  | "USER"
  | "SYSTEM"

export type AnalyticsActorType = "ADMIN" | "CANDIDATE" | "SYSTEM"

export interface AnalyticsTrendPoint {
  date: string
  metricKey: string
  count: number
  value: number
}

export interface PlatformOverviewResponse {
  totalCodingProblems: number
  totalMcqs: number
  totalQuestionBanks: number
  totalAssessments: number
  activeAssessments: number
  completedAssessments: number
  scheduledAssessments: number
  totalCandidates: number
  totalEnrollments: number
  totalAttemptsStarted: number
  totalAttemptsSubmitted: number
  overallCompletionRate: number
  avgAttemptDurationMinutes: number
}

export interface ContentAnalyticsResponse {
  codingProblemsPublished: number
  codingProblemsDraft: number
  mcqsPublished: number
  mcqsDraft: number
  questionBanksPublished: number
  questionBanksDraft: number
  creationTrends: AnalyticsTrendPoint[]
}

export interface AssessmentAnalyticsResponse {
  assessmentsCreated: number
  assessmentsActivated: number
  assessmentsCompleted: number
  manualActivations: number
  autoActivations: number
  manualCompletions: number
  autoCompletions: number
  candidatesInvited: number
  attemptsStarted: number
  attemptsSubmitted: number
  attemptsExpired: number
  participationRate: number
  completionRate: number
  averageAttemptDurationMinutes: number
  trends: AnalyticsTrendPoint[]
}

export interface SubmissionAnalyticsResponse {
  totalSubmissions: number
  totalInteractiveRuns: number
  acceptedSubmissions: number
  rejectedSubmissions: number
  passRate: number
  avgExecutionTimeMs: number
  verdictDistribution: Record<string, number>
  languageDistribution: Record<string, number>
  trends: AnalyticsTrendPoint[]
}

export interface OperationalAnalyticsResponse {
  activeExecutionWorkers: number
  currentQueueDepth: number
  maxQueueCapacity: number
  totalEventsLogged: number
  systemStatus: string
}

export interface ActivityTimelineItemResponse {
  id: string
  eventType: AnalyticsEventType
  entityType: AnalyticsEntityType
  entityId: string
  actorType: AnalyticsActorType
  actorId?: string
  assessmentId?: string
  candidateEmail?: string
  timestamp: string
  attributes?: Record<string, unknown>
}

export interface AnalyticsDateRangeParams {
  startDate?: string // YYYY-MM-DD
  endDate?: string // YYYY-MM-DD
}

export interface TimelineFilterParams {
  eventType?: string
  page?: number
  size?: number
}
