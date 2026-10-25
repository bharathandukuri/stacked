import {
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  Sparkles,
  Loader2,
  Lock,
} from "lucide-react"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import type {
  AssessmentSelectionMode,
  AssessmentValidationResponse,
  QuestionBankPreviewDto,
} from "@/types/assessment"
import type { QuestionBankSummary } from "@/types/question-bank"
import type { CuratedQuestionItem } from "./step-question-selection"

interface StepReviewValidationProps {
  title: string
  durationMinutes: number
  scheduledStart: string
  scheduledEnd: string
  maxAttempts: number | undefined
  passingScore: number | undefined
  selectionMode: AssessmentSelectionMode
  selectedBank: QuestionBankSummary | undefined
  bankPreview: QuestionBankPreviewDto | undefined
  curatedQuestions: CuratedQuestionItem[]
  parsedCandidateEmails: string[]
  enableInvitationCode: boolean
  totalCalculatedQuestions: number
  totalCalculatedPoints: number
  mcqCount: number
  codingCount: number
  isValidating: boolean
  validationResult: AssessmentValidationResponse | null
  onRevalidate: () => void
  onFixFieldInStep: (stepNumber: number) => void
}

export function StepReviewValidation({
  title,
  durationMinutes,
  scheduledStart,
  scheduledEnd,
  maxAttempts,
  passingScore,
  selectionMode,
  selectedBank,
  bankPreview,
  curatedQuestions,
  parsedCandidateEmails,
  enableInvitationCode,
  totalCalculatedQuestions,
  totalCalculatedPoints,
  mcqCount,
  codingCount,
  isValidating,
  validationResult,
  onRevalidate,
  onFixFieldInStep,
}: StepReviewValidationProps) {
  const getStepForField = (field: string): number => {
    switch (field) {
      case "title":
      case "durationMinutes":
      case "scheduledStart":
      case "scheduledEnd":
      case "passingScore":
      case "maxAttempts":
        return 1
      case "questionBankId":
      case "items":
      case "selectionMode":
        return 2
      case "candidateEmails":
      case "invitationCodeExpiresAt":
      case "invitationCodeMaxUses":
        return 3
      default:
        return 1
    }
  }

  return (
    <div className="space-y-6">
      {/* Permanent Immutability Warning Banner */}
      <div className="flex items-start gap-3 p-4 rounded-xl border border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400">
        <Lock className="size-5 shrink-0 mt-0.5 text-amber-600 dark:text-amber-400" />
        <div className="space-y-1">
          <div className="font-semibold text-sm">Assessment Immutability Notice</div>
          <p className="text-xs text-amber-700/90 dark:text-amber-400/90 leading-relaxed">
            Once created, Stacked assessments are <strong>permanently immutable</strong>. The definition, timing, question bank association, and question snapshots cannot be modified. If configuration changes are needed later, a new assessment must be created.
          </p>
        </div>
      </div>

      {/* Pre-Creation Backend Validation Status */}
      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-lg flex items-center gap-2">
              <CheckCircle2 className="size-5 text-primary" />
              Pre-Creation Validation Engine
            </CardTitle>
            <Button
              variant="outline"
              size="sm"
              onClick={onRevalidate}
              disabled={isValidating}
              className="text-xs h-7 gap-1"
            >
              {isValidating ? (
                <Loader2 className="size-3 animate-spin" />
              ) : (
                <Sparkles className="size-3" />
              )}
              <span>Re-validate</span>
            </Button>
          </div>
          <CardDescription>
            All assessment fields, question contents, code execution constraints, and candidate parameters are validated before saving.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {isValidating ? (
            <div className="py-6 flex flex-col items-center justify-center gap-2 text-muted-foreground text-xs">
              <Loader2 className="size-5 animate-spin text-primary" />
              <span>Running comprehensive backend validation checks...</span>
            </div>
          ) : validationResult ? (
            validationResult.valid ? (
              <Alert className="border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400">
                <CheckCircle2 className="size-4 text-emerald-600 dark:text-emerald-400" />
                <AlertTitle>All Validation Checks Passed</AlertTitle>
                <AlertDescription className="text-xs">
                  Configuration is complete. Question bank questions and judging rules are verified and ready for atomic creation.
                </AlertDescription>
              </Alert>
            ) : (
              <div className="space-y-3">
                <Alert variant="destructive">
                  <AlertCircle className="size-4" />
                  <AlertTitle>
                    Validation Errors Detected ({validationResult.errors.length})
                  </AlertTitle>
                  <AlertDescription className="text-xs">
                    The assessment cannot be created until the following blocking configuration issues are resolved:
                  </AlertDescription>
                </Alert>

                <div className="divide-y divide-border rounded-lg border bg-destructive/5 overflow-hidden">
                  {validationResult.errors.map((err, idx) => (
                    <div
                      key={idx}
                      className="p-3 flex items-center justify-between text-xs gap-3"
                    >
                      <div className="flex items-center gap-2 min-w-0">
                        <Badge
                          variant="destructive"
                          className="font-mono text-[10px] shrink-0"
                        >
                          {err.field}
                        </Badge>
                        <span className="text-destructive font-medium truncate">
                          {err.message}
                        </span>
                      </div>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => onFixFieldInStep(getStepForField(err.field))}
                        className="h-6 text-[11px] px-2 shrink-0 border-destructive/30 hover:bg-destructive/10"
                      >
                        Fix in Step {getStepForField(err.field)}
                      </Button>
                    </div>
                  ))}
                </div>
              </div>
            )
          ) : null}

          {/* Warnings */}
          {validationResult?.warnings && validationResult.warnings.length > 0 && (
            <div className="space-y-2">
              {validationResult.warnings.map((warn, i) => (
                <div
                  key={i}
                  className="flex items-center gap-2 p-2.5 rounded-lg border border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400 text-xs"
                >
                  <AlertTriangle className="size-3.5 shrink-0" />
                  <span>{warn}</span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Assessment Configuration Review Cards */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Assessment Configuration Review</CardTitle>
          <CardDescription>
            Summary of configuration that will be locked into the assessment record.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Summary Metric Strip */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 bg-muted/40 rounded-xl border text-center">
            <div>
              <div className="text-xs text-muted-foreground">Duration</div>
              <div className="text-lg font-bold text-foreground mt-0.5">
                {durationMinutes} mins
              </div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Total Questions</div>
              <div className="text-lg font-bold text-foreground mt-0.5">
                {totalCalculatedQuestions}
              </div>
              <div className="text-[11px] text-muted-foreground">
                {mcqCount} MCQ • {codingCount} Code
              </div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Total Points</div>
              <div className="text-lg font-bold text-foreground mt-0.5">
                {totalCalculatedPoints} pts
              </div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Invited Candidates</div>
              <div className="text-lg font-bold text-primary mt-0.5">
                {parsedCandidateEmails.length}
              </div>
            </div>
          </div>

          {/* Detailed Spec List */}
          <div className="space-y-2.5 text-xs">
            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Title</span>
              <span className="font-semibold text-foreground text-sm">{title}</span>
            </div>

            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Question Bank Source</span>
              <span className="font-medium text-foreground">
                {selectionMode === "QUESTION_BANK"
                  ? `${bankPreview?.title || selectedBank?.title || "Question Bank"} (${totalCalculatedQuestions} questions)`
                  : `Custom Selection (${curatedQuestions.length} questions)`}
              </span>
            </div>

            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Schedule Window</span>
              <span className="font-medium text-foreground">
                {scheduledStart
                  ? new Date(scheduledStart).toLocaleString()
                  : "Immediately Open"}{" "}
                &rarr;{" "}
                {scheduledEnd
                  ? new Date(scheduledEnd).toLocaleString()
                  : "No Deadline"}
              </span>
            </div>

            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Attempt Limits</span>
              <span className="font-medium text-foreground">
                {maxAttempts ? `${maxAttempts} attempt(s)` : "Unlimited Attempts"}
              </span>
            </div>

            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Passing Benchmark</span>
              <span className="font-medium text-foreground">
                {passingScore
                  ? `${passingScore} / ${totalCalculatedPoints} points (${Math.round((passingScore / totalCalculatedPoints) * 100)}%)`
                  : "Not Configured"}
              </span>
            </div>

            <div className="flex justify-between py-2 border-b">
              <span className="text-muted-foreground">Self-Join Passcode</span>
              <span className="font-medium">
                {enableInvitationCode ? (
                  <span className="text-emerald-600 dark:text-emerald-400 font-semibold">
                    Enabled (Auto-generated on creation)
                  </span>
                ) : (
                  <span className="text-muted-foreground">Disabled</span>
                )}
              </span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
