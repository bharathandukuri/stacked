import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosResponse,
} from "axios"
import type { ApiError, ApiResponse } from "@/types/api"

/**
 * Default API Base URL. Uses environment variable or defaults to /api/v1.
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api/v1"

/**
 * Configured Axios instance with production defaults
 */
export const API: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
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
 * Response Interceptor
 * - Unwraps Axios response
 * - Emits auth:unauthorized event on 401
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
  (error: AxiosError<ApiResponse<unknown>>) => {
    const apiError = normalizeApiError(error)

    if (error.response?.status === 401) {
      localStorage.removeItem("auth_token")
      window.dispatchEvent(new CustomEvent("auth:unauthorized"))
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
