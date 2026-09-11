import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios"
import type { ApiError, ApiResponse } from "@/types/api"
import { useAuthStore } from "@/stores/auth-store"

/**
 * Default API Base URL. Uses environment variable or defaults to /api.
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api"

/**
 * Configured Axios instance with production defaults
 */
export const API: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  withCredentials: true, // Enables sending and receiving HTTP-only cookies
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
})

/**
 * Normalizes Axios errors into a unified ApiError structure
 */
function normalizeApiError(error: AxiosError<ApiResponse<unknown>>): ApiError {
  if (error.response?.data?.error) {
    return error.response.data.error
  }

  if (error.response?.data?.message) {
    return {
      statusCode: error.response.status,
      status: error.response.statusText || "ERROR",
      errorCode: "API_ERROR",
      message: error.response.data.message,
      timestamp: new Date().toISOString(),
    }
  }

  if (error.code === "ECONNABORTED") {
    return {
      statusCode: 408,
      status: "REQUEST_TIMEOUT",
      errorCode: "TIMEOUT",
      message:
        "The server took too long to respond. Please check your connection.",
      timestamp: new Date().toISOString(),
    }
  }

  if (!error.response) {
    return {
      statusCode: 0,
      status: "NETWORK_ERROR",
      errorCode: "NETWORK_UNAVAILABLE",
      message:
        "Unable to reach the server. Ensure the backend and Docker containers are running.",
      timestamp: new Date().toISOString(),
    }
  }

  return {
    statusCode: error.response.status,
    status: error.response.statusText || "UNKNOWN_ERROR",
    errorCode: "SERVER_ERROR",
    message: error.message || "An unexpected error occurred.",
    timestamp: new Date().toISOString(),
  }
}

/**
 * Request Interceptor:
 * Automatically attaches the Bearer token from the auth store.
 */
API.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken
    if (token && !config.headers.get("Authorization")) {
      config.headers.set("Authorization", `Bearer ${token}`)
    }
    return config
  },
  (error) => Promise.reject(error)
)

/**
 * Token Refresh Queue State
 */
let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error)
    } else if (token) {
      promise.resolve(token)
    }
  })
  failedQueue = []
}

/**
 * Response Interceptor:
 * - Unwraps response
 * - Handles token refresh mutex/queue on 401
 * - Normalizes errors into ApiError
 */
API.interceptors.response.use(
  (response: AxiosResponse) => {
    if (import.meta.env.DEV) {
      console.debug(
        `[API Response] ${response.status} ${response.config.url}`,
        response.data
      )
    }
    return response
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean
    }

    const isAuthEndpoint =
      originalRequest?.url?.includes("/auth/admin/login") ||
      originalRequest?.url?.includes("/auth/refresh")

    // Attempt token refresh on 401 for protected endpoints
    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !isAuthEndpoint
    ) {
      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then((newToken) => {
            originalRequest.headers.set("Authorization", `Bearer ${newToken}`)
            return API(originalRequest)
          })
          .catch((err) => Promise.reject(err))
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const refreshResponse = await axios.post<
          ApiResponse<{ accessToken: string }>
        >(`${BASE_URL}/auth/refresh`, {}, { withCredentials: true })

        const newAccessToken = refreshResponse.data.data.accessToken
        useAuthStore.getState().actions.setAccessToken(newAccessToken)

        processQueue(null, newAccessToken)
        originalRequest.headers.set("Authorization", `Bearer ${newAccessToken}`)
        return API(originalRequest)
      } catch (refreshErr) {
        processQueue(refreshErr, null)
        useAuthStore.getState().actions.clearAuth()
        window.dispatchEvent(new CustomEvent("auth:unauthorized"))
        return Promise.reject(normalizeApiError(error))
      } finally {
        isRefreshing = false
      }
    }

    const apiError = normalizeApiError(error)

    if (error.response?.status === 401 && isAuthEndpoint) {
      useAuthStore.getState().actions.clearAuth()
    }

    if (import.meta.env.DEV) {
      console.error(
        `[API Error] ${apiError.statusCode} [${apiError.errorCode}]: ${apiError.message}`,
        apiError
      )
    }

    return Promise.reject(apiError)
  }
)

export default API
