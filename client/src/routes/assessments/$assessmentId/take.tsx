import { useState, useEffect, useMemo, useRef, useCallback } from "react"
import axios from "axios"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import {
  Clock,
  CheckCircle2,
  AlertCircle,
  Play,
  Send,
  RotateCcw,
  Code2,
  CheckSquare,
  ChevronLeft,
  ChevronRight,
  ShieldAlert,
  Loader2,
  Check,
  Bookmark,
  BookmarkCheck,
  LayoutGrid,
  FileCode,
  Terminal,
} from "lucide-react"

import { Logo } from "@/components/logo"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Textarea } from "@/components/ui/textarea"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { ResizablePanelGroup, ResizablePanel, ResizableHandle } from "@/components/ui/resizable"
import { MonacoEditor } from "@/components/monaco-editor"
import { MarkdownPreview } from "@/components/markdown-preview/markdown-preview"
import { useAssessmentWebSocket } from "@/hooks/use-assessment-websocket"
import {
  useActiveAttempt,
  useFinalSubmitAssessment,
  useMarkQuestionForReview,
  useRunCustomInput,
  useRunSample,
  useSaveCodeResponse,
  useSaveMcqResponse,
  useSubmitCodeResponse,
  useRecordTelemetry,
} from "@/hooks/use-assessments"
import { getLanguageById } from "@/config/languages"
import { resolveMonacoLanguage, resolveStarterCode } from "@/lib/monaco-languages"
import { toast } from "@/components/ui/toast"
import { cn } from "@/lib/utils"
import type {
  AssessmentItemResponse,
  CodingProblemExecutionResponse,
  SavedQuestionResponse,
  ClientTelemetryEventDto,
} from "@/types/assessment"

export const Route = createFileRoute("/assessments/$assessmentId/take")({
  component: AssessmentExamPage,
})

export function AssessmentExamPage() {
  const { assessmentId } = Route.useParams()
  const navigate = useNavigate()

  // Queries & Mutations
  const {
    data: attempt,
    isLoading,
    isError,
    error,
    refetch: refetchAttempt,
  } = useActiveAttempt(assessmentId)

  const saveMcqMutation = useSaveMcqResponse(assessmentId)
  const saveCodeMutation = useSaveCodeResponse(assessmentId)
  const runSampleMutation = useRunSample(assessmentId)
  const runCustomInputMutation = useRunCustomInput(assessmentId)
  const submitCodeMutation = useSubmitCodeResponse(assessmentId)
  const markForReviewMutation = useMarkQuestionForReview(assessmentId)
  const finalSubmitMutation = useFinalSubmitAssessment(assessmentId)
  const recordTelemetryMutation = useRecordTelemetry(assessmentId)

  // Current Question Pointer
  const [currentIndex, setCurrentIndex] = useState(0)

  // Current question object
  const currentQuestion: AssessmentItemResponse | undefined = useMemo(() => {
    return attempt?.questions?.[currentIndex]
  }, [attempt?.questions, currentIndex])

  // Local response stores (keyed by questionId)
  const [localResponses, setLocalResponses] = useState<Record<string, SavedQuestionResponse>>({})
  const [saveStatus, setSaveStatus] = useState<"SAVED" | "SAVING" | "UNSAVED">("SAVED")

  // Initialize local responses once per attempt
  const loadedAttemptIdRef = useRef<string | null>(null)
  useEffect(() => {
    if (attempt && attempt.attemptId !== loadedAttemptIdRef.current) {
      loadedAttemptIdRef.current = attempt.attemptId
      if (attempt.responses) {
        setLocalResponses(attempt.responses)
      }
    }
  }, [attempt])

  // Code editor state
  const [currentLanguage, setCurrentLanguage] = useState<string>("java-21")
  const [currentCode, setCurrentCode] = useState<string>("")
  const [activeBottomTab, setActiveBottomTab] = useState<"sample" | "custom" | "submit">("sample")
  const [selectedSampleIndex, setSelectedSampleIndex] = useState<number>(0)
  const [customInputText, setCustomInputText] = useState<string>("")
  const [sampleResult, setSampleResult] = useState<CodingProblemExecutionResponse | null>(null)
  const [customResult, setCustomResult] = useState<CodingProblemExecutionResponse | null>(null)
  const [submitResult, setSubmitResult] = useState<SavedQuestionResponse | null>(null)

  // Modals
  const [submitModalOpen, setSubmitModalOpen] = useState(false)
  const [overviewModalOpen, setOverviewModalOpen] = useState(false)
  const [timeExpiredModalOpen, setTimeExpiredModalOpen] = useState(false)

  // Countdown Timer
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null)

  // Telemetry buffer
  const telemetryBufferRef = useRef<ClientTelemetryEventDto[]>([])

  const flushTelemetry = useCallback(() => {
    if (telemetryBufferRef.current.length === 0) return
    const batch = [...telemetryBufferRef.current]
    telemetryBufferRef.current = []
    recordTelemetryMutation.mutate(batch)
  }, [recordTelemetryMutation])

  const recordEvent = useCallback(
    (type: ClientTelemetryEventDto["eventType"], metadata?: Record<string, unknown>) => {
      const event: ClientTelemetryEventDto = {
        eventType: type,
        questionId: currentQuestion?.questionId,
        metadata,
        clientTimestamp: new Date().toISOString(),
      }
      telemetryBufferRef.current.push(event)
      if (telemetryBufferRef.current.length >= 5) {
        flushTelemetry()
      }
    },
    [currentQuestion?.questionId, flushTelemetry]
  )

  // Periodic telemetry flush & window focus/blur tracking
  useEffect(() => {
    const handleBlur = () => {
      recordEvent("WINDOW_BLUR", { reason: "window_blur" })
      flushTelemetry()
    }

    const handleFocus = () => {
      recordEvent("WINDOW_FOCUS", { reason: "window_focus" })
      flushTelemetry()
    }

    window.addEventListener("blur", handleBlur)
    window.addEventListener("focus", handleFocus)

    const interval = setInterval(flushTelemetry, 15000)

    return () => {
      window.removeEventListener("blur", handleBlur)
      window.removeEventListener("focus", handleFocus)
      clearInterval(interval)
    }
  }, [recordEvent, flushTelemetry])

  // Track question navigation in telemetry
  const lastNavigatedIndexRef = useRef<number>(-1)
  useEffect(() => {
    if (currentIndex !== lastNavigatedIndexRef.current && currentQuestion?.questionId) {
      lastNavigatedIndexRef.current = currentIndex
      recordEvent("NAVIGATE_QUESTION", { questionIndex: currentIndex + 1 })
    }
  }, [currentIndex, currentQuestion?.questionId, recordEvent])

  // WebSocket Connection
  const wsStatus = useAssessmentWebSocket({
    assessmentId,
    attemptId: attempt?.attemptId || "",
    enabled: Boolean(attempt && attempt.status === "IN_PROGRESS"),
  })

  // If already submitted or expired, navigate to results
  useEffect(() => {
    if (attempt?.status === "SUBMITTED" || attempt?.status === "EXPIRED") {
      navigate({
        to: "/assessments/$assessmentId/results",
        params: { assessmentId },
      })
    }
  }, [attempt?.status, assessmentId, navigate])

  // Timer Tick
  useEffect(() => {
    if (!attempt?.effectiveDeadline) return

    const deadlineTime = new Date(attempt.effectiveDeadline).getTime()

    const updateTimer = () => {
      const now = Date.now()
      const diffSec = Math.max(0, Math.floor((deadlineTime - now) / 1000))
      setRemainingSeconds(diffSec)

      if (diffSec <= 0 && attempt.status === "IN_PROGRESS") {
        setTimeExpiredModalOpen(true)
      }
    }

    updateTimer()
    const interval = setInterval(updateTimer, 1000)
    return () => clearInterval(interval)
  }, [attempt?.effectiveDeadline, attempt?.status])

  // Sync editor language & code when switching questions
  const activeQuestionId = currentQuestion?.questionId
  const loadedQuestionIdRef = useRef<string | null>(null)

  useEffect(() => {
    if (!currentQuestion || activeQuestionId === loadedQuestionIdRef.current) return
    loadedQuestionIdRef.current = activeQuestionId ?? null

    if (currentQuestion.questionType === "CODING") {
      const savedResp = localResponses[currentQuestion.questionId]
      const availableLanguages =
        currentQuestion.supportedLanguageIds && currentQuestion.supportedLanguageIds.length > 0
          ? currentQuestion.supportedLanguageIds
          : ["java-21"]
      const selectedLang =
        savedResp?.languageId && availableLanguages.includes(savedResp.languageId)
          ? savedResp.languageId
          : availableLanguages[0] || "java-21"

      setCurrentLanguage(selectedLang)

      const savedCode = savedResp?.code
      const resolvedStarter = resolveStarterCode(currentQuestion.starterCode, selectedLang)
      const defaultStarter =
        resolvedStarter || `// Write your solution in ${selectedLang}\n`

      setCurrentCode(savedCode !== undefined ? savedCode : defaultStarter)
      setSampleResult(null)
      setCustomResult(null)
      setSubmitResult(null)
      setCustomInputText("")
      setSelectedSampleIndex(0)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeQuestionId])

  // Debounced auto-save for code
  const codeSaveTimeoutRef = useRef<number | null>(null)

  const handleCodeChange = (newCode: string) => {
    setCurrentCode(newCode)
    setSaveStatus("SAVING")

    if (codeSaveTimeoutRef.current) {
      clearTimeout(codeSaveTimeoutRef.current)
    }

    codeSaveTimeoutRef.current = window.setTimeout(async () => {
      if (!currentQuestion) return
      try {
        await saveCodeMutation.mutateAsync({
          questionId: currentQuestion.questionId,
          languageId: currentLanguage,
          code: newCode,
        })
        setLocalResponses((prev) => ({
          ...prev,
          [currentQuestion.questionId]: {
            ...prev[currentQuestion.questionId],
            questionId: currentQuestion.questionId,
            questionType: "CODING",
            languageId: currentLanguage,
            code: newCode,
          },
        }))
        setSaveStatus("SAVED")
      } catch {
        setSaveStatus("UNSAVED")
      }
    }, 1200)
  }

  // Language switch handler
  const handleLanguageChange = (lang: string) => {
    setCurrentLanguage(lang)
    const starter =
      resolveStarterCode(currentQuestion?.starterCode, lang) ||
      `// Write your solution in ${lang}\n`
    setCurrentCode(starter)
    handleCodeChange(starter)
  }

  // Reset to starter code
  const handleResetStarterCode = () => {
    if (!currentQuestion) return
    const starter =
      resolveStarterCode(currentQuestion.starterCode, currentLanguage) ||
      `// Write your solution in ${currentLanguage}\n`
    setCurrentCode(starter)
    handleCodeChange(starter)
    toast.add({
      title: "Starter Code Reset",
      description: "Code has been reset to default starter template.",
      type: "success",
    })
  }

  // MCQ Selection Handler
  const handleMcqOptionSelect = async (optionId: string) => {
    if (!currentQuestion) return

    const existingResp = localResponses[currentQuestion.questionId]
    const existingOptions = existingResp?.selectedOptionIds || []
    const isMulti = currentQuestion.mcqType === "MULTIPLE_CHOICE"

    let updatedOptions: string[] = []
    if (isMulti) {
      if (existingOptions.includes(optionId)) {
        updatedOptions = existingOptions.filter((id) => id !== optionId)
      } else {
        updatedOptions = [...existingOptions, optionId]
      }
    } else {
      updatedOptions = [optionId]
    }

    // Update locally
    setLocalResponses((prev) => ({
      ...prev,
      [currentQuestion.questionId]: {
        ...prev[currentQuestion.questionId],
        questionId: currentQuestion.questionId,
        questionType: "MCQ",
        selectedOptionIds: updatedOptions,
      },
    }))
    setSaveStatus("SAVING")

    try {
      await saveMcqMutation.mutateAsync({
        questionId: currentQuestion.questionId,
        selectedOptionIds: updatedOptions,
      })
      setSaveStatus("SAVED")
    } catch {
      setSaveStatus("UNSAVED")
    }
  }

  // Clear MCQ selection
  const handleClearMcqSelection = async () => {
    if (!currentQuestion) return

    setLocalResponses((prev) => ({
      ...prev,
      [currentQuestion.questionId]: {
        ...prev[currentQuestion.questionId],
        questionId: currentQuestion.questionId,
        questionType: "MCQ",
        selectedOptionIds: [],
      },
    }))
    setSaveStatus("SAVING")

    try {
      await saveMcqMutation.mutateAsync({
        questionId: currentQuestion.questionId,
        selectedOptionIds: [],
      })
      setSaveStatus("SAVED")
    } catch {
      setSaveStatus("UNSAVED")
    }
  }

  // Toggle Mark For Review
  const handleToggleMarkForReview = async () => {
    if (!currentQuestion) return
    const currentMarked =
      localResponses[currentQuestion.questionId]?.markedForReview ?? false
    const newMarked = !currentMarked

    setLocalResponses((prev) => ({
      ...prev,
      [currentQuestion.questionId]: {
        ...prev[currentQuestion.questionId],
        questionId: currentQuestion.questionId,
        questionType: currentQuestion.questionType,
        markedForReview: newMarked,
      },
    }))

    try {
      await markForReviewMutation.mutateAsync({
        questionId: currentQuestion.questionId,
        markedForReview: newMarked,
      })
      toast.add({
        title: newMarked ? "Marked for Review" : "Removed Review Mark",
        description: newMarked
          ? `Question #${currentIndex + 1} marked for review.`
          : `Question #${currentIndex + 1} unmarked.`,
        type: "success",
      })
    } catch {
      setLocalResponses((prev) => ({
        ...prev,
        [currentQuestion.questionId]: {
          ...prev[currentQuestion.questionId],
          markedForReview: currentMarked,
        },
      }))
    }
  }

  // Run Sample Tests
  const handleRunSampleTests = async () => {
    if (!currentQuestion) return
    setActiveBottomTab("sample")
    try {
      const res = await runSampleMutation.mutateAsync({
        questionId: currentQuestion.questionId,
        languageId: currentLanguage,
        code: currentCode,
      })
      setSampleResult(res)
      setSelectedSampleIndex(0)
    } catch {
      // error handled by mutation
    }
  }

  // Run Custom Input
  const handleRunCustomInput = async () => {
    if (!currentQuestion) return
    setActiveBottomTab("custom")
    try {
      const res = await runCustomInputMutation.mutateAsync({
        questionId: currentQuestion.questionId,
        languageId: currentLanguage,
        code: currentCode,
        stdin: customInputText,
      })
      setCustomResult(res)
    } catch {
      // error handled by mutation
    }
  }

  // Submit Code Solution
  const handleSubmitCode = async () => {
    if (!currentQuestion) return
    setActiveBottomTab("submit")
    try {
      const res = await submitCodeMutation.mutateAsync({
        questionId: currentQuestion.questionId,
        languageId: currentLanguage,
        code: currentCode,
      })
      setSubmitResult(res)
      setLocalResponses((prev) => ({
        ...prev,
        [currentQuestion.questionId]: {
          ...prev[currentQuestion.questionId],
          ...res,
        },
      }))
    } catch {
      // error handled by mutation
    }
  }

  // Final Assessment Submission
  const handleConfirmFinalSubmit = async () => {
    try {
      flushTelemetry()
      await finalSubmitMutation.mutateAsync()
      setSubmitModalOpen(false)
      navigate({
        to: "/assessments/$assessmentId/results",
        params: { assessmentId },
      })
    } catch {
      // error handled by mutation
    }
  }

  // Format seconds to HH:MM:SS
  const formatTimer = (totalSec: number | null) => {
    if (totalSec === null) return "--:--"
    const hours = Math.floor(totalSec / 3600)
    const minutes = Math.floor((totalSec % 3600) / 60)
    const seconds = totalSec % 60

    if (hours > 0) {
      return `${hours.toString().padStart(2, "0")}:${minutes
        .toString()
        .padStart(2, "0")}:${seconds.toString().padStart(2, "0")}`
    }
    return `${minutes.toString().padStart(2, "0")}:${seconds
      .toString()
      .padStart(2, "0")}`
  }

  // Stats calculation
  const questionsList = useMemo(
    () => attempt?.questions ?? [],
    [attempt?.questions]
  )
  const totalQuestionsCount = questionsList.length

  const answeredCount = useMemo(() => {
    return questionsList.filter((q) => {
      const resp = localResponses[q.questionId]
      if (!resp) return false
      if (q.questionType === "MCQ") {
        return (resp.selectedOptionIds?.length ?? 0) > 0
      }
      if (q.questionType === "CODING") {
        return Boolean(resp.code && resp.code.trim().length > 0)
      }
      return false
    }).length
  }, [questionsList, localResponses])

  const markedCount = useMemo(() => {
    return questionsList.filter((q) => {
      return Boolean(localResponses[q.questionId]?.markedForReview)
    }).length
  }, [questionsList, localResponses])

  const unansweredCount = Math.max(0, totalQuestionsCount - answeredCount)

  const isCurrentQuestionMarked = currentQuestion
    ? Boolean(localResponses[currentQuestion.questionId]?.markedForReview)
    : false

  if (isLoading) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3 text-muted-foreground">
          <Loader2 className="size-8 animate-spin text-primary" />
          <p className="text-sm font-medium">Initializing secure assessment environment...</p>
        </div>
      </div>
    )
  }

  if (isError || !attempt) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-background p-6">
        <div className="max-w-md text-center space-y-4">
          <AlertCircle className="size-12 text-destructive mx-auto" />
          <h2 className="text-2xl font-bold">Unable to Load Assessment Attempt</h2>
          <p className="text-sm text-muted-foreground">
            {axios.isAxiosError(error)
              ? (error.response?.data as { message?: string } | undefined)?.message ||
                "Your test session could not be established. Please check your network and try again."
              : error instanceof Error
                ? error.message
                : "Your test session could not be established. Please check your network and try again."}
          </p>
          {axios.isAxiosError(error) && error.response?.status === 401 ? (
            <div className="flex flex-col gap-2 pt-2">
              <Button onClick={() => navigate({ to: "/login" })}>
                Sign In to Continue
              </Button>
              <Button
                variant="outline"
                onClick={() =>
                  navigate({
                    to: "/assessments/$assessmentId",
                    params: { assessmentId },
                  })
                }
              >
                Back to Assessment Overview
              </Button>
            </div>
          ) : (
            <Button onClick={() => refetchAttempt()} variant="outline">
              Try Again
            </Button>
          )}
        </div>
      </div>
    )
  }

  // Timer urgency style
  const isUrgent = remainingSeconds !== null && remainingSeconds < 300 // < 5 mins
  const isCritical = remainingSeconds !== null && remainingSeconds < 60 // < 1 min

  return (
    <div className="flex flex-col h-screen w-full bg-background overflow-hidden text-foreground">
      {/* Superseded Warning Banner if opened in another window */}
      {wsStatus.isSuperseded && (
        <div className="bg-destructive text-destructive-foreground px-4 py-2 text-center text-xs font-semibold flex items-center justify-center gap-2 shadow-md z-50">
          <ShieldAlert className="size-4 shrink-0" />
          <span>
            {wsStatus.supersededMessage ||
              "Assessment was opened in another window. This session is now inactive."}
          </span>
        </div>
      )}

      {/* Top Header Bar */}
      <header className="h-14 border-b border-border bg-card/90 backdrop-blur-md px-4 sm:px-6 flex items-center justify-between shrink-0 z-30">
        {/* Left: Brand + Assessment Title */}
        <div className="flex items-center gap-3 min-w-0">
          <Logo size={24} />
          <div className="h-4 w-px bg-border hidden sm:block" />
          <span className="text-sm font-semibold truncate max-w-xs sm:max-w-md">
            {attempt.title}
          </span>
          <Badge variant="outline" className="text-[10px] hidden md:inline-flex">
            Question {currentIndex + 1} of {totalQuestionsCount}
          </Badge>
        </div>

        {/* Center: Live Authoritative Countdown Timer */}
        <div className="flex items-center gap-3">
          <div
            className={cn(
              "flex items-center gap-1.5 px-3 py-1 rounded-full border text-xs font-mono font-bold transition-colors select-none",
              isCritical
                ? "bg-destructive/15 text-destructive border-destructive/40 animate-pulse"
                : isUrgent
                  ? "bg-amber-500/15 text-amber-600 dark:text-amber-400 border-amber-500/40"
                  : "bg-muted text-foreground border-border"
            )}
          >
            <Clock className="size-3.5" />
            <span>{formatTimer(remainingSeconds)}</span>
          </div>

          {/* Autosave Indicator */}
          <span className="text-[11px] text-muted-foreground hidden lg:inline-flex items-center gap-1">
            {saveStatus === "SAVING" ? (
              <>
                <Loader2 className="size-3 animate-spin text-primary" />
                <span>Saving...</span>
              </>
            ) : saveStatus === "SAVED" ? (
              <>
                <Check className="size-3 text-emerald-500" />
                <span>Saved</span>
              </>
            ) : (
              <span className="text-destructive">Unsaved</span>
            )}
          </span>
        </div>

        {/* Right: Mark for Review + Overview + Presence + Submit */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Mark for Review Toggle */}
          <Button
            variant={isCurrentQuestionMarked ? "default" : "outline"}
            size="sm"
            onClick={handleToggleMarkForReview}
            disabled={markForReviewMutation.isPending}
            className={cn(
              "h-8 text-xs gap-1.5 font-medium px-2.5 sm:px-3",
              isCurrentQuestionMarked
                ? "bg-purple-600 hover:bg-purple-700 text-white"
                : "text-muted-foreground hover:text-foreground"
            )}
            title="Mark this question to review later"
          >
            {isCurrentQuestionMarked ? (
              <BookmarkCheck className="size-3.5 fill-current" />
            ) : (
              <Bookmark className="size-3.5" />
            )}
            <span className="hidden sm:inline">
              {isCurrentQuestionMarked ? "Marked" : "Mark for Review"}
            </span>
          </Button>

          {/* Overview Matrix Button */}
          <Button
            variant="outline"
            size="sm"
            onClick={() => setOverviewModalOpen(true)}
            className="h-8 text-xs gap-1.5 px-2.5 sm:px-3 text-muted-foreground hover:text-foreground"
            title="View assessment instructions and question overview"
          >
            <LayoutGrid className="size-3.5" />
            <span className="hidden md:inline">Overview</span>
          </Button>

          {/* Connection Dot */}
          <div
            className="flex items-center gap-1.5 text-xs text-muted-foreground"
            title={
              wsStatus.isConnected
                ? "WebSocket Live"
                : wsStatus.isConnecting
                  ? "Connecting..."
                  : "Disconnected"
            }
          >
            <div
              className={cn(
                "size-2.5 rounded-full transition-all",
                wsStatus.isConnected
                  ? "bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.6)]"
                  : wsStatus.isConnecting
                    ? "bg-amber-500 animate-pulse"
                    : "bg-destructive"
              )}
            />
          </div>

          {/* Submit Assessment Button */}
          <Button
            size="sm"
            onClick={() => setSubmitModalOpen(true)}
            className="h-8 gap-1.5 text-xs font-semibold px-3 sm:px-4"
          >
            <Send className="size-3.5" />
            <span>Submit</span>
          </Button>
        </div>
      </header>

      {/* Main Workspace Body */}
      <div className="flex-1 flex overflow-hidden">
        {/* Simplified Sidebar: Question Numbers & Status Hints ONLY */}
        <aside className="w-16 border-r border-border bg-card/30 flex flex-col items-center py-3 shrink-0 select-none">
          <div className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider mb-2">
            Q#
          </div>

          {/* Vertical rail of numbered question buttons */}
          <div className="flex-1 w-full overflow-y-auto px-2 space-y-2 flex flex-col items-center">
            {questionsList.map((q, idx) => {
              const resp = localResponses[q.questionId]
              const isAns =
                q.questionType === "MCQ"
                  ? (resp?.selectedOptionIds?.length ?? 0) > 0
                  : Boolean(resp?.code && resp.code.trim().length > 0)
              const isMarked = Boolean(resp?.markedForReview)
              const isCurrent = idx === currentIndex

              return (
                <button
                  key={q.questionId}
                  type="button"
                  onClick={() => setCurrentIndex(idx)}
                  title={`Question ${idx + 1}${isMarked ? " (Marked)" : isAns ? " (Answered)" : " (Unanswered)"}`}
                  className={cn(
                    "relative size-9 rounded-lg font-mono text-xs font-bold transition-all flex items-center justify-center cursor-pointer",
                    isCurrent
                      ? "bg-primary text-primary-foreground shadow-md ring-2 ring-primary/40"
                      : isMarked
                        ? "border border-purple-500/50 bg-purple-500/15 text-purple-600 dark:text-purple-400 hover:bg-purple-500/25"
                        : isAns
                          ? "border border-emerald-500/40 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/20"
                          : "border border-border/80 bg-muted/40 text-muted-foreground hover:bg-muted hover:text-foreground"
                  )}
                >
                  {idx + 1}
                  {/* Status dot / bookmark hint */}
                  {isMarked ? (
                    <span className="absolute -top-1 -right-1 size-2.5 rounded-full bg-purple-500 ring-2 ring-background" />
                  ) : isAns ? (
                    <span className="absolute -top-1 -right-1 size-2 rounded-full bg-emerald-500 ring-2 ring-background" />
                  ) : null}
                </button>
              )
            })}
          </div>

          {/* Sidebar Legend Hints */}
          <div className="pt-2 border-t border-border/60 w-full px-2 flex flex-col items-center gap-1.5 text-[9px] text-muted-foreground">
            <div className="flex items-center gap-1" title="Answered">
              <span className="size-2 rounded-full bg-emerald-500 shrink-0" />
              <span className="hidden sm:inline">Ans</span>
            </div>
            <div className="flex items-center gap-1" title="Marked for review">
              <span className="size-2 rounded-full bg-purple-500 shrink-0" />
              <span className="hidden sm:inline">Flag</span>
            </div>
          </div>
        </aside>

        {/* Center / Right Question Workspace */}
        <main className="flex-1 flex flex-col min-w-0 overflow-hidden bg-background">
          {currentQuestion && (
            <>
              {/* Question Subheader */}
              <div className="h-11 border-b border-border px-4 sm:px-6 flex items-center justify-between shrink-0 bg-card/40">
                <div className="flex items-center gap-2.5">
                  <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">
                    Question {currentIndex + 1} of {totalQuestionsCount}
                  </span>
                  <Badge variant="secondary" className="text-[11px] gap-1">
                    {currentQuestion.questionType === "MCQ" ? (
                      <>
                        <CheckSquare className="size-3 text-sky-500" />
                        <span>MCQ ({currentQuestion.mcqType === "MULTIPLE_CHOICE" ? "Multiple Choice" : "Single Choice"})</span>
                      </>
                    ) : (
                      <>
                        <Code2 className="size-3 text-purple-500" />
                        <span>Coding Problem</span>
                      </>
                    )}
                  </Badge>
                  <Badge variant="outline" className="text-[11px]">
                    {currentQuestion.points} Points
                  </Badge>
                  {isCurrentQuestionMarked && (
                    <Badge className="bg-purple-500/15 text-purple-600 dark:text-purple-400 border-purple-500/30 text-[11px] gap-1">
                      <Bookmark className="size-3 fill-current" />
                      <span>Marked</span>
                    </Badge>
                  )}
                </div>

                <div className="flex items-center gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs gap-1"
                    disabled={currentIndex === 0}
                    onClick={() => setCurrentIndex((p) => Math.max(0, p - 1))}
                  >
                    <ChevronLeft className="size-3.5" />
                    <span className="hidden sm:inline">Previous</span>
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs gap-1"
                    disabled={currentIndex >= totalQuestionsCount - 1}
                    onClick={() => setCurrentIndex((p) => Math.min(totalQuestionsCount - 1, p + 1))}
                  >
                    <span className="hidden sm:inline">Next</span>
                    <ChevronRight className="size-3.5" />
                  </Button>
                </div>
              </div>

              {/* CONSISTENT SPLIT-PANEL LAYOUT FOR MCQ */}
              {currentQuestion.questionType === "MCQ" && (
                <div className="flex-1 flex overflow-hidden">
                  <ResizablePanelGroup orientation="horizontal" className="h-full w-full">
                    {/* Left Panel: MCQ Problem Statement */}
                    <ResizablePanel defaultSize={50} minSize={30}>
                      <div className="h-full flex flex-col overflow-hidden border-r border-border bg-card/10">
                        <div className="p-4 sm:p-6 border-b border-border/80">
                          <h2 className="text-lg font-bold text-foreground">
                            {currentQuestion.title}
                          </h2>
                        </div>
                        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4">
                          {currentQuestion.mcqContent ? (
                            <div className="prose dark:prose-invert max-w-none text-sm text-foreground/90">
                              <MarkdownPreview content={currentQuestion.mcqContent} />
                            </div>
                          ) : (
                            <p className="text-sm text-muted-foreground italic">No question statement provided.</p>
                          )}
                        </div>
                      </div>
                    </ResizablePanel>

                    <ResizableHandle withHandle />

                    {/* Right Panel: MCQ Options List */}
                    <ResizablePanel defaultSize={50} minSize={30}>
                      <div className="h-full flex flex-col overflow-hidden bg-card/20">
                        {/* Options Header Bar */}
                        <div className="p-4 border-b border-border flex items-center justify-between shrink-0 bg-card/40">
                          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                            {currentQuestion.mcqType === "MULTIPLE_CHOICE"
                              ? "Options (Select all that apply):"
                              : "Options (Select one):"}
                          </span>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={handleClearMcqSelection}
                            className="h-7 text-xs text-muted-foreground hover:text-foreground"
                          >
                            Clear Selection
                          </Button>
                        </div>

                        {/* Options List with Markdown Rendering */}
                        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-3">
                          {currentQuestion.options?.map((opt, optIndex) => {
                            const savedResp = localResponses[currentQuestion.questionId]
                            const isSelected = Boolean(
                              savedResp?.selectedOptionIds?.includes(opt.id)
                            )
                            const optionLetter = String.fromCharCode(65 + optIndex)

                            return (
                              <button
                                key={opt.id}
                                type="button"
                                onClick={() => handleMcqOptionSelect(opt.id)}
                                className={cn(
                                  "w-full text-left p-4 rounded-xl border transition-all flex items-start gap-3.5 cursor-pointer",
                                  isSelected
                                    ? "border-primary bg-primary/10 shadow-xs ring-1 ring-primary"
                                    : "border-border/80 hover:border-primary/40 hover:bg-muted/40"
                                )}
                              >
                                <div
                                  className={cn(
                                    "flex size-6 shrink-0 items-center justify-center rounded-md border text-xs font-bold mt-0.5 transition-colors",
                                    isSelected
                                      ? "border-primary bg-primary text-primary-foreground"
                                      : "border-border bg-muted/40 text-muted-foreground"
                                  )}
                                >
                                  {isSelected ? <Check className="size-3.5 stroke-[3]" /> : optionLetter}
                                </div>
                                <div className="flex-1 text-sm leading-relaxed text-foreground min-w-0 prose dark:prose-invert max-w-none">
                                  <MarkdownPreview content={opt.content} />
                                </div>
                              </button>
                            )
                          })}
                        </div>

                        {/* Question Bottom Action Footer */}
                        <div className="p-3 border-t border-border flex items-center justify-between shrink-0 bg-card/30">
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={currentIndex === 0}
                            onClick={() => setCurrentIndex((p) => Math.max(0, p - 1))}
                            className="text-xs gap-1"
                          >
                            <ChevronLeft className="size-3.5" />
                            <span>Previous</span>
                          </Button>
                          <Button
                            variant="default"
                            size="sm"
                            disabled={currentIndex >= totalQuestionsCount - 1}
                            onClick={() => setCurrentIndex((p) => Math.min(totalQuestionsCount - 1, p + 1))}
                            className="text-xs gap-1"
                          >
                            <span>Next</span>
                            <ChevronRight className="size-3.5" />
                          </Button>
                        </div>
                      </div>
                    </ResizablePanel>
                  </ResizablePanelGroup>
                </div>
              )}

              {/* CONSISTENT SPLIT-PANEL LAYOUT FOR CODING */}
              {currentQuestion.questionType === "CODING" && (
                <div className="flex-1 flex overflow-hidden">
                  <ResizablePanelGroup orientation="horizontal" className="h-full w-full">
                    {/* Left Pane: Statement & Sample Testcases */}
                    <ResizablePanel defaultSize={45} minSize={30}>
                      <div className="h-full flex flex-col overflow-hidden border-r border-border bg-card/10">
                        <div className="p-4 border-b border-border/80 flex items-center justify-between shrink-0">
                          <h2 className="text-base font-bold text-foreground truncate">
                            {currentQuestion.title}
                          </h2>
                        </div>

                        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6">
                          {/* Statement Markdown */}
                          {currentQuestion.codingStatement && (
                            <div className="prose dark:prose-invert max-w-none text-sm text-foreground/90">
                              <MarkdownPreview content={currentQuestion.codingStatement} />
                            </div>
                          )}

                          {/* Public Sample Testcases */}
                          {currentQuestion.sampleTestCases &&
                            currentQuestion.sampleTestCases.length > 0 && (
                              <div className="space-y-4 pt-4 border-t border-border">
                                <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
                                  Sample Test Cases
                                </h3>
                                <div className="space-y-3">
                                  {currentQuestion.sampleTestCases.map((tc, idx) => (
                                    <div
                                      key={tc.id || idx}
                                      className="rounded-lg border border-border/80 bg-muted/20 p-3 space-y-2 text-xs"
                                    >
                                      <span className="font-semibold text-muted-foreground">
                                        Sample #{idx + 1}
                                      </span>
                                      {tc.stdin !== undefined && (
                                        <div>
                                          <p className="text-[11px] text-muted-foreground font-mono">
                                            Input:
                                          </p>
                                          <pre className="p-2 rounded bg-background/80 border font-mono text-[11px] mt-1 whitespace-pre-wrap">
                                            {tc.stdin || "(empty input)"}
                                          </pre>
                                        </div>
                                      )}
                                      {tc.output !== undefined && (
                                        <div>
                                          <p className="text-[11px] text-muted-foreground font-mono">
                                            Expected Output:
                                          </p>
                                          <pre className="p-2 rounded bg-background/80 border font-mono text-[11px] mt-1 whitespace-pre-wrap">
                                            {tc.output}
                                          </pre>
                                        </div>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              </div>
                            )}
                        </div>

                        {/* Left Pane Navigation Footer */}
                        <div className="p-3 border-t border-border flex items-center justify-between shrink-0 bg-card/30">
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={currentIndex === 0}
                            onClick={() => setCurrentIndex((p) => Math.max(0, p - 1))}
                            className="text-xs gap-1"
                          >
                            <ChevronLeft className="size-3.5" />
                            <span>Previous</span>
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={currentIndex >= totalQuestionsCount - 1}
                            onClick={() => setCurrentIndex((p) => Math.min(totalQuestionsCount - 1, p + 1))}
                            className="text-xs gap-1"
                          >
                            <span>Next</span>
                            <ChevronRight className="size-3.5" />
                          </Button>
                        </div>
                      </div>
                    </ResizablePanel>

                    <ResizableHandle withHandle />

                    {/* Right Pane: Monaco Editor & Redesigned Console */}
                    <ResizablePanel defaultSize={55} minSize={35}>
                      <div className="h-full flex flex-col overflow-hidden">
                        {/* Editor Controls Bar */}
                        <div className="h-11 border-b border-border/80 px-4 flex items-center justify-between shrink-0 bg-card/60">
                          <div className="flex items-center gap-2">
                            <span className="text-xs text-muted-foreground font-medium">
                              Language:
                            </span>
                            <NativeSelect
                              value={currentLanguage}
                              onChange={(e) => handleLanguageChange(e.target.value)}
                              className="h-8 text-xs font-mono w-44"
                            >
                              {(currentQuestion.supportedLanguageIds && currentQuestion.supportedLanguageIds.length > 0
                                ? currentQuestion.supportedLanguageIds
                                : ["java-21"]
                              ).map((langId) => {
                                const cfg = getLanguageById(langId)
                                return (
                                  <NativeSelectOption key={langId} value={langId}>
                                    {cfg ? cfg.name : langId}
                                  </NativeSelectOption>
                                )
                              })}
                            </NativeSelect>
                          </div>

                          <div className="flex items-center gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={handleResetStarterCode}
                              className="h-7 text-xs gap-1 text-muted-foreground hover:text-foreground"
                            >
                              <RotateCcw className="size-3" />
                              <span>Reset</span>
                            </Button>

                            <Button
                              variant="outline"
                              size="sm"
                              disabled={runSampleMutation.isPending}
                              onClick={handleRunSampleTests}
                              className="h-7 text-xs gap-1.5"
                            >
                              {runSampleMutation.isPending ? (
                                <Loader2 className="size-3 animate-spin text-primary" />
                              ) : (
                                <Play className="size-3 text-emerald-500 fill-emerald-500" />
                              )}
                              <span>Run Tests</span>
                            </Button>

                            <Button
                              size="sm"
                              disabled={submitCodeMutation.isPending}
                              onClick={handleSubmitCode}
                              className="h-7 text-xs gap-1.5 font-semibold"
                            >
                              {submitCodeMutation.isPending ? (
                                <Loader2 className="size-3 animate-spin" />
                              ) : (
                                <CheckCircle2 className="size-3" />
                              )}
                              <span>Submit Code</span>
                            </Button>
                          </div>
                        </div>

                        {/* Monaco Editor Split with Redesigned Bottom Panel */}
                        <ResizablePanelGroup orientation="vertical" className="flex-1">
                          <ResizablePanel defaultSize={60} minSize={25}>
                            <div className="h-full w-full">
                              <MonacoEditor
                                language={resolveMonacoLanguage(currentLanguage)}
                                value={currentCode}
                                onChange={handleCodeChange}
                                height="100%"
                                options={{
                                  minimap: { enabled: false },
                                  fontSize: 13,
                                  lineNumbers: "on",
                                  scrollBeyondLastLine: false,
                                  automaticLayout: true,
                                }}
                              />
                            </div>
                          </ResizablePanel>

                          <ResizableHandle withHandle />

                          {/* Redesigned Testcase / Console Panel */}
                          <ResizablePanel defaultSize={40} minSize={20}>
                            <div className="h-full flex flex-col border-t border-border bg-card/40">
                              {/* Console Tab Header */}
                              <div className="h-9 border-b border-border/80 px-4 flex items-center gap-6 text-xs font-semibold shrink-0">
                                <button
                                  type="button"
                                  onClick={() => setActiveBottomTab("sample")}
                                  className={cn(
                                    "pb-1 border-b-2 transition-colors cursor-pointer flex items-center gap-1.5",
                                    activeBottomTab === "sample"
                                      ? "border-primary text-foreground"
                                      : "border-transparent text-muted-foreground hover:text-foreground"
                                  )}
                                >
                                  <FileCode className="size-3.5" />
                                  <span>Sample Testcases</span>
                                </button>
                                <button
                                  type="button"
                                  onClick={() => setActiveBottomTab("custom")}
                                  className={cn(
                                    "pb-1 border-b-2 transition-colors cursor-pointer flex items-center gap-1.5",
                                    activeBottomTab === "custom"
                                      ? "border-primary text-foreground"
                                      : "border-transparent text-muted-foreground hover:text-foreground"
                                  )}
                                >
                                  <Terminal className="size-3.5" />
                                  <span>Custom Input</span>
                                </button>
                                <button
                                  type="button"
                                  onClick={() => setActiveBottomTab("submit")}
                                  className={cn(
                                    "pb-1 border-b-2 transition-colors cursor-pointer flex items-center gap-1.5",
                                    activeBottomTab === "submit"
                                      ? "border-primary text-foreground"
                                      : "border-transparent text-muted-foreground hover:text-foreground"
                                  )}
                                >
                                  <CheckCircle2 className="size-3.5" />
                                  <span>Submission Output</span>
                                </button>
                              </div>

                              <div className="flex-1 overflow-y-auto p-4 text-xs">
                                {/* TAB 1: Sample Testcases with Case Selectors */}
                                {activeBottomTab === "sample" && (
                                  <div>
                                    {runSampleMutation.isPending ? (
                                      <div className="flex items-center gap-2 text-muted-foreground py-6">
                                        <Loader2 className="size-4 animate-spin text-primary" />
                                        <span>Executing code against sample test cases in sandbox...</span>
                                      </div>
                                    ) : sampleResult ? (
                                      <div className="space-y-4">
                                        {/* Status Header */}
                                        <div className="flex items-center justify-between pb-2 border-b border-border/60">
                                          <div className="flex items-center gap-2">
                                            <Badge
                                              variant={
                                                sampleResult.overallStatus === "SUCCESS"
                                                  ? "default"
                                                  : "destructive"
                                              }
                                              className={cn(
                                                "text-xs font-semibold",
                                                sampleResult.overallStatus === "SUCCESS"
                                                  ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                                                  : "bg-destructive/15 text-destructive border-destructive/30"
                                              )}
                                            >
                                              {sampleResult.overallStatus}
                                            </Badge>
                                            <span className="text-muted-foreground font-medium">
                                              Passed {sampleResult.passedTestcases} / {sampleResult.totalTestcases} test cases
                                            </span>
                                          </div>
                                          <span className="text-muted-foreground font-mono">
                                            {sampleResult.totalExecutionTimeMs} ms
                                          </span>
                                        </div>

                                        {/* Testcase Pill Selectors */}
                                        <div className="flex items-center gap-2 overflow-x-auto pb-1">
                                          {sampleResult.results?.map((res, idx) => (
                                            <button
                                              key={idx}
                                              type="button"
                                              onClick={() => setSelectedSampleIndex(idx)}
                                              className={cn(
                                                "px-3 py-1.5 rounded-lg border text-xs font-medium transition-all flex items-center gap-1.5 cursor-pointer",
                                                selectedSampleIndex === idx
                                                  ? "border-primary bg-primary/10 text-foreground ring-1 ring-primary"
                                                  : "border-border/80 hover:bg-muted/40 text-muted-foreground"
                                              )}
                                            >
                                              <span
                                                className={cn(
                                                  "size-2 rounded-full",
                                                  res.passed ? "bg-emerald-500" : "bg-destructive"
                                                )}
                                              />
                                              <span>Case {idx + 1}</span>
                                            </button>
                                          ))}
                                        </div>

                                        {/* Selected Case Detail */}
                                        {sampleResult.results?.[selectedSampleIndex] && (() => {
                                          const res = sampleResult.results[selectedSampleIndex]
                                          return (
                                            <div className="space-y-3 pt-2">
                                              <div className="flex items-center justify-between">
                                                <Badge
                                                  variant={res.passed ? "default" : "destructive"}
                                                  className={cn(
                                                    "text-[11px]",
                                                    res.passed
                                                      ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                                                      : "bg-destructive/15 text-destructive border-destructive/30"
                                                  )}
                                                >
                                                  {res.status}
                                                </Badge>
                                                <span className="text-[11px] text-muted-foreground font-mono">
                                                  {res.runtimeMs ?? res.executionTimeMs ?? 0} ms
                                                </span>
                                              </div>

                                              {/* Formatted Diff Cards */}
                                              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                                <div className="space-y-1">
                                                  <span className="text-[11px] font-semibold text-muted-foreground">
                                                    Your Output:
                                                  </span>
                                                  <pre className="font-mono p-2.5 bg-background border rounded-lg text-xs whitespace-pre-wrap min-h-[60px]">
                                                    {res.actualOutput || "(empty)"}
                                                  </pre>
                                                </div>

                                                <div className="space-y-1">
                                                  <span className="text-[11px] font-semibold text-muted-foreground">
                                                    Expected Output:
                                                  </span>
                                                  <pre className="font-mono p-2.5 bg-background border rounded-lg text-xs whitespace-pre-wrap min-h-[60px]">
                                                    {res.expectedOutput || "(empty)"}
                                                  </pre>
                                                </div>
                                              </div>

                                              {/* Stderr / Error Message Card */}
                                              {res.stderr && (
                                                <div className="space-y-1 pt-1">
                                                  <span className="text-[11px] font-semibold text-destructive">
                                                    Error / Compiler / Stderr Output:
                                                  </span>
                                                  <pre className="font-mono p-2.5 bg-destructive/10 text-destructive border border-destructive/30 rounded-lg text-xs whitespace-pre-wrap">
                                                    {res.stderr}
                                                  </pre>
                                                </div>
                                              )}
                                            </div>
                                          )
                                        })()}
                                      </div>
                                    ) : (
                                      <p className="text-muted-foreground py-6 text-center">
                                        Click "Run Tests" above to execute your solution against sample test cases.
                                      </p>
                                    )}
                                  </div>
                                )}

                                {/* TAB 2: Custom Input Runner */}
                                {activeBottomTab === "custom" && (
                                  <div className="space-y-4">
                                    <div className="space-y-2">
                                      <div className="flex items-center justify-between">
                                        <label className="text-xs font-semibold text-foreground">
                                          Standard Input (stdin):
                                        </label>
                                        <Button
                                          size="sm"
                                          variant="outline"
                                          onClick={handleRunCustomInput}
                                          disabled={runCustomInputMutation.isPending}
                                          className="h-7 text-xs gap-1.5"
                                        >
                                          {runCustomInputMutation.isPending ? (
                                            <Loader2 className="size-3 animate-spin text-primary" />
                                          ) : (
                                            <Play className="size-3 text-emerald-500 fill-emerald-500" />
                                          )}
                                          <span>Run Custom Input</span>
                                        </Button>
                                      </div>
                                      <Textarea
                                        placeholder="Enter standard input values here to test edge cases..."
                                        rows={3}
                                        value={customInputText}
                                        onChange={(e) => setCustomInputText(e.target.value)}
                                        className="font-mono text-xs"
                                      />
                                    </div>

                                    {/* Custom Output Result */}
                                    {runCustomInputMutation.isPending ? (
                                      <div className="flex items-center gap-2 text-muted-foreground py-3">
                                        <Loader2 className="size-4 animate-spin text-primary" />
                                        <span>Executing code with custom input in sandbox...</span>
                                      </div>
                                    ) : customResult ? (
                                      <div className="rounded-lg border p-3 bg-background space-y-3">
                                        <div className="flex items-center justify-between pb-2 border-b">
                                          <Badge
                                            className={cn(
                                              "text-[10px]",
                                              customResult.overallStatus === "SUCCESS"
                                                ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                                                : "bg-destructive/15 text-destructive border-destructive/30"
                                            )}
                                          >
                                            {customResult.overallStatus}
                                          </Badge>
                                          <span className="text-muted-foreground font-mono">
                                            {customResult.totalExecutionTimeMs} ms
                                          </span>
                                        </div>

                                        {customResult.results?.[0]?.stdout !== undefined && (
                                          <div>
                                            <span className="text-muted-foreground font-semibold">Standard Output:</span>
                                            <pre className="font-mono p-2 bg-muted rounded-md text-xs mt-1 whitespace-pre-wrap">
                                              {customResult.results[0].stdout || "(empty output)"}
                                            </pre>
                                          </div>
                                        )}

                                        {customResult.results?.[0]?.stderr && (
                                          <div>
                                            <span className="text-destructive font-semibold">Stderr / Runtime Error:</span>
                                            <pre className="font-mono p-2 bg-destructive/10 text-destructive rounded-md text-xs mt-1 whitespace-pre-wrap">
                                              {customResult.results[0].stderr}
                                            </pre>
                                          </div>
                                        )}
                                      </div>
                                    ) : (
                                      <p className="text-muted-foreground py-3 text-xs">
                                        Provide standard input above and click "Run Custom Input".
                                      </p>
                                    )}
                                  </div>
                                )}

                                {/* TAB 3: Submission Output */}
                                {activeBottomTab === "submit" && (
                                  <div>
                                    {submitCodeMutation.isPending ? (
                                      <div className="flex items-center gap-2 text-muted-foreground py-6">
                                        <Loader2 className="size-4 animate-spin text-primary" />
                                        <span>Evaluating code against complete test suite...</span>
                                      </div>
                                    ) : submitResult ? (
                                      <div className="space-y-3">
                                        <div className="flex items-center gap-2">
                                          <Badge
                                            className={cn(
                                              "text-xs font-semibold py-1 px-2.5",
                                              submitResult.allTestcasesPassed
                                                ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30"
                                                : "bg-destructive/15 text-destructive border-destructive/30"
                                            )}
                                          >
                                            {submitResult.allTestcasesPassed
                                              ? "PASSED ALL TEST CASES"
                                              : "TEST CASES FAILED"}
                                          </Badge>
                                        </div>
                                        <p className="text-muted-foreground text-xs leading-relaxed">
                                          Your code solution has been compiled, evaluated, and securely recorded with your assessment attempt.
                                        </p>
                                      </div>
                                    ) : (
                                      <p className="text-muted-foreground py-6 text-center">
                                        Click "Submit Code" to evaluate your solution against the judging suite.
                                      </p>
                                    )}
                                  </div>
                                )}
                              </div>
                            </div>
                          </ResizablePanel>
                        </ResizablePanelGroup>
                      </div>
                    </ResizablePanel>
                  </ResizablePanelGroup>
                </div>
              )}
            </>
          )}
        </main>
      </div>

      {/* Assessment Question Matrix Overview Modal */}
      <Dialog open={overviewModalOpen} onOpenChange={setOverviewModalOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <LayoutGrid className="size-5 text-primary" />
              <span>Assessment Overview</span>
            </DialogTitle>
            <DialogDescription>
              Review assessment details and jump to any question.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-2">
            {/* Assessment Metadata & Instructions */}
            <div className="p-3.5 rounded-lg border bg-muted/20 space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-foreground">{attempt.title}</span>
                <span className="text-muted-foreground font-mono">
                  {formatTimer(remainingSeconds)} remaining
                </span>
              </div>
              {attempt.description && (
                <p className="text-muted-foreground leading-relaxed">
                  {attempt.description}
                </p>
              )}
            </div>

            {/* Metric Chips */}
            <div className="grid grid-cols-3 gap-2 text-center">
              <div className="p-2.5 rounded-lg border bg-emerald-500/5 border-emerald-500/20">
                <div className="text-[11px] text-muted-foreground">Answered</div>
                <div className="text-lg font-bold text-emerald-600 dark:text-emerald-400">
                  {answeredCount}
                </div>
              </div>
              <div className="p-2.5 rounded-lg border bg-amber-500/5 border-amber-500/20">
                <div className="text-[11px] text-muted-foreground">Unanswered</div>
                <div className="text-lg font-bold text-amber-500">{unansweredCount}</div>
              </div>
              <div className="p-2.5 rounded-lg border bg-purple-500/5 border-purple-500/20">
                <div className="text-[11px] text-muted-foreground">Marked for Review</div>
                <div className="text-lg font-bold text-purple-600 dark:text-purple-400">
                  {markedCount}
                </div>
              </div>
            </div>

            {/* Questions Grid Matrix */}
            <div className="grid grid-cols-6 sm:grid-cols-8 gap-2 max-h-60 overflow-y-auto p-1">
              {questionsList.map((q, idx) => {
                const resp = localResponses[q.questionId]
                const isAns =
                  q.questionType === "MCQ"
                    ? (resp?.selectedOptionIds?.length ?? 0) > 0
                    : Boolean(resp?.code && resp.code.trim().length > 0)
                const isMarked = Boolean(resp?.markedForReview)
                const isCurrent = idx === currentIndex

                return (
                  <button
                    key={q.questionId}
                    type="button"
                    onClick={() => {
                      setCurrentIndex(idx)
                      setOverviewModalOpen(false)
                    }}
                    className={cn(
                      "flex flex-col items-center justify-center p-2 rounded-lg border text-xs font-bold transition-all relative cursor-pointer",
                      isCurrent ? "ring-2 ring-primary" : "",
                      isMarked
                        ? "border-purple-500 bg-purple-500/15 text-purple-600 dark:text-purple-400"
                        : isAns
                          ? "border-emerald-500 bg-emerald-500/15 text-emerald-600 dark:text-emerald-400"
                          : "border-border bg-muted/30 text-muted-foreground hover:bg-muted/60"
                    )}
                  >
                    <span>{idx + 1}</span>
                    <span className="text-[8px] font-normal uppercase mt-0.5">
                      {q.questionType === "CODING" ? "Code" : "MCQ"}
                    </span>
                    {isMarked && (
                      <div className="absolute top-1 right-1">
                        <Bookmark className="size-2 text-purple-500 fill-purple-500" />
                      </div>
                    )}
                  </button>
                )
              })}
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setOverviewModalOpen(false)}>
              Close
            </Button>
            <Button
              onClick={() => {
                setOverviewModalOpen(false)
                setSubmitModalOpen(true)
              }}
              className="gap-1.5"
            >
              <Send className="size-3.5" />
              <span>Submit Assessment</span>
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Final Submit Confirmation Modal */}
      <Dialog open={submitModalOpen} onOpenChange={setSubmitModalOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Send className="size-5 text-primary" />
              <span>Submit Assessment?</span>
            </DialogTitle>
            <DialogDescription>
              Are you sure you want to finish and submit your assessment? Once submitted, your answers cannot be modified.
            </DialogDescription>
          </DialogHeader>

          <div className="py-2 space-y-2 text-xs">
            <div className="flex justify-between py-1 border-b">
              <span className="text-muted-foreground">Answered Questions:</span>
              <span className="font-bold text-foreground">
                {answeredCount} / {totalQuestionsCount}
              </span>
            </div>
            <div className="flex justify-between py-1 border-b">
              <span className="text-muted-foreground">Unanswered Questions:</span>
              <span
                className={cn(
                  "font-bold",
                  unansweredCount > 0 ? "text-amber-500" : "text-foreground"
                )}
              >
                {unansweredCount}
              </span>
            </div>
            <div className="flex justify-between py-1 border-b">
              <span className="text-muted-foreground">Marked for Review:</span>
              <span className="font-bold text-purple-600 dark:text-purple-400">
                {markedCount}
              </span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-muted-foreground">Time Remaining:</span>
              <span className="font-bold text-foreground font-mono">
                {formatTimer(remainingSeconds)}
              </span>
            </div>

            {unansweredCount > 0 && (
              <div className="p-2.5 rounded-lg border border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400 mt-2">
                You have {unansweredCount} unanswered question(s). You can still return to complete them before the timer runs out.
              </div>
            )}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setSubmitModalOpen(false)}>
              Keep Working
            </Button>
            <Button
              onClick={handleConfirmFinalSubmit}
              disabled={finalSubmitMutation.isPending}
              className="gap-2 font-semibold"
            >
              {finalSubmitMutation.isPending && (
                <Loader2 className="size-4 animate-spin" />
              )}
              <span>Confirm & Submit</span>
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Time Expired Modal */}
      <Dialog open={timeExpiredModalOpen} onOpenChange={() => {}}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-destructive">
              <Clock className="size-5" />
              <span>Time Has Expired</span>
            </DialogTitle>
            <DialogDescription>
              The allotted time for this assessment has concluded. All saved answers have been preserved.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              onClick={() => {
                setTimeExpiredModalOpen(false)
                handleConfirmFinalSubmit()
              }}
              className="w-full"
            >
              Finalize & View Results
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
