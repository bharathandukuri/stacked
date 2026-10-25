import { useEffect, useRef, useState, useCallback } from "react"
import { useAuthStore } from "@/stores/auth-store"

interface UseAssessmentWebSocketOptions {
  assessmentId: string
  attemptId: string
  enabled?: boolean
  onSuperseded?: (message: string) => void
  onManualActivation?: (message: string) => void
  onManualCompletion?: (message: string) => void
}

interface WebSocketStatus {
  isConnected: boolean
  isConnecting: boolean
  isSuperseded: boolean
  supersededMessage: string | null
  manualInterventionMessage: string | null
  lastHeartbeatTime: Date | null
  connectionId: string | null
}

export function useAssessmentWebSocket({
  assessmentId,
  attemptId,
  enabled = true,
  onSuperseded,
  onManualActivation,
  onManualCompletion,
}: UseAssessmentWebSocketOptions) {
  const accessToken = useAuthStore((s) => s.accessToken)
  const [status, setStatus] = useState<WebSocketStatus>({
    isConnected: false,
    isConnecting: false,
    isSuperseded: false,
    supersededMessage: null,
    manualInterventionMessage: null,
    lastHeartbeatTime: null,
    connectionId: null,
  })

  // Store latest callbacks in refs to avoid reconnection loops on parent re-renders
  const onSupersededRef = useRef(onSuperseded)
  const onManualActivationRef = useRef(onManualActivation)
  const onManualCompletionRef = useRef(onManualCompletion)

  useEffect(() => {
    onSupersededRef.current = onSuperseded
    onManualActivationRef.current = onManualActivation
    onManualCompletionRef.current = onManualCompletion
  })

  const wsRef = useRef<WebSocket | null>(null)
  const heartbeatTimerRef = useRef<number | null>(null)
  const reconnectTimeoutRef = useRef<number | null>(null)
  const reconnectAttemptsRef = useRef(0)
  const isSupersededRef = useRef(false)
  const unmountedRef = useRef(false)
  const activeParamsRef = useRef<string | null>(null)

  const clearTimers = useCallback(() => {
    if (heartbeatTimerRef.current) {
      clearInterval(heartbeatTimerRef.current)
      heartbeatTimerRef.current = null
    }
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
      reconnectTimeoutRef.current = null
    }
  }, [])

  const connect = useCallback(() => {
    if (
      !enabled ||
      !accessToken ||
      !assessmentId ||
      !attemptId ||
      unmountedRef.current ||
      isSupersededRef.current
    ) {
      return
    }

    const connectionKey = `${assessmentId}:${attemptId}:${accessToken}`

    // If socket is already open or connecting to this exact session, do not duplicate
    if (
      wsRef.current &&
      (wsRef.current.readyState === WebSocket.OPEN ||
        wsRef.current.readyState === WebSocket.CONNECTING) &&
      activeParamsRef.current === connectionKey
    ) {
      return
    }

    clearTimers()

    // Close any previous socket before creating a new one
    if (wsRef.current) {
      const prevWs = wsRef.current
      wsRef.current = null
      try {
        prevWs.close(1000, "Opening new connection")
      } catch {
        // ignore close errors
      }
    }

    activeParamsRef.current = connectionKey

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:"
    const host = window.location.host
    const wsUrl = `${protocol}//${host}/ws/assessment?token=${encodeURIComponent(
      accessToken
    )}&assessmentId=${encodeURIComponent(assessmentId)}&attemptId=${encodeURIComponent(
      attemptId
    )}`

    setStatus((prev) => ({ ...prev, isConnecting: true }))

    try {
      const ws = new WebSocket(wsUrl)
      wsRef.current = ws

      ws.onopen = () => {
        if (unmountedRef.current || isSupersededRef.current) {
          ws.close(1000, "Unmounted or superseded")
          return
        }
        reconnectAttemptsRef.current = 0
        setStatus((prev) => ({
          ...prev,
          isConnected: true,
          isConnecting: false,
          isSuperseded: false,
        }))

        // Start 15s heartbeat interval
        heartbeatTimerRef.current = window.setInterval(() => {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ type: "HEARTBEAT" }))
          }
        }, 15000)
      }

      ws.onmessage = (event) => {
        try {
          const payload = JSON.parse(event.data)
          if (payload.type === "CONNECTION_ACK") {
            setStatus((prev) => ({
              ...prev,
              connectionId: payload.connectionId,
              isConnected: true,
            }))
          } else if (payload.type === "HEARTBEAT_ACK") {
            setStatus((prev) => ({
              ...prev,
              lastHeartbeatTime: new Date(),
            }))
          } else if (payload.type === "SESSION_SUPERSEDED") {
            isSupersededRef.current = true
            clearTimers()
            const msg =
              payload.message ||
              "Your test session was opened in another window or tab. This session is now inactive."
            setStatus((prev) => ({
              ...prev,
              isConnected: false,
              isConnecting: false,
              isSuperseded: true,
              supersededMessage: msg,
            }))
            onSupersededRef.current?.(msg)
          } else if (payload.type === "ASSESSMENT_MANUALLY_ACTIVATED") {
            const msg =
              payload.message ||
              "The administrator has manually activated this assessment."
            setStatus((prev) => ({
              ...prev,
              manualInterventionMessage: msg,
            }))
            onManualActivationRef.current?.(msg)
          } else if (payload.type === "ASSESSMENT_MANUALLY_COMPLETED") {
            const msg =
              payload.message ||
              "The administrator has manually completed this assessment."
            setStatus((prev) => ({
              ...prev,
              manualInterventionMessage: msg,
            }))
            onManualCompletionRef.current?.(msg)
          }
        } catch {
          // ignore non-json messages
        }
      }

      ws.onclose = (event) => {
        clearTimers()
        if (wsRef.current === ws) {
          wsRef.current = null
        }

        if (unmountedRef.current) return

        const wasSuperseded = event.code === 4009 || isSupersededRef.current
        if (wasSuperseded) {
          isSupersededRef.current = true
        }

        setStatus((prev) => ({
          ...prev,
          isConnected: false,
          isConnecting: false,
          isSuperseded: wasSuperseded || prev.isSuperseded,
        }))

        // Do not auto-reconnect if normal close (1000), going away (1001), or superseded (4009)
        const isCleanClose =
          event.code === 1000 || event.code === 1001 || wasSuperseded

        if (!isCleanClose && reconnectAttemptsRef.current < 5) {
          const delay = Math.min(1000 * 2 ** reconnectAttemptsRef.current, 10000)
          reconnectAttemptsRef.current += 1
          reconnectTimeoutRef.current = window.setTimeout(() => {
            if (!unmountedRef.current && !isSupersededRef.current) {
              connect()
            }
          }, delay)
        }
      }

      ws.onerror = () => {
        // onclose will handle cleanup and status
      }
    } catch {
      setStatus((prev) => ({ ...prev, isConnecting: false, isConnected: false }))
    }
  }, [accessToken, assessmentId, attemptId, enabled, clearTimers])

  useEffect(() => {
    unmountedRef.current = false
    isSupersededRef.current = false

    if (enabled && accessToken && assessmentId && attemptId) {
      connect()
    }

    return () => {
      unmountedRef.current = true
      clearTimers()
      if (wsRef.current) {
        const ws = wsRef.current
        wsRef.current = null
        try {
          ws.close(1000, "Component unmounted")
        } catch {
          // ignore close error
        }
      }
      activeParamsRef.current = null
    }
  }, [enabled, accessToken, assessmentId, attemptId, connect, clearTimers])

  return status
}
