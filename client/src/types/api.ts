/**
 * Unified API Response structure returned from backend
 */
export interface ApiResponse<T = unknown> {
  success: boolean
  statusCode: number
  message: string
  data: T
  error?: ApiError
  timestamp: string
}

/**
 * Standard pagination response wrapper matching Spring Data Page
 */
export interface PageResponse<T = unknown> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first?: boolean
  last?: boolean
  empty?: boolean
}

/**
 * Structured API Error payload returned from backend GlobalExceptionHandler
 */
export interface ApiError {
  statusCode: number
  status: string
  errorCode: string
  message: string
  path?: string
  timestamp: string
  validationErrors?: Record<string, string>
  details?: unknown
}
