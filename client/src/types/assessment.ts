import type { BankQuestionType } from "./question-bank"

export type AssessmentStatus = "NOT_STARTED" | "ACTIVE" | "COMPLETED"

export type AssessmentSelectionMode = "QUESTION_BANK" | "INDIVIDUAL_QUESTIONS"

export type CandidateParticipationStatus =
  | "INVITED"
  | "IN_PROGRESS"
  | "SUBMITTED"
  | "EXPIRED"
  | "CANCELLED"

export type AttemptStatus =
  | "IN_PROGRESS"
  | "SUBMITTED"
  | "EXPIRED"
  | "CANCELLED"

export type ConnectionEventType =
  | "CONNECTED"
  | "HEARTBEAT"
  | "DISCONNECTED"
  | "STALE_REPLACED"
  | "RECONNECTED"

export interface OptionSnapshot {
  id: string
  content: string
  order: number
  isCorrect?: boolean
}

export interface AssessmentItemResponse {
  questionId: string
  questionType: BankQuestionType
  order: number
  points: number
  title: string
  mcqContent?: string
  mcqType?: "SINGLE_CHOICE" | "MULTIPLE_CHOICE"
  options?: OptionSnapshot[]
  correctOptionIds?: string[]
  codingStatement?: string
  codingProblemType?: "GENERIC" | "DATABASE"
  supportedLanguageIds?: string[]
  starterCode?: Record<string, string>
  sampleTestCases?: Array<{
    id?: string
    stdin?: string
    dbInit?: string
    output?: string
  }>
}

export interface AssessmentSummaryResponse {
  id: string
  slug: string
  title: string
  description?: string
  status: AssessmentStatus
  selectionMode: AssessmentSelectionMode
  scheduledStart?: string
  scheduledEnd?: string
  durationMinutes: number
  maxAttempts?: number
  passingScore?: number
  totalScore: number
  totalQuestions: number
  mcqCount: number
  codingCount: number
  assignedCandidateCount: number
  invitationCode?: string
  invitationCodeEnabled?: boolean
  invitationCodeExpiresAt?: string
  invitationCodeMaxUses?: number
  invitationCodeUsedCount?: number
  authorId?: string
  createdAt: string
  updatedAt?: string
}

export interface AssessmentDetailResponse extends AssessmentSummaryResponse {
  questionBankId?: string
  items: AssessmentItemResponse[]
}

export interface AssessmentCustomItemPayload {
  questionId: string
  questionType: BankQuestionType
  order: number
  points?: number
}

export interface CreateAssessmentPayload {
  title: string
  description?: string
  selectionMode: AssessmentSelectionMode
  questionBankId?: string
  customQuestionItems?: AssessmentCustomItemPayload[]
  candidateEmailsRaw?: string
  candidateEmails?: string[]
  scheduledStart?: string
  scheduledEnd?: string
  durationMinutes: number
  maxAttempts?: number
  passingScore?: number
  enableInvitationCode?: boolean
  invitationCodeExpiresAt?: string
  invitationCodeMaxUses?: number
}

export interface AssessmentValidationError {
  field: string
  message: string
  code: string
  questionId?: string
}

export interface QuestionBankPreviewItemDto {
  questionId: string
  questionType: BankQuestionType
  title: string
  difficulty: string
  points: number
  topics: string[]
  supportedLanguages?: string[]
  eligible: boolean
  ineligibilityReason?: string
}

export interface QuestionBankPreviewDto {
  id: string
  slug: string
  title: string
  description?: string
  category?: string
  status: string
  totalQuestions: number
  mcqCount: number
  codingCount: number
  totalPoints: number
  difficultyDistribution: Record<string, number>
  supportedCodingLanguages: string[]
  items: QuestionBankPreviewItemDto[]
  eligibleForAssessment: boolean
  ineligibilityReason?: string
}

export interface AssessmentValidationResponse {
  valid: boolean
  errors: AssessmentValidationError[]
  warnings: string[]
  bankPreview?: QuestionBankPreviewDto
  calculatedTotalScore?: number
  calculatedTotalQuestions?: number
  calculatedDurationMinutes?: number
  candidateCount?: number
}

export interface BulkAssignCandidatesPayload {
  emailsRaw?: string
  emails?: string[]
}

export interface BulkAssignCandidatesResponse {
  totalProcessed: number
  successfullyAssigned: number
  duplicatesSkipped: number
  invalidEmails: string[]
  assignedEmails: string[]
}

export interface CandidateAssessmentSummary {
  assessmentId: string
  slug: string
  title: string
  description?: string
  assessmentStatus: AssessmentStatus
  scheduledStart?: string
  scheduledEnd?: string
  durationMinutes: number
  maxAttempts?: number
  attemptsUsed: number
  assignmentStatus: CandidateParticipationStatus
  canStart: boolean
  cannotStartReason?: string
  activeAttemptId?: string
  totalQuestions: number
  totalScore: number
}

export interface StartAttemptResponse {
  attemptId: string
  assessmentId: string
  attemptNumber: number
  status: AttemptStatus
  startedAt: string
  effectiveDeadline: string
  remainingSeconds: number
}

export interface SavedQuestionResponse {
  questionId: string
  questionType: BankQuestionType
  selectedOptionIds?: string[]
  languageId?: string
  code?: string
  lastExecutionResult?: CodingProblemExecutionResponse
  allTestcasesPassed?: boolean
  markedForReview?: boolean
  pointsAwarded?: number
  savedAt?: string
}

export interface JoinByCodePayload {
  code: string
}

export interface MarkForReviewPayload {
  questionId: string
  markedForReview: boolean
}

export interface RunAssessmentCustomInputPayload {
  questionId: string
  languageId: string
  code: string
  stdin?: string
}

export interface AssessmentReadinessResponse {
  authenticated: boolean
  candidateUserId?: string
  candidateEmail?: string
  assessmentId: string
  slug: string
  title: string
  description?: string
  assessmentStatus: AssessmentStatus
  scheduledStart?: string
  scheduledEnd?: string
  durationMinutes: number
  maxAttempts?: number
  attemptsUsed: number
  participationStatus: CandidateParticipationStatus
  canStart: boolean
  cannotStartReason?: string
  activeAttemptId?: string
  hasActiveAttempt: boolean
  effectiveDeadline?: string
  remainingSeconds?: number
  totalQuestions: number
  totalScore: number
  backendReady: boolean
  serverTimestamp: string
}

export interface CandidateAttemptDetail {
  attemptId: string
  assessmentId: string
  slug: string
  title: string
  description?: string
  attemptNumber: number
  status: AttemptStatus
  startedAt: string
  effectiveDeadline: string
  remainingSeconds: number
  totalQuestions: number
  totalScore: number
  questions: AssessmentItemResponse[]
  responses: Record<string, SavedQuestionResponse>
}

export interface SaveMcqResponsePayload {
  questionId: string
  selectedOptionIds: string[]
}

export interface SaveCodeResponsePayload {
  questionId: string
  languageId: string
  code: string
}

export interface SubmitCodeResponsePayload {
  questionId: string
  languageId: string
  code: string
}

export interface RunAssessmentSamplePayload {
  questionId: string
  languageId: string
  code: string
  stdin?: string
}

export interface FinalSubmitResponse {
  attemptId: string
  assessmentId: string
  status: AttemptStatus
  submittedAt: string
  score: number
  maxScore: number
  passed?: boolean
  message: string
}

export interface CandidateAssignmentReport {
  id: string
  assessmentId: string
  candidateEmail: string
  candidateUserId?: string
  status: CandidateParticipationStatus
  attemptsCount: number
  lastAttemptId?: string
  invitedAt?: string
  startedAt?: string
  completedAt?: string
  latestScore?: number
}

export interface AttemptReport {
  id: string
  assessmentId: string
  candidateUserId: string
  candidateEmail: string
  attemptNumber: number
  status: AttemptStatus
  startedAt: string
  effectiveDeadline: string
  submittedAt?: string
  score?: number
  maxPossibleScore?: number
  passed?: boolean
  integrityFlagsCount: number
}

export interface SampleTestcaseResult {
  testCaseId?: string
  status: string
  stdout?: string
  stderr?: string
  exitCode?: number
  runtimeMs?: number
  executionTimeMs?: number
  memoryMb?: number
  expectedOutput?: string
  actualOutput?: string
  passed: boolean
}

export interface CodingProblemExecutionResponse {
  overallStatus: string
  totalExecutionTimeMs: number
  totalTestcases: number
  passedTestcases: number
  failedTestcases: number
  logs?: string
  results: SampleTestcaseResult[]
}

export interface AssessmentConnectionEvent {
  id: string
  assessmentId: string
  attemptId: string
  candidateUserId: string
  connectionId: string
  eventType: ConnectionEventType
  isIntegrityFlag: boolean
  reason?: string
  clientIp?: string
  userAgent?: string
  timestamp: string
}

export type AssessmentAuditEventType =
  | "SESSION_STARTED"
  | "NAVIGATE_QUESTION"
  | "MCQ_ANSWER_CHANGED"
  | "CODE_SAVED"
  | "CODE_RUN_SAMPLE"
  | "CODE_RUN_CUSTOM"
  | "CODE_SUBMITTED"
  | "MARK_REVIEW_TOGGLED"
  | "WINDOW_BLUR"
  | "WINDOW_FOCUS"
  | "CONNECTION_INTERRUPTED"
  | "CONNECTION_RESTORED"
  | "STALE_SESSION_REPLACED"
  | "FINAL_SUBMISSION"

export type AssessmentAuditEventSource =
  | "SERVER_VERIFIED"
  | "CLIENT_TELEMETRY"
  | "SYSTEM_PRESENCE"

export interface AssessmentAuditEvent {
  id: string
  assessmentId: string
  attemptId: string
  candidateUserId: string
  candidateEmail?: string
  questionId?: string
  eventType: AssessmentAuditEventType
  source: AssessmentAuditEventSource
  clientIp?: string
  userAgent?: string
  metadata?: Record<string, unknown>
  timestamp: string
  createdAt: string
}

export interface ClientTelemetryEventDto {
  eventType: AssessmentAuditEventType
  questionId?: string
  metadata?: Record<string, unknown>
  clientTimestamp?: string
}

export interface QuestionResultSummary {
  questionId: string
  questionIndex: number
  title: string
  questionType: BankQuestionType
  pointsAwarded: number
  maxPoints: number
  isAnswered: boolean
  isCorrect?: boolean | null
  markedForReview: boolean

  mcqStatement?: string
  codingStatement?: string

  selectedOptionIds?: string[]
  options?: OptionSnapshot[]

  languageId?: string
  code?: string
  lastExecutionResult?: CodingProblemExecutionResponse
  allTestcasesPassed?: boolean | null
}

export interface CandidateAssessmentResultResponse {
  assessmentId: string
  title: string
  description?: string

  attemptId: string
  attemptNumber: number
  status: AttemptStatus

  startedAt: string
  submittedAt?: string
  timeSpentSeconds: number

  score: number
  maxPossibleScore: number
  percentage: number
  passingScore?: number
  passed?: boolean

  accuracyPercentage: number

  totalQuestions: number
  answeredCount: number
  unansweredCount: number
  markedForReviewCount: number

  mcqCount: number
  codingCount: number

  resultsVisible: boolean

  questionResults: QuestionResultSummary[]
}

export interface QuestionMetric {
  questionId: string
  index: number
  title: string
  questionType: BankQuestionType
  maxPoints: number
  answeredCount: number
  correctCount: number
  averageScore: number
  successRate: number
}

export interface AssessmentMetricsResponse {
  assessmentId: string
  assessmentTitle: string

  totalCandidatesAssigned: number
  invitedCount: number
  inProgressCount: number
  submittedCount: number
  expiredCount: number

  completionRate: number
  passRate: number

  averageScore: number
  medianScore: number
  highestScore: number
  lowestScore: number
  maxPossibleScore: number

  averageTimeSpentMinutes: number

  totalIntegrityFlags: number
  totalCodeSubmissions: number
  codingSuccessRate: number

  questionMetrics: QuestionMetric[]
}



