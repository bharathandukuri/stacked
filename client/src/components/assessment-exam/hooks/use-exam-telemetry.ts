import { useEffect, useRef, useCallback } from "react"
import { useRecordTelemetry } from "@/hooks/use-assessments"
import type {
  ClientTelemetryEventDto,
  AssessmentAuditEventType,
} from "@/types/assessment"

interface UseExamTelemetryOptions {
  assessmentId: string
  attemptId: string | undefined
  currentQuestionId: string | undefined
  enabled: boolean
}

export function useExamTelemetry({
  assessmentId,
  attemptId,
  currentQuestionId,
  enabled,
}: UseExamTelemetryOptions) {
  const recordTelemetryMutation = useRecordTelemetry(assessmentId)
  const telemetryBufferRef = useRef<ClientTelemetryEventDto[]>([])

  const flushTelemetry = useCallback(() => {
    if (!enabled || !attemptId || telemetryBufferRef.current.length === 0) return

    const batch = [...telemetryBufferRef.current]
    telemetryBufferRef.current = []

    recordTelemetryMutation.mutate(batch, {
      onError: () => {
        // Re-queue events on transient network failure (up to 50 max)
        telemetryBufferRef.current = [
          ...batch.slice(-30),
          ...telemetryBufferRef.current,
        ].slice(-50)
      },
    })
  }, [enabled, attemptId, recordTelemetryMutation])

  const recordEvent = useCallback(
    (
      eventType: AssessmentAuditEventType,
      questionId?: string,
      details?: Record<string, unknown>
    ) => {
      if (!enabled || !attemptId) return

      const event: ClientTelemetryEventDto = {
        eventType,
        questionId: questionId ?? currentQuestionId,
        clientTimestamp: new Date().toISOString(),
        metadata: details,
      }

      telemetryBufferRef.current.push(event)

      // Immediate flush for security-critical actions
      if (
        eventType === "WINDOW_BLUR" ||
        eventType === "WINDOW_FOCUS" ||
        eventType === "FINAL_SUBMISSION"
      ) {
        flushTelemetry()
      }
    },
    [enabled, attemptId, currentQuestionId, flushTelemetry]
  )

  // Listen for window blur and focus
  useEffect(() => {
    if (!enabled) return

    const handleBlur = () => {
      recordEvent("WINDOW_BLUR", undefined, {
        screenX: window.screenX,
        screenY: window.screenY,
      })
    }

    const handleFocus = () => {
      recordEvent("WINDOW_FOCUS")
    }

    window.addEventListener("blur", handleBlur)
    window.addEventListener("focus", handleFocus)

    // Flush telemetry periodically every 15 seconds
    const interval = setInterval(flushTelemetry, 15000)

    return () => {
      window.removeEventListener("blur", handleBlur)
      window.removeEventListener("focus", handleFocus)
      clearInterval(interval)
      flushTelemetry()
    }
  }, [enabled, recordEvent, flushTelemetry])

  return {
    recordEvent,
    flushTelemetry,
  }
}
