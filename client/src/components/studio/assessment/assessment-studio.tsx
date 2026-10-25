import { useState, useMemo, useEffect, useCallback, useRef } from "react"
import axios from "axios"
import { useNavigate } from "@tanstack/react-router"
import {
  Clock,
  Users,
  CheckCircle2,
  Layers,
  ArrowRight,
  ArrowLeft,
  Loader2,
  ChevronRight,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  QuestionPickerDialog,
  type SelectedQuestionItem,
} from "@/components/studio/question-bank/question-picker-dialog"
import { useQuestionBanks } from "@/hooks/use-question-banks"
import {
  useCreateAssessment,
  useValidateAssessment,
  useQuestionBankPreview,
} from "@/hooks/use-assessments"
import { toast } from "@/components/ui/toast"
import { cn } from "@/lib/utils"
import type {
  AssessmentSelectionMode,
  AssessmentValidationResponse,
  CreateAssessmentPayload,
} from "@/types/assessment"
import type { BankQuestionType } from "@/types/question-bank"

import { StepBasicInfo } from "./steps/step-basic-info"
import { StepQuestionSelection } from "./steps/step-question-selection"
import { StepCandidateInvitations } from "./steps/step-candidate-invitations"
import { StepReviewValidation } from "./steps/step-review-validation"

interface CuratedQuestionItem {
  questionId: string
  questionType: BankQuestionType
  title: string
  slug?: string
  difficulty?: string
  points: number
  order: number
}

const EMAIL_REGEX = /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g

export function AssessmentStudio() {
  const navigate = useNavigate()
  const [currentStep, setCurrentStep] = useState(1)

  // Step 1: Basics & Schedule
  const [title, setTitle] = useState("")
  const [description, setDescription] = useState("")
  const [durationMinutes, setDurationMinutes] = useState(60)
  const [scheduledStart, setScheduledStart] = useState("")
  const [scheduledEnd, setScheduledEnd] = useState("")
  const [maxAttempts, setMaxAttempts] = useState<number | undefined>(undefined)
  const [passingScore, setPassingScore] = useState<number | undefined>(undefined)

  // Step 2: Question Selection
  const [selectionMode, setSelectionMode] = useState<AssessmentSelectionMode>("QUESTION_BANK")
  const [selectedQuestionBankId, setSelectedQuestionBankId] = useState("")
  const [curatedQuestions, setCuratedQuestions] = useState<CuratedQuestionItem[]>([])
  const [pickerOpen, setPickerOpen] = useState(false)

  // Step 3: Candidate Access
  const [candidateEmailsRaw, setCandidateEmailsRaw] = useState("")
  const [enableInvitationCode, setEnableInvitationCode] = useState(true)
  const [invitationCodeExpiresAt, setInvitationCodeExpiresAt] = useState("")
  const [invitationCodeMaxUses, setInvitationCodeMaxUses] = useState<number | undefined>(undefined)

  // Fetch published question banks for selection
  const { data: publishedBanksData, isLoading: loadingBanks } = useQuestionBanks({
    status: "PUBLISHED",
    size: 50,
  })
  const publishedBanks = useMemo(
    () => publishedBanksData?.items ?? [],
    [publishedBanksData?.items]
  )

  // Load preview of selected bank
  const {
    data: bankPreview,
    isLoading: loadingBankPreview,
  } = useQuestionBankPreview(selectedQuestionBankId)

  const createMutation = useCreateAssessment()
  const { mutate: validateAssessment, isPending: isValidating } = useValidateAssessment()

  // Track validation response for Step 4
  const [validationResult, setValidationResult] = useState<AssessmentValidationResponse | null>(null)

  // Extract unique emails
  const parsedCandidateEmails = useMemo(() => {
    if (!candidateEmailsRaw) return []
    const matches = candidateEmailsRaw.match(EMAIL_REGEX) || []
    return Array.from(new Set(matches.map((e) => e.toLowerCase().trim())))
  }, [candidateEmailsRaw])

  const selectedBank = useMemo(() => {
    return publishedBanks.find((b) => b.id === selectedQuestionBankId)
  }, [publishedBanks, selectedQuestionBankId])

  const totalCalculatedPoints = useMemo(() => {
    if (selectionMode === "QUESTION_BANK") {
      return bankPreview?.totalPoints ?? selectedBank?.totalPoints ?? 0
    }
    return curatedQuestions.reduce((acc, q) => acc + q.points, 0)
  }, [selectionMode, selectedBank, bankPreview, curatedQuestions])

  const totalCalculatedQuestions = useMemo(() => {
    if (selectionMode === "QUESTION_BANK") {
      return bankPreview?.totalQuestions ?? selectedBank?.totalQuestions ?? 0
    }
    return curatedQuestions.length
  }, [selectionMode, selectedBank, bankPreview, curatedQuestions])

  const mcqCount = useMemo(() => {
    if (selectionMode === "QUESTION_BANK") {
      return bankPreview?.mcqCount ?? selectedBank?.mcqCount ?? 0
    }
    return curatedQuestions.filter((q) => q.questionType === "MCQ").length
  }, [selectionMode, selectedBank, bankPreview, curatedQuestions])

  const codingCount = useMemo(() => {
    if (selectionMode === "QUESTION_BANK") {
      return bankPreview?.codingCount ?? selectedBank?.codingCount ?? 0
    }
    return curatedQuestions.filter((q) => q.questionType === "CODING").length
  }, [selectionMode, selectedBank, bankPreview, curatedQuestions])

  // Build current assessment payload
  const buildPayload = useCallback((): CreateAssessmentPayload => {
    return {
      title: title.trim(),
      description: description.trim() || undefined,
      selectionMode,
      questionBankId: selectionMode === "QUESTION_BANK" ? selectedQuestionBankId : undefined,
      customQuestionItems:
        selectionMode === "INDIVIDUAL_QUESTIONS"
          ? curatedQuestions.map((q, idx) => ({
              questionId: q.questionId,
              questionType: q.questionType,
              order: idx + 1,
              points: q.points,
            }))
          : undefined,
      candidateEmailsRaw: candidateEmailsRaw || undefined,
      candidateEmails: parsedCandidateEmails,
      scheduledStart: scheduledStart ? new Date(scheduledStart).toISOString() : undefined,
      scheduledEnd: scheduledEnd ? new Date(scheduledEnd).toISOString() : undefined,
      durationMinutes: Number(durationMinutes) || 60,
      maxAttempts: maxAttempts ? Number(maxAttempts) : undefined,
      passingScore: passingScore ? Number(passingScore) : undefined,
      enableInvitationCode,
      invitationCodeExpiresAt: invitationCodeExpiresAt
        ? new Date(invitationCodeExpiresAt).toISOString()
        : undefined,
      invitationCodeMaxUses: invitationCodeMaxUses ? Number(invitationCodeMaxUses) : undefined,
    }
  }, [
    title,
    description,
    selectionMode,
    selectedQuestionBankId,
    curatedQuestions,
    candidateEmailsRaw,
    parsedCandidateEmails,
    scheduledStart,
    scheduledEnd,
    durationMinutes,
    maxAttempts,
    passingScore,
    enableInvitationCode,
    invitationCodeExpiresAt,
    invitationCodeMaxUses,
  ])

  // Keep latest buildPayload reference for validation without re-triggering effects
  const buildPayloadRef = useRef(buildPayload)
  useEffect(() => {
    buildPayloadRef.current = buildPayload
  })

  // Run pre-creation validation when entering Step 4
  const runPreCreationValidation = useCallback(() => {
    const payload = buildPayloadRef.current()
    validateAssessment(payload, {
      onSuccess: (data) => {
        setValidationResult(data)
      },
      onError: (err: unknown) => {
        let message = "Failed to validate assessment configuration."
        if (axios.isAxiosError(err)) {
          const data = err.response?.data as { message?: string } | undefined
          if (data?.message) message = data.message
        } else if (err instanceof Error) {
          message = err.message
        }
        toast.add({
          title: "Validation Check Failed",
          description: message,
          type: "error",
        })
      },
    })
  }, [validateAssessment])

  useEffect(() => {
    if (currentStep === 4) {
      runPreCreationValidation()
    }
  }, [currentStep, runPreCreationValidation])

  const handleAddQuestionsFromPicker = (selected: SelectedQuestionItem[]) => {
    const newItems: CuratedQuestionItem[] = selected.map((item, idx) => ({
      questionId: item.questionId,
      questionType: item.questionType,
      title: item.title,
      slug: item.slug,
      difficulty: item.difficulty,
      points: item.points ?? item.defaultPoints ?? (item.questionType === "CODING" ? 10 : 1),
      order: curatedQuestions.length + idx + 1,
    }))
    setCuratedQuestions((prev) => [...prev, ...newItems])
  }

  const handleRemoveQuestion = (questionId: string) => {
    setCuratedQuestions((prev) => prev.filter((q) => q.questionId !== questionId))
  }

  const handleUpdatePoints = (questionId: string, pts: number) => {
    setCuratedQuestions((prev) =>
      prev.map((q) => (q.questionId === questionId ? { ...q, points: Math.max(1, pts) } : q))
    )
  }

  const handleSubmit = () => {
    const payload = buildPayload()

    createMutation.mutate(payload, {
      onSuccess: (created) => {
        toast.add({
          title: "Assessment Created",
          description: `Assessment '${created.title}' has been successfully created and locked.`,
          type: "success",
        })
        navigate({
          to: "/admin/assessments/$assessmentId",
          params: { assessmentId: created.id },
        })
      },
      onError: (err: unknown) => {
        let errorMsg = "Failed to create assessment."
        if (axios.isAxiosError(err)) {
          const data = err.response?.data as { message?: string } | undefined
          if (data?.message) errorMsg = data.message
        } else if (err instanceof Error) {
          errorMsg = err.message
        }
        toast.add({
          title: "Creation Rejected",
          description: errorMsg,
          type: "error",
        })
      },
    })
  }

  const isSaving = createMutation.isPending

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-background text-foreground">
      {/* Studio Top Navbar */}
      <header className="sticky top-0 z-30 flex h-14 w-full shrink-0 items-center justify-between border-b border-border bg-background/95 px-4 backdrop-blur-md sm:px-6">
        <div className="flex min-w-0 items-center gap-3">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate({ to: "/admin/assessments" })}
            className="size-8"
          >
            <ArrowLeft className="size-4" />
          </Button>

          <div className="flex min-w-0 items-center gap-2">
            <span className="flex size-2 shrink-0 rounded-full bg-emerald-500 ring-2 ring-emerald-500/20" />
            <h1 className="truncate text-sm font-semibold tracking-tight text-foreground">
              {title.trim() || "New Assessment Studio"}
            </h1>
            <Badge
              variant="secondary"
              className="shrink-0 px-2 py-0.5 font-mono text-[10px] uppercase border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
            >
              Creation Wizard
            </Badge>
          </div>
        </div>

        {/* Stepper Navigation */}
        <div className="hidden items-center gap-1 rounded-lg border border-border/60 bg-muted/40 p-1 text-xs md:flex">
          {[
            { num: 1, label: "Basics & Timing", icon: Clock },
            { num: 2, label: "Question Source", icon: Layers },
            { num: 3, label: "Candidate Access", icon: Users },
            { num: 4, label: "Validation & Review", icon: CheckCircle2 },
          ].map((step, idx) => {
            const Icon = step.icon
            const isActive = currentStep === step.num
            const isCompleted = currentStep > step.num

            return (
              <div key={step.num} className="flex items-center">
                <button
                  type="button"
                  onClick={() => setCurrentStep(step.num)}
                  className={cn(
                    "flex items-center gap-1.5 rounded-md px-3 py-1 font-medium transition-colors cursor-pointer select-none",
                    isActive
                      ? "bg-background font-semibold text-foreground shadow-2xs"
                      : isCompleted
                        ? "text-foreground hover:bg-background/50"
                        : "text-muted-foreground hover:text-foreground"
                  )}
                >
                  <Icon className={cn("size-3.5", isActive ? "text-primary" : "")} />
                  <span>
                    {step.num}. {step.label}
                  </span>
                </button>
                {idx < 3 && <ChevronRight className="size-3 text-muted-foreground/40 mx-1" />}
              </div>
            )
          })}
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2">
          {currentStep < 4 ? (
            <Button
              size="sm"
              onClick={() => setCurrentStep((prev) => Math.min(4, prev + 1))}
              disabled={currentStep === 1 && !title.trim()}
              className="gap-1.5 text-xs font-medium"
            >
              <span>Next</span>
              <ArrowRight className="size-3.5" />
            </Button>
          ) : (
            <Button
              size="sm"
              disabled={isSaving || isValidating || (validationResult !== null && !validationResult.valid)}
              onClick={() => handleSubmit()}
              className="gap-1.5 text-xs font-medium"
            >
              {isSaving ? (
                <Loader2 className="size-3.5 animate-spin" />
              ) : (
                <CheckCircle2 className="size-3.5" />
              )}
              <span>Create Assessment</span>
            </Button>
          )}
        </div>
      </header>

      {/* Main Multi-Step Workspace */}
      <main className="min-h-0 w-full flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
        <div className="max-w-5xl mx-auto space-y-8">
          {/* STEP 1: BASICS & TIMING */}
          {currentStep === 1 && (
            <div className="space-y-6">
              <StepBasicInfo
                title={title}
                onTitleChange={setTitle}
                description={description}
                onDescriptionChange={setDescription}
                durationMinutes={durationMinutes}
                onDurationChange={setDurationMinutes}
                scheduledStart={scheduledStart}
                onScheduledStartChange={setScheduledStart}
                scheduledEnd={scheduledEnd}
                onScheduledEndChange={setScheduledEnd}
                maxAttempts={maxAttempts}
                onMaxAttemptsChange={setMaxAttempts}
                passingScore={passingScore}
                onPassingScoreChange={setPassingScore}
              />

              <div className="flex justify-end">
                <Button
                  onClick={() => setCurrentStep(2)}
                  disabled={!title.trim()}
                  className="gap-2"
                >
                  <span>Continue to Question Source</span>
                  <ArrowRight className="size-4" />
                </Button>
              </div>
            </div>
          )}

          {/* STEP 2: QUESTION SOURCE */}
          {currentStep === 2 && (
            <div className="space-y-6">
              <StepQuestionSelection
                selectionMode={selectionMode}
                onSelectionModeChange={setSelectionMode}
                selectedQuestionBankId={selectedQuestionBankId}
                onSelectedQuestionBankIdChange={setSelectedQuestionBankId}
                curatedQuestions={curatedQuestions}
                onOpenPicker={() => setPickerOpen(true)}
                onUpdatePoints={handleUpdatePoints}
                onRemoveQuestion={handleRemoveQuestion}
                publishedBanks={publishedBanks}
                loadingBanks={loadingBanks}
                bankPreview={bankPreview}
                loadingBankPreview={loadingBankPreview}
              />

              <div className="flex justify-between">
                <Button
                  variant="outline"
                  onClick={() => setCurrentStep(1)}
                  className="gap-2"
                >
                  <ArrowLeft className="size-4" />
                  <span>Back to Basics</span>
                </Button>
                <Button
                  onClick={() => setCurrentStep(3)}
                  disabled={
                    (selectionMode === "QUESTION_BANK" && !selectedQuestionBankId) ||
                    (selectionMode === "INDIVIDUAL_QUESTIONS" && curatedQuestions.length === 0)
                  }
                  className="gap-2"
                >
                  <span>Continue to Candidates</span>
                  <ArrowRight className="size-4" />
                </Button>
              </div>
            </div>
          )}

          {/* STEP 3: CANDIDATE ACCESS */}
          {currentStep === 3 && (
            <div className="space-y-6">
              <StepCandidateInvitations
                candidateEmailsRaw={candidateEmailsRaw}
                onCandidateEmailsRawChange={setCandidateEmailsRaw}
                parsedCandidateEmails={parsedCandidateEmails}
                enableInvitationCode={enableInvitationCode}
                onEnableInvitationCodeChange={setEnableInvitationCode}
                invitationCodeExpiresAt={invitationCodeExpiresAt}
                onInvitationCodeExpiresAtChange={setInvitationCodeExpiresAt}
                invitationCodeMaxUses={invitationCodeMaxUses}
                onInvitationCodeMaxUsesChange={setInvitationCodeMaxUses}
              />

              <div className="flex justify-between">
                <Button
                  variant="outline"
                  onClick={() => setCurrentStep(2)}
                  className="gap-2"
                >
                  <ArrowLeft className="size-4" />
                  <span>Back to Questions</span>
                </Button>
                <Button
                  onClick={() => setCurrentStep(4)}
                  className="gap-2"
                >
                  <span>Review & Finalize</span>
                  <ArrowRight className="size-4" />
                </Button>
              </div>
            </div>
          )}

          {/* STEP 4: REVIEW & VALIDATION */}
          {currentStep === 4 && (
            <div className="space-y-6">
              <StepReviewValidation
                title={title}
                durationMinutes={durationMinutes}
                scheduledStart={scheduledStart}
                scheduledEnd={scheduledEnd}
                maxAttempts={maxAttempts}
                passingScore={passingScore}
                selectionMode={selectionMode}
                selectedBank={selectedBank}
                bankPreview={bankPreview}
                curatedQuestions={curatedQuestions}
                parsedCandidateEmails={parsedCandidateEmails}
                enableInvitationCode={enableInvitationCode}
                totalCalculatedQuestions={totalCalculatedQuestions}
                totalCalculatedPoints={totalCalculatedPoints}
                mcqCount={mcqCount}
                codingCount={codingCount}
                isValidating={isValidating}
                validationResult={validationResult}
                onRevalidate={() => runPreCreationValidation()}
                onFixFieldInStep={(stepNum) => setCurrentStep(stepNum)}
              />

              {/* Bottom Submit Controls */}
              <div className="flex justify-between items-center pt-2">
                <Button
                  variant="outline"
                  onClick={() => setCurrentStep(3)}
                  className="gap-2"
                >
                  <ArrowLeft className="size-4" />
                  <span>Back to Candidates</span>
                </Button>

                <div className="flex items-center gap-2">
                  <Button
                    disabled={
                      isSaving ||
                      isValidating ||
                      (validationResult !== null && !validationResult.valid)
                    }
                    onClick={() => handleSubmit()}
                    className="gap-2 text-xs font-medium"
                  >
                    {isSaving ? (
                      <Loader2 className="size-3.5 animate-spin" />
                    ) : (
                      <CheckCircle2 className="size-3.5" />
                    )}
                    <span>Confirm & Create Assessment</span>
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* Question Picker Dialog */}
      <QuestionPickerDialog
        open={pickerOpen}
        onOpenChange={setPickerOpen}
        existingQuestionIds={new Set(curatedQuestions.map((q) => q.questionId))}
        onAddQuestions={handleAddQuestionsFromPicker}
      />
    </div>
  )
}
