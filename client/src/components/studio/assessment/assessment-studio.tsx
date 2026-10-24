import { useState, useMemo, useEffect, useCallback } from "react"
import axios from "axios"
import { useNavigate } from "@tanstack/react-router"
import {
  GraduationCap,
  Clock,
  Users,
  CheckCircle2,
  Layers,
  ArrowRight,
  ArrowLeft,
  Loader2,
  Code2,
  CheckSquare,
  KeyRound,
  AlertCircle,
  ChevronRight,
  Send,
  Lock,
  AlertTriangle,
  FileQuestion,
  Sparkles,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { Switch } from "@/components/ui/switch"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
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
  const validateMutation = useValidateAssessment()

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

  // Run pre-creation validation when entering Step 4
  const runPreCreationValidation = useCallback(() => {
    const payload = buildPayload()
    validateMutation.mutate(payload, {
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
  }, [buildPayload, validateMutation])

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
  const isValidating = validateMutation.isPending

  // Map validation error field to step number
  const getStepForField = (field: string): number => {
    if (field === "title" || field === "durationMinutes" || field === "scheduledWindow" || field === "maxAttempts" || field === "passingScore") {
      return 1
    }
    if (field === "questionBankId" || field === "questions") {
      return 2
    }
    if (field === "candidateEmails" || field === "invitationCodeExpiresAt" || field === "invitationCodeMaxUses") {
      return 3
    }
    return 1
  }

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
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Assessment Details</CardTitle>
                  <CardDescription>
                    Provide a distinct title and instructions for candidates taking this assessment.
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-foreground">
                      Assessment Title <span className="text-destructive">*</span>
                    </label>
                    <Input
                      placeholder="e.g. Senior Backend Java Engineer Assessment"
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                    />
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-foreground">
                      Instructions / Description
                    </label>
                    <Textarea
                      placeholder="Enter guidelines, allowed programming languages, reference hints, and conduct rules..."
                      rows={4}
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                    />
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle className="text-lg flex items-center gap-2">
                    <Clock className="size-5 text-primary" />
                    Timing & Benchmark Constraints
                  </CardTitle>
                  <CardDescription>
                    Configure overall countdown duration, scheduled availability window, and passing criteria.
                  </CardDescription>
                </CardHeader>
                <CardContent className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-foreground">
                      Duration (Minutes) <span className="text-destructive">*</span>
                    </label>
                    <Input
                      type="number"
                      min={1}
                      max={1440}
                      value={durationMinutes}
                      onChange={(e) => setDurationMinutes(Number(e.target.value))}
                    />
                    <p className="text-xs text-muted-foreground">
                      Countdown timer given to candidates once an attempt is started.
                    </p>
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-foreground">
                      Max Attempts (Optional)
                    </label>
                    <Input
                      type="number"
                      min={1}
                      placeholder="Unlimited if empty"
                      value={maxAttempts ?? ""}
                      onChange={(e) =>
                        setMaxAttempts(e.target.value ? Number(e.target.value) : undefined)
                      }
                    />
                    <p className="text-xs text-muted-foreground">
                      Leave empty for unlimited attempts during the schedule window.
                    </p>
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-foreground">
                      Scheduled Start (Optional)
                    </label>
                    <Input
                      type="datetime-local"
                      value={scheduledStart}
                      onChange={(e) => setScheduledStart(e.target.value)}
                    />
                    <p className="text-xs text-muted-foreground">
                      Candidates cannot access or start before this timestamp.
                    </p>
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-foreground">
                      Scheduled End / Deadline (Optional)
                    </label>
                    <Input
                      type="datetime-local"
                      value={scheduledEnd}
                      onChange={(e) => setScheduledEnd(e.target.value)}
                    />
                    <p className="text-xs text-muted-foreground">
                      Absolute cut-off time after which all attempts are automatically closed.
                    </p>
                  </div>

                  <div className="space-y-2 sm:col-span-2">
                    <label className="text-sm font-semibold text-foreground">
                      Passing Benchmark (Points - Optional)
                    </label>
                    <Input
                      type="number"
                      min={1}
                      placeholder="e.g. 70"
                      value={passingScore ?? ""}
                      onChange={(e) =>
                        setPassingScore(e.target.value ? Number(e.target.value) : undefined)
                      }
                    />
                    <p className="text-xs text-muted-foreground">
                      Minimum score required to receive a Passing grade on post-submission results.
                    </p>
                  </div>
                </CardContent>
              </Card>

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
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Question Source Strategy</CardTitle>
                  <CardDescription>
                    Question Banks provide validated, peer-reviewed questions with complete scoring rubrics.
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <button
                      type="button"
                      onClick={() => setSelectionMode("QUESTION_BANK")}
                      className={cn(
                        "flex flex-col items-start p-4 rounded-xl border text-left transition-all cursor-pointer",
                        selectionMode === "QUESTION_BANK"
                          ? "border-primary bg-primary/5 ring-2 ring-primary/20 shadow-xs"
                          : "border-border hover:bg-muted/40"
                      )}
                    >
                      <div className="flex items-center gap-2 mb-1.5">
                        <GraduationCap className="size-5 text-primary" />
                        <span className="font-semibold text-foreground">Published Question Bank (Recommended)</span>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        Select a published question bank. Its questions, judge configurations, and starter code are safely snapshotted into this immutable assessment.
                      </p>
                    </button>

                    <button
                      type="button"
                      onClick={() => setSelectionMode("INDIVIDUAL_QUESTIONS")}
                      className={cn(
                        "flex flex-col items-start p-4 rounded-xl border text-left transition-all cursor-pointer",
                        selectionMode === "INDIVIDUAL_QUESTIONS"
                          ? "border-primary bg-primary/5 ring-2 ring-primary/20 shadow-xs"
                          : "border-border hover:bg-muted/40"
                      )}
                    >
                      <div className="flex items-center gap-2 mb-1.5">
                        <Layers className="size-5 text-primary" />
                        <span className="font-semibold text-foreground">Custom Question Selection</span>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        Hand-pick individual published questions and customize question points manually.
                      </p>
                    </button>
                  </div>
                </CardContent>
              </Card>

              {/* Primary Mode: Question Bank Selection & Live Preview */}
              {selectionMode === "QUESTION_BANK" && (
                <div className="space-y-6">
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-lg">Choose a Published Question Bank</CardTitle>
                      <CardDescription>
                        Only question banks in PUBLISHED status can be selected for assessments.
                      </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {loadingBanks ? (
                        <div className="py-8 flex items-center justify-center text-muted-foreground gap-2">
                          <Loader2 className="size-4 animate-spin text-primary" />
                          <span className="text-xs">Loading published question banks...</span>
                        </div>
                      ) : publishedBanks.length === 0 ? (
                        <div className="p-8 border border-dashed rounded-lg text-center text-muted-foreground">
                          <AlertCircle className="size-8 mx-auto text-muted-foreground/50 mb-2" />
                          <p className="font-medium text-foreground">No Published Question Banks Found</p>
                          <p className="text-xs text-muted-foreground mt-1">
                            Go to Question Banks and publish a bank first, or switch to Custom Question Selection.
                          </p>
                        </div>
                      ) : (
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-72 overflow-y-auto p-1">
                          {publishedBanks.map((bank) => {
                            const isSelected = selectedQuestionBankId === bank.id
                            return (
                              <div
                                key={bank.id}
                                onClick={() => setSelectedQuestionBankId(bank.id)}
                                className={cn(
                                  "p-4 rounded-lg border transition-all cursor-pointer flex flex-col justify-between",
                                  isSelected
                                    ? "border-primary bg-primary/5 ring-2 ring-primary shadow-xs"
                                    : "border-border hover:bg-muted/30"
                                )}
                              >
                                <div>
                                  <div className="flex items-center justify-between mb-1">
                                    <span className="font-semibold text-sm text-foreground">
                                      {bank.title}
                                    </span>
                                    {isSelected && (
                                      <CheckCircle2 className="size-4 text-primary shrink-0" />
                                    )}
                                  </div>
                                  {bank.description && (
                                    <p className="text-xs text-muted-foreground line-clamp-2 mb-2">
                                      {bank.description}
                                    </p>
                                  )}
                                  {bank.category && (
                                    <Badge variant="outline" className="text-[10px] uppercase font-mono mb-2">
                                      {bank.category}
                                    </Badge>
                                  )}
                                </div>
                                <div className="flex items-center gap-2 pt-2 border-t text-[11px] text-muted-foreground">
                                  <span>{bank.totalQuestions} Questions</span>
                                  <span>•</span>
                                  <span>{bank.totalPoints} Pts</span>
                                  <span>•</span>
                                  <span>
                                    {bank.mcqCount} MCQ / {bank.codingCount} Coding
                                  </span>
                                </div>
                              </div>
                            )
                          })}
                        </div>
                      )}
                    </CardContent>
                  </Card>

                  {/* Rich Bank Preview Panel */}
                  {selectedQuestionBankId && (
                    <Card className="border-primary/30">
                      <CardHeader className="pb-3">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <Sparkles className="size-4 text-primary" />
                            <CardTitle className="text-base">Selected Bank Snapshot Preview</CardTitle>
                          </div>
                          {loadingBankPreview ? (
                            <Loader2 className="size-3.5 animate-spin text-muted-foreground" />
                          ) : bankPreview?.eligibleForAssessment ? (
                            <Badge className="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30 gap-1">
                              <CheckCircle2 className="size-3" />
                              <span>Eligible for Assessment</span>
                            </Badge>
                          ) : (
                            <Badge variant="destructive" className="gap-1">
                              <AlertCircle className="size-3" />
                              <span>Ineligible Questions</span>
                            </Badge>
                          )}
                        </div>
                        <CardDescription>
                          This configuration and all questions below will be snapshotted permanently into this assessment upon creation.
                        </CardDescription>
                      </CardHeader>
                      <CardContent className="space-y-4">
                        {loadingBankPreview ? (
                          <div className="py-6 flex items-center justify-center gap-2 text-muted-foreground text-xs">
                            <Loader2 className="size-4 animate-spin text-primary" />
                            Loading bank snapshot preview and judging configurations...
                          </div>
                        ) : bankPreview ? (
                          <>
                            {/* Ineligibility Alert */}
                            {!bankPreview.eligibleForAssessment && bankPreview.ineligibilityReason && (
                              <Alert variant="destructive">
                                <AlertCircle className="size-4" />
                                <AlertTitle>Bank Has Ineligible Questions</AlertTitle>
                                <AlertDescription className="text-xs">
                                  {bankPreview.ineligibilityReason}
                                </AlertDescription>
                              </Alert>
                            )}

                            {/* Metrics Breakdown */}
                            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-3 bg-muted/40 rounded-lg border text-center text-xs">
                              <div>
                                <span className="text-muted-foreground">Total Questions</span>
                                <div className="text-base font-bold text-foreground mt-0.5">
                                  {bankPreview.totalQuestions}
                                </div>
                              </div>
                              <div>
                                <span className="text-muted-foreground">MCQ vs Coding</span>
                                <div className="text-base font-bold text-foreground mt-0.5">
                                  {bankPreview.mcqCount} MCQ / {bankPreview.codingCount} Code
                                </div>
                              </div>
                              <div>
                                <span className="text-muted-foreground">Total Assessment Score</span>
                                <div className="text-base font-bold text-foreground mt-0.5">
                                  {bankPreview.totalPoints} Points
                                </div>
                              </div>
                              <div>
                                <span className="text-muted-foreground">Category</span>
                                <div className="text-base font-bold text-foreground mt-0.5">
                                  {bankPreview.category || "General"}
                                </div>
                              </div>
                            </div>

                            {/* Difficulty Distribution */}
                            <div className="flex flex-wrap items-center gap-2 text-xs">
                              <span className="text-muted-foreground font-medium">Difficulty Breakdown:</span>
                              {Object.entries(bankPreview.difficultyDistribution || {}).map(([diff, count]) => (
                                <Badge key={diff} variant="secondary" className="text-xs">
                                  {diff}: {count}
                                </Badge>
                              ))}
                            </div>

                            {/* Supported Languages */}
                            {bankPreview.supportedCodingLanguages && bankPreview.supportedCodingLanguages.length > 0 && (
                              <div className="flex flex-wrap items-center gap-2 text-xs">
                                <span className="text-muted-foreground font-medium flex items-center gap-1">
                                  <Code2 className="size-3.5 text-primary" />
                                  Supported Languages:
                                </span>
                                {bankPreview.supportedCodingLanguages.map((lang) => (
                                  <Badge key={lang} variant="outline" className="font-mono text-[10px]">
                                    {lang}
                                  </Badge>
                                ))}
                              </div>
                            )}

                            {/* Questions Item List */}
                            <div className="border rounded-lg overflow-hidden">
                              <div className="px-3 py-2 bg-muted/50 border-b text-xs font-semibold text-muted-foreground">
                                Question Bank Contents ({bankPreview.items.length})
                              </div>
                              <div className="max-h-60 overflow-y-auto divide-y divide-border">
                                {bankPreview.items.map((item, idx) => (
                                  <div key={item.questionId} className="p-3 flex items-center justify-between text-xs gap-3">
                                    <div className="flex items-center gap-2.5 min-w-0">
                                      <span className="font-mono text-muted-foreground w-5 text-right">
                                        {idx + 1}.
                                      </span>
                                      {item.questionType === "MCQ" ? (
                                        <CheckSquare className="size-3.5 text-amber-500 shrink-0" />
                                      ) : (
                                        <Code2 className="size-3.5 text-blue-500 shrink-0" />
                                      )}
                                      <span className="font-medium text-foreground truncate">
                                        {item.title}
                                      </span>
                                      <Badge variant="outline" className="text-[10px] uppercase shrink-0">
                                        {item.difficulty}
                                      </Badge>
                                      {!item.eligible && (
                                        <Badge variant="destructive" className="text-[10px] shrink-0" title={item.ineligibilityReason}>
                                          Issues
                                        </Badge>
                                      )}
                                    </div>
                                    <div className="flex items-center gap-3 shrink-0">
                                      <span className="font-mono font-medium text-foreground">
                                        {item.points} pts
                                      </span>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          </>
                        ) : null}
                      </CardContent>
                    </Card>
                  )}
                </div>
              )}

              {/* Mode B: Individual Questions */}
              {selectionMode === "INDIVIDUAL_QUESTIONS" && (
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between">
                    <div>
                      <CardTitle className="text-lg">Curated Assessment Questions</CardTitle>
                      <CardDescription>
                        Pick individual questions from the repository.
                      </CardDescription>
                    </div>
                    <Button
                      size="sm"
                      onClick={() => setPickerOpen(true)}
                      className="gap-1.5 text-xs"
                    >
                      <span>Add Questions</span>
                    </Button>
                  </CardHeader>
                  <CardContent>
                    {curatedQuestions.length === 0 ? (
                      <div className="p-8 border border-dashed rounded-lg text-center text-muted-foreground">
                        <FileQuestion className="size-8 mx-auto text-muted-foreground/50 mb-2" />
                        <p className="font-medium text-foreground">No Questions Added Yet</p>
                        <p className="text-xs text-muted-foreground mt-1">
                          Click "Add Questions" to select published questions.
                        </p>
                      </div>
                    ) : (
                      <div className="border rounded-lg overflow-hidden divide-y divide-border">
                        {curatedQuestions.map((q, idx) => (
                          <div key={q.questionId} className="p-3 flex items-center justify-between text-xs gap-3">
                            <div className="flex items-center gap-2.5 min-w-0">
                              <span className="font-mono text-muted-foreground w-5 text-right">
                                {idx + 1}.
                              </span>
                              {q.questionType === "MCQ" ? (
                                <CheckSquare className="size-3.5 text-amber-500 shrink-0" />
                              ) : (
                                <Code2 className="size-3.5 text-blue-500 shrink-0" />
                              )}
                              <span className="font-medium text-foreground truncate">
                                {q.title}
                              </span>
                            </div>
                            <div className="flex items-center gap-3 shrink-0">
                              <Input
                                type="number"
                                min={1}
                                className="w-16 h-7 text-xs font-mono text-center"
                                value={q.points}
                                onChange={(e) => handleUpdatePoints(q.questionId, Number(e.target.value))}
                              />
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleRemoveQuestion(q.questionId)}
                                className="h-7 px-2 text-destructive hover:bg-destructive/10"
                              >
                                Remove
                              </Button>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              )}

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
              {/* Method A: Email Invitations */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg flex items-center gap-2">
                    <Send className="size-5 text-primary" />
                    Direct Candidate Assignments
                  </CardTitle>
                  <CardDescription>
                    Candidates with these emails will automatically find this assessment on their dashboard upon login.
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <div className="flex justify-between text-xs font-semibold">
                      <span>Candidate Email Addresses</span>
                      <span className="text-primary font-bold">
                        {parsedCandidateEmails.length} Recognized Email(s)
                      </span>
                    </div>
                    <Textarea
                      placeholder="alice@example.com, bob@company.org&#10;carol@university.edu"
                      rows={5}
                      value={candidateEmailsRaw}
                      onChange={(e) => setCandidateEmailsRaw(e.target.value)}
                    />
                    <p className="text-xs text-muted-foreground">
                      Accepts comma-separated, space-separated, or newline-separated email addresses.
                    </p>
                  </div>

                  {parsedCandidateEmails.length > 0 && (
                    <div className="p-3 bg-muted/40 rounded-lg border">
                      <span className="text-xs font-semibold text-foreground block mb-2">
                        Recognized Candidate List ({parsedCandidateEmails.length}):
                      </span>
                      <div className="flex flex-wrap gap-1.5 max-h-36 overflow-y-auto">
                        {parsedCandidateEmails.map((email) => (
                          <Badge key={email} variant="secondary" className="text-xs font-mono">
                            {email}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Method B: Shareable Invitation Code */}
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle className="text-lg flex items-center gap-2">
                        <KeyRound className="size-5 text-primary" />
                        Shareable Join Passcode
                      </CardTitle>
                      <CardDescription>
                        Generate a secure passcode allowing candidates to self-enroll and start the test.
                      </CardDescription>
                    </div>
                    <Switch
                      checked={enableInvitationCode}
                      onCheckedChange={setEnableInvitationCode}
                    />
                  </div>
                </CardHeader>

                {enableInvitationCode && (
                  <CardContent className="space-y-4 pt-2">
                    <div className="p-3 rounded-lg border border-primary/20 bg-primary/5 text-xs text-muted-foreground">
                      A cryptographically secure unique passcode (e.g. <span className="font-mono font-semibold text-primary">STK-XXXX-YYYY</span>) will be automatically generated and bound to this assessment.
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <label className="text-sm font-semibold text-foreground">
                          Passcode Expiration (Optional)
                        </label>
                        <Input
                          type="datetime-local"
                          value={invitationCodeExpiresAt}
                          onChange={(e) => setInvitationCodeExpiresAt(e.target.value)}
                        />
                        <p className="text-xs text-muted-foreground">
                          After this time, no new candidate can join with this code.
                        </p>
                      </div>

                      <div className="space-y-2">
                        <label className="text-sm font-semibold text-foreground">
                          Max Uses Limit (Optional)
                        </label>
                        <Input
                          type="number"
                          min={1}
                          placeholder="Unlimited if empty"
                          value={invitationCodeMaxUses ?? ""}
                          onChange={(e) =>
                            setInvitationCodeMaxUses(
                              e.target.value ? Number(e.target.value) : undefined
                            )
                          }
                        />
                        <p className="text-xs text-muted-foreground">
                          Maximum number of candidates allowed to join with this code.
                        </p>
                      </div>
                    </div>
                  </CardContent>
                )}
              </Card>

              <div className="flex justify-between">
                <Button
                  variant="outline"
                  onClick={() => setCurrentStep(2)}
                  className="gap-2"
                >
                  <ArrowLeft className="size-4" />
                  <span>Back to Questions</span>
                </Button>
                <Button onClick={() => setCurrentStep(4)} className="gap-2">
                  <span>Continue to Validation & Review</span>
                  <ArrowRight className="size-4" />
                </Button>
              </div>
            </div>
          )}

          {/* STEP 4: PRE-CREATION VALIDATION & REVIEW */}
          {currentStep === 4 && (
            <div className="space-y-6">
              {/* Permanent Immutability Warning Banner */}
              <div className="flex items-start gap-3 p-4 rounded-xl border border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400">
                <Lock className="size-5 shrink-0 mt-0.5 text-amber-600 dark:text-amber-400" />
                <div className="space-y-1">
                  <div className="font-semibold text-sm">
                    Assessment Immutability Notice
                  </div>
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
                      onClick={runPreCreationValidation}
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
                          <AlertTitle>Validation Errors Detected ({validationResult.errors.length})</AlertTitle>
                          <AlertDescription className="text-xs">
                            The assessment cannot be created until the following blocking configuration issues are resolved:
                          </AlertDescription>
                        </Alert>

                        <div className="divide-y divide-border rounded-lg border bg-destructive/5 overflow-hidden">
                          {validationResult.errors.map((err, idx) => (
                            <div key={idx} className="p-3 flex items-center justify-between text-xs gap-3">
                              <div className="flex items-center gap-2 min-w-0">
                                <Badge variant="destructive" className="font-mono text-[10px] shrink-0">
                                  {err.field}
                                </Badge>
                                <span className="text-destructive font-medium truncate">
                                  {err.message}
                                </span>
                              </div>
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={() => setCurrentStep(getStepForField(err.field))}
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
                        <div key={i} className="flex items-center gap-2 p-2.5 rounded-lg border border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-400 text-xs">
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
                        {scheduledStart ? new Date(scheduledStart).toLocaleString() : "Immediately Open"}{" "}
                        &rarr;{" "}
                        {scheduledEnd ? new Date(scheduledEnd).toLocaleString() : "No Deadline"}
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
                        {passingScore ? `${passingScore} / ${totalCalculatedPoints} points (${Math.round((passingScore / totalCalculatedPoints) * 100)}%)` : "Not Configured"}
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
                    disabled={isSaving || isValidating || (validationResult !== null && !validationResult.valid)}
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
