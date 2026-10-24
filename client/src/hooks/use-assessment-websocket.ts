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

  const wsRef = useRef<WebSocket | null>(null)
  const heartbeatTimerRef = useRef<number | null>(null)
  const reconnectTimeoutRef = useRef<number | null>(null)
  const reconnectAttemptsRef = useRef(0)
  const unmountedRef = useRef(false)
  const connectRef = useRef<() => void>(() => {})

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
    if (!enabled || !accessToken || !assessmentId || !attemptId || unmountedRef.current) {
      return
    }

    clearTimers()

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
        if (unmountedRef.current) {
          ws.close()
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
            const msg =
              payload.message ||
              "Your test session was opened in another window or tab. This session is now inactive."
            setStatus((prev) => ({
              ...prev,
              isConnected: false,
              isSuperseded: true,
              supersededMessage: msg,
            }))
            onSuperseded?.(msg)
          } else if (payload.type === "ASSESSMENT_MANUALLY_ACTIVATED") {
            const msg =
              payload.message ||
              "The administrator has manually activated this assessment."
            setStatus((prev) => ({
              ...prev,
              manualInterventionMessage: msg,
            }))
            onManualActivation?.(msg)
          } else if (payload.type === "ASSESSMENT_MANUALLY_COMPLETED") {
            const msg =
              payload.message ||
              "The administrator has manually completed this assessment."
            setStatus((prev) => ({
              ...prev,
              manualInterventionMessage: msg,
            }))
            onManualCompletion?.(msg)
          }
        } catch {
          // ignore non-json messages
        }
      }

      ws.onclose = (event) => {
        clearTimers()
        if (unmountedRef.current) return

        const wasSuperseded = event.code === 4009
        setStatus((prev) => ({
          ...prev,
          isConnected: false,
          isConnecting: false,
          isSuperseded: wasSuperseded || prev.isSuperseded,
        }))

        // Attempt automatic reconnection if not normal close and not superseded
        if (!wasSuperseded && event.code !== 1000 && reconnectAttemptsRef.current < 5) {
          const delay = Math.min(1000 * 2 ** reconnectAttemptsRef.current, 10000)
          reconnectAttemptsRef.current += 1
          reconnectTimeoutRef.current = window.setTimeout(() => {
            connectRef.current()
          }, delay)
        }
      }

      ws.onerror = () => {
        // ws.onclose will fire after onerror
      }
    } catch {
      setStatus((prev) => ({ ...prev, isConnecting: false, isConnected: false }))
    }
  }, [accessToken, assessmentId, attemptId, enabled, onSuperseded, onManualActivation, onManualCompletion, clearTimers])

  useEffect(() => {
    connectRef.current = connect
  }, [connect])

  useEffect(() => {
    unmountedRef.current = false
    connect()

    return () => {
      unmountedRef.current = true
      clearTimers()
      if (wsRef.current) {
        wsRef.current.close(1000, "Component unmounted")
        wsRef.current = null
      }
    }
  }, [connect, clearTimers])

  return status
}

