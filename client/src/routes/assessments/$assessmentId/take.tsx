import { useState, useEffect, useRef, useMemo, useCallback } from "react"
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import axios from "axios"

import {
  ResizablePanelGroup,
  ResizablePanel,
  ResizableHandle,
} from "@/components/ui/resizable"
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
} from "@/hooks/use-assessments"
import { resolveStarterCode } from "@/lib/monaco-languages"
import { toast } from "@/components/ui/toast"
import { PageLoadingState } from "@/components/common/page-loading-state"
import { PageErrorState } from "@/components/common/page-error-state"

import { useExamTelemetry } from "@/components/assessment-exam/hooks/use-exam-telemetry"
import { ExamHeader } from "@/components/assessment-exam/exam-header"
import { ExamSidebar } from "@/components/assessment-exam/exam-sidebar"
import { ExamStatement } from "@/components/assessment-exam/exam-statement"
import { ExamMcqPanel } from "@/components/assessment-exam/exam-mcq-panel"
import { ExamCodeWorkspace } from "@/components/assessment-exam/exam-code-workspace"
import { ExamBottomTabs } from "@/components/assessment-exam/exam-bottom-tabs"
import { ExamNavigationFooter } from "@/components/assessment-exam/exam-navigation-footer"
import { ExamOverviewDialog } from "@/components/assessment-exam/dialogs/exam-overview-dialog"
import { ExamSubmitDialog } from "@/components/assessment-exam/dialogs/exam-submit-dialog"
import {
  ExamExpiredDialog,
  ExamManualCompletionDialog,
} from "@/components/assessment-exam/dialogs/exam-alert-dialogs"

import type {
  AssessmentItemResponse,
  CodingProblemExecutionResponse,
  SavedQuestionResponse,
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
    refetch,
  } = useActiveAttempt(assessmentId)

  const saveMcqMutation = useSaveMcqResponse(assessmentId)
  const saveCodeMutation = useSaveCodeResponse(assessmentId)
  const runSampleMutation = useRunSample(assessmentId)
  const runCustomInputMutation = useRunCustomInput(assessmentId)
  const submitCodeMutation = useSubmitCodeResponse(assessmentId)
  const markForReviewMutation = useMarkQuestionForReview(assessmentId)
  const finalSubmitMutation = useFinalSubmitAssessment(assessmentId)

  // Question navigation state
  const [currentIndex, setCurrentIndex] = useState(0)
  const questionsList = useMemo(() => attempt?.questions ?? [], [attempt?.questions])
  const totalQuestionsCount = questionsList.length
  const currentQuestion: AssessmentItemResponse | undefined = questionsList[currentIndex]

  // Local responses & save status
  const [localResponses, setLocalResponses] = useState<Record<string, SavedQuestionResponse>>({})
  const [saveStatus, setSaveStatus] = useState<"SAVED" | "SAVING" | "UNSAVED">("SAVED")
  const loadedAttemptIdRef = useRef<string | null>(null)

  // Code workspace state
  const [currentLanguage, setCurrentLanguage] = useState<string>("java-21")
  const [currentCode, setCurrentCode] = useState<string>("")
  const codeRef = useRef<string>("")
  const [activeBottomTab, setActiveBottomTab] = useState<"sample" | "custom" | "submit">("sample")
  const [selectedSampleIndex, setSelectedSampleIndex] = useState<number>(0)
  const [customInputText, setCustomInputText] = useState<string>("")
  const [sampleResult, setSampleResult] = useState<CodingProblemExecutionResponse | null>(null)
  const [customResult, setCustomResult] = useState<CodingProblemExecutionResponse | null>(null)
  const [submitResult, setSubmitResult] = useState<SavedQuestionResponse | null>(null)

  // Dialog states
  const [submitModalOpen, setSubmitModalOpen] = useState(false)
  const [overviewModalOpen, setOverviewModalOpen] = useState(false)
  const [timeExpiredModalOpen, setTimeExpiredModalOpen] = useState(false)
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null)
  const [manualInterventionModal, setManualInterventionModal] = useState<string | null>(null)

  // Telemetry Hook
  const { recordEvent } = useExamTelemetry({
    assessmentId,
    attemptId: attempt?.attemptId,
    currentQuestionId: currentQuestion?.questionId,
    enabled: Boolean(attempt && attempt.status === "IN_PROGRESS"),
  })

  // WebSocket for real-time proctoring / remote admin completion
  const handleManualCompletion = useCallback((message: string) => {
    setManualInterventionModal(message)
  }, [])

  const handleManualActivation = useCallback((message: string) => {
    toast.add({
      title: "Assessment Active",
      description: message,
      type: "info",
    })
  }, [])

  useAssessmentWebSocket({
    assessmentId,
    attemptId: attempt?.attemptId || "",
    enabled: Boolean(attempt && attempt.status === "IN_PROGRESS"),
    onManualCompletion: handleManualCompletion,
    onManualActivation: handleManualActivation,
  })

  // Detect manual completion if reconnecting after completion
  useEffect(() => {
    if (attempt?.manuallyCompleted && !manualInterventionModal) {
      setManualInterventionModal(
        attempt.manualInterventionMessage ||
          "The administrator has manually completed this assessment."
      )
    }
  }, [attempt?.manuallyCompleted, attempt?.manualInterventionMessage, manualInterventionModal])

  // If already submitted or expired, navigate to results
  useEffect(() => {
    if (
      !manualInterventionModal &&
      (attempt?.status === "SUBMITTED" || attempt?.status === "EXPIRED")
    ) {
      navigate({
        to: "/assessments/$assessmentId/results",
        params: { assessmentId },
      })
    }
  }, [attempt?.status, assessmentId, navigate, manualInterventionModal])

  // Timer Tick
  useEffect(() => {
    if (attempt?.remainingSeconds !== undefined && attempt.remainingSeconds !== null) {
      setRemainingSeconds(attempt.remainingSeconds)
    }
  }, [attempt?.remainingSeconds])

  useEffect(() => {
    if (remainingSeconds === null || remainingSeconds <= 0) return

    const timer = setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev === null) return null
        if (prev <= 1) {
          clearInterval(timer)
          setTimeExpiredModalOpen(true)
          finalSubmitMutation.mutate(undefined, {
            onSettled: () => {
              navigate({
                to: "/assessments/$assessmentId/results",
                params: { assessmentId },
              })
            },
          })
          return 0
        }
        return prev - 1
      })
    }, 1000)

    return () => clearInterval(timer)
  }, [remainingSeconds, finalSubmitMutation, navigate, assessmentId])

  // Populate attempt responses on first load
  useEffect(() => {
    if (attempt && attempt.attemptId !== loadedAttemptIdRef.current) {
      loadedAttemptIdRef.current = attempt.attemptId
      if (attempt.responses) {
        setLocalResponses(attempt.responses)
      }
    }
  }, [attempt])

  // Reset editor state when switching questions
  useEffect(() => {
    if (!currentQuestion) return

    if (currentQuestion.questionType === "CODING") {
      const savedResp = localResponses[currentQuestion.questionId]
      const savedLang = savedResp?.languageId
      const savedCode = savedResp?.code

      const selectedLang =
        savedLang ||
        currentQuestion.supportedLanguageIds?.[0] ||
        "java-21"
      setCurrentLanguage(selectedLang)

      const resolvedStarter = resolveStarterCode(
        currentQuestion.starterCode,
        selectedLang
      )
      const defaultStarter =
        resolvedStarter || `// Write your solution in ${selectedLang}\n`

      const initialCode = savedCode !== undefined ? savedCode : defaultStarter
      codeRef.current = initialCode
      setCurrentCode(initialCode)
      setSampleResult(null)
      setCustomResult(null)
      setSubmitResult(
        savedResp?.submissions?.length || savedResp?.lastExecutionResult
          ? savedResp
          : null
      )
      setCustomInputText("")
      setSelectedSampleIndex(0)
    }
  }, [currentIndex, currentQuestion, localResponses])

  // Debounced auto-save for code
  const codeSaveTimeoutRef = useRef<number | null>(null)

  const handleCodeChange = (newCode: string) => {
    codeRef.current = newCode
    setSaveStatus("SAVING")

    if (codeSaveTimeoutRef.current) {
      window.clearTimeout(codeSaveTimeoutRef.current)
    }

    codeSaveTimeoutRef.current = window.setTimeout(async () => {
      if (!currentQuestion) return
      try {
        const saved = await saveCodeMutation.mutateAsync({
          questionId: currentQuestion.questionId,
          languageId: currentLanguage,
          code: newCode,
        })
        setLocalResponses((prev) => ({
          ...prev,
          [currentQuestion.questionId]: saved,
        }))
        setSaveStatus("SAVED")
      } catch {
        setSaveStatus("UNSAVED")
      }
    }, 1000)
  }

  // Language switch handler
  const handleLanguageChange = (lang: string) => {
    setCurrentLanguage(lang)
    const starter =
      resolveStarterCode(currentQuestion?.starterCode, lang) ||
      `// Write your solution in ${lang}\n`
    codeRef.current = starter
    setCurrentCode(starter)
    handleCodeChange(starter)
  }

  // Reset starter code
  const handleResetStarter = () => {
    if (!currentQuestion) return
    const starter =
      resolveStarterCode(currentQuestion.starterCode, currentLanguage) ||
      `// Write your solution in ${currentLanguage}\n`
    codeRef.current = starter
    setCurrentCode(starter)
    handleCodeChange(starter)
    toast.add({
      title: "Code Reset",
      description: "Editor restored to initial starter template.",
      type: "info",
    })
  }

  // MCQ Selection handler
  const handleOptionToggle = async (optionId: string) => {
    if (!currentQuestion) return
    const existingResp = localResponses[currentQuestion.questionId]
    const existingOptions = existingResp?.selectedOptionIds || []

    let updatedOptions: string[]
    if (currentQuestion.mcqType === "MULTIPLE_CHOICE") {
      if (existingOptions.includes(optionId)) {
        updatedOptions = existingOptions.filter((id) => id !== optionId)
      } else {
        updatedOptions = [...existingOptions, optionId]
      }
    } else {
      updatedOptions = [optionId]
    }

    setLocalResponses((prev) => ({
      ...prev,
      [currentQuestion.questionId]: {
        ...prev[currentQuestion.questionId],
        questionId: currentQuestion.questionId,
        selectedOptionIds: updatedOptions,
        savedAt: new Date().toISOString(),
      },
    }))

    setSaveStatus("SAVING")
    try {
      const saved = await saveMcqMutation.mutateAsync({
        questionId: currentQuestion.questionId,
        selectedOptionIds: updatedOptions,
      })
      setLocalResponses((prev) => ({
        ...prev,
        [currentQuestion.questionId]: saved,
      }))
      setSaveStatus("SAVED")
      recordEvent("MCQ_ANSWER_CHANGED", currentQuestion.questionId, {
        optionIds: updatedOptions,
      })
    } catch {
      setSaveStatus("UNSAVED")
    }
  }

  // Clear MCQ options
  const handleClearOptions = async () => {
    if (!currentQuestion) return
    setLocalResponses((prev) => ({
      ...prev,
      [currentQuestion.questionId]: {
        ...prev[currentQuestion.questionId],
        questionId: currentQuestion.questionId,
        selectedOptionIds: [],
        savedAt: new Date().toISOString(),
      },
    }))

    setSaveStatus("SAVING")
    try {
      const saved = await saveMcqMutation.mutateAsync({
        questionId: currentQuestion.questionId,
        selectedOptionIds: [],
      })
      setLocalResponses((prev) => ({
        ...prev,
        [currentQuestion.questionId]: saved,
      }))
      setSaveStatus("SAVED")
    } catch {
      setSaveStatus("UNSAVED")
    }
  }

  // Mark for review toggle
  const handleToggleMark = async () => {
    if (!currentQuestion) return
    const currentMarked = Boolean(
      localResponses[currentQuestion.questionId]?.markedForReview
    )
    const newMarked = !currentMarked

    setLocalResponses((prev) => ({
      ...prev,
      [currentQuestion.questionId]: {
        ...prev[currentQuestion.questionId],
        questionId: currentQuestion.questionId,
        markedForReview: newMarked,
        savedAt: new Date().toISOString(),
      },
    }))

    try {
      const updated = await markForReviewMutation.mutateAsync({
        questionId: currentQuestion.questionId,
        markedForReview: newMarked,
      })
      setLocalResponses((prev) => ({
        ...prev,
        [currentQuestion.questionId]: updated,
      }))
    } catch {
      // rollback on error
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
  const handleRunTests = async () => {
    if (!currentQuestion) return
    if (activeBottomTab === "custom") {
      handleRunCustomInput()
      return
    }

    setActiveBottomTab("sample")
    try {
      const res = await runSampleMutation.mutateAsync({
        questionId: currentQuestion.questionId,
        languageId: currentLanguage,
        code: codeRef.current || currentCode,
      })
      setSampleResult(res)
      setSelectedSampleIndex(0)
    } catch {
      // handled by mutation error
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
        code: codeRef.current || currentCode,
        stdin: customInputText,
      })
      setCustomResult(res)
    } catch (err: unknown) {
      const msg = axios.isAxiosError(err)
        ? (err.response?.data as { message?: string })?.message ||
          "Execution error on custom input"
        : err instanceof Error
          ? err.message
          : "Execution failed"
      toast.add({
        title: "Custom Input Failed",
        description: msg,
        type: "error",
      })
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
        code: codeRef.current || currentCode,
      })
      setSubmitResult(res)
      setLocalResponses((prev) => ({
        ...prev,
        [currentQuestion.questionId]: res,
      }))
      recordEvent("CODE_SUBMITTED", currentQuestion.questionId, {
        languageId: currentLanguage,
        passed: res.allTestcasesPassed,
      })
    } catch {
      // handled by mutation error
    }
  }

  // Final submit handler
  const handleConfirmFinalSubmit = async () => {
    try {
      await finalSubmitMutation.mutateAsync()
      setSubmitModalOpen(false)
      navigate({
        to: "/assessments/$assessmentId/results",
        params: { assessmentId },
      })
    } catch {
      // handled by mutation error
    }
  }

  // Answered checker helper
  const isQuestionAnswered = useCallback(
    (q: AssessmentItemResponse, resp: SavedQuestionResponse | undefined) => {
      if (!resp) return false
      if (q.questionType === "MCQ") {
        return (resp.selectedOptionIds?.length ?? 0) > 0
      }
      if (q.questionType === "CODING") {
        if ((resp.submissions?.length ?? 0) > 0 || resp.lastExecutionResult) {
          return true
        }
        if (resp.code && resp.code.trim().length > 0) {
          const starter = resolveStarterCode(
            q.starterCode,
            resp.languageId || currentLanguage
          )
          if (!starter) return true
          const normalize = (s: string) => s.replace(/\s+/g, "").trim()
          return normalize(resp.code) !== normalize(starter)
        }
        return false
      }
      return false
    },
    [currentLanguage]
  )

  // Stats calculation
  const answeredCount = useMemo(() => {
    return questionsList.filter((q) => {
      const resp = localResponses[q.questionId]
      return isQuestionAnswered(q, resp)
    }).length
  }, [questionsList, localResponses, isQuestionAnswered])

  const markedCount = useMemo(() => {
    return questionsList.filter((q) => {
      return Boolean(localResponses[q.questionId]?.markedForReview)
    }).length
  }, [questionsList, localResponses])

  const unansweredCount = Math.max(0, totalQuestionsCount - answeredCount)
  const isCurrentQuestionMarked = currentQuestion
    ? Boolean(localResponses[currentQuestion.questionId]?.markedForReview)
    : false

  const formatTimer = (totalSec: number | null) => {
    if (totalSec === null || totalSec < 0) return "--:--"
    const hours = Math.floor(totalSec / 3600)
    const minutes = Math.floor((totalSec % 3600) / 60)
    const seconds = totalSec % 60
    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, "0")}:${seconds
        .toString()
        .padStart(2, "0")}`
    }
    return `${minutes.toString().padStart(2, "0")}:${seconds
      .toString()
      .padStart(2, "0")}`
  }

  // Loading & Error States
  if (isLoading) {
    return <PageLoadingState message="Initializing secure assessment environment..." />
  }

  if (isError || !attempt || !currentQuestion) {
    return (
      <PageErrorState
        title="Unable to Load Assessment Session"
        message={
          axios.isAxiosError(error)
            ? (error.response?.data as { message?: string })?.message ||
              "Your attempt may have expired or is not active."
            : error instanceof Error
              ? error.message
              : "Could not initialize examination workspace."
        }
        onRetry={() => refetch()}
        returnTo="/dashboard"
        returnLabel="Return to Dashboard"
      />
    )
  }

  return (
    <div className="flex flex-col h-screen w-screen overflow-hidden bg-background text-foreground select-none font-sans">
      {/* Top Header */}
      <ExamHeader
        title={attempt.title}
        currentIndex={currentIndex}
        totalQuestions={totalQuestionsCount}
        saveStatus={saveStatus}
        remainingSeconds={remainingSeconds}
        formatTimer={formatTimer}
        onSubmitClick={() => setSubmitModalOpen(true)}
        isSubmitting={finalSubmitMutation.isPending}
      />

      {/* Main Workspace Body */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left Vertical Question Rail */}
        <ExamSidebar
          questions={questionsList}
          currentIndex={currentIndex}
          localResponses={localResponses}
          isQuestionAnswered={isQuestionAnswered}
          onSelectQuestion={setCurrentIndex}
        />

        {/* Center / Right Split Area */}
        <div className="flex-1 flex overflow-hidden min-w-0">
          {currentQuestion.questionType === "MCQ" ? (
            <div className="flex-1 grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-border overflow-hidden">
              <ExamStatement
                question={currentQuestion}
                currentIndex={currentIndex}
              />
              <ExamMcqPanel
                question={currentQuestion}
                savedResponse={localResponses[currentQuestion.questionId]}
                onOptionToggle={handleOptionToggle}
                onClearOptions={handleClearOptions}
              />
            </div>
          ) : (
            <ResizablePanelGroup orientation="horizontal" className="flex-1">
              <ResizablePanel defaultSize={45} minSize={25}>
                <ExamStatement
                  question={currentQuestion}
                  currentIndex={currentIndex}
                />
              </ResizablePanel>

              <ResizableHandle withHandle />

              <ResizablePanel defaultSize={55} minSize={30}>
                <ExamCodeWorkspace
                  question={currentQuestion}
                  currentLanguage={currentLanguage}
                  currentCode={currentCode}
                  activeBottomTab={activeBottomTab}
                  isSampleRunning={runSampleMutation.isPending}
                  isCustomRunning={runCustomInputMutation.isPending}
                  isSubmittingCode={submitCodeMutation.isPending}
                  onLanguageChange={handleLanguageChange}
                  onCodeChange={handleCodeChange}
                  onResetStarter={handleResetStarter}
                  onRunTests={handleRunTests}
                  onSubmitCode={handleSubmitCode}
                  bottomPanelContent={
                    <ExamBottomTabs
                      question={currentQuestion}
                      activeTab={activeBottomTab}
                      onTabChange={setActiveBottomTab}
                      isSampleRunning={runSampleMutation.isPending}
                      sampleResult={sampleResult}
                      selectedSampleIndex={selectedSampleIndex}
                      onSelectSampleIndex={setSelectedSampleIndex}
                      customInputText={customInputText}
                      onCustomInputChange={setCustomInputText}
                      isCustomRunning={runCustomInputMutation.isPending}
                      customResult={customResult}
                      onRunCustomInput={handleRunCustomInput}
                      isSubmittingCode={submitCodeMutation.isPending}
                      submitResult={submitResult}
                      savedResponse={localResponses[currentQuestion.questionId]}
                    />
                  }
                />
              </ResizablePanel>
            </ResizablePanelGroup>
          )}
        </div>
      </div>

      {/* Navigation Footer */}
      <ExamNavigationFooter
        currentIndex={currentIndex}
        totalQuestions={totalQuestionsCount}
        answeredCount={answeredCount}
        isMarked={isCurrentQuestionMarked}
        onPrevious={() => setCurrentIndex((idx) => Math.max(0, idx - 1))}
        onNext={() =>
          setCurrentIndex((idx) => Math.min(totalQuestionsCount - 1, idx + 1))
        }
        onToggleMark={handleToggleMark}
        onOpenOverview={() => setOverviewModalOpen(true)}
      />

      {/* Dialogs */}
      <ExamOverviewDialog
        open={overviewModalOpen}
        onOpenChange={setOverviewModalOpen}
        questions={questionsList}
        currentIndex={currentIndex}
        localResponses={localResponses}
        isQuestionAnswered={isQuestionAnswered}
        answeredCount={answeredCount}
        unansweredCount={unansweredCount}
        markedCount={markedCount}
        onSelectQuestion={setCurrentIndex}
      />

      <ExamSubmitDialog
        open={submitModalOpen}
        onOpenChange={setSubmitModalOpen}
        totalQuestions={totalQuestionsCount}
        answeredCount={answeredCount}
        unansweredCount={unansweredCount}
        markedCount={markedCount}
        isSubmitting={finalSubmitMutation.isPending}
        onConfirmSubmit={handleConfirmFinalSubmit}
      />

      <ExamExpiredDialog
        open={timeExpiredModalOpen}
        onViewResults={() =>
          navigate({
            to: "/assessments/$assessmentId/results",
            params: { assessmentId },
          })
        }
      />

      <ExamManualCompletionDialog
        open={Boolean(manualInterventionModal)}
        message={manualInterventionModal}
        onViewResults={() =>
          navigate({
            to: "/assessments/$assessmentId/results",
            params: { assessmentId },
          })
        }
      />
    </div>
  )
}
