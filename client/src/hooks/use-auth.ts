import type { AxiosError } from "axios"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { authService } from "@/services/auth-service"
import { setAccessToken, useAuthStore } from "@/stores/auth-store"
import {
  getAccessTokenCookie,
  setAccessTokenCookie,
  removeAccessTokenCookie,
} from "@/lib/auth-cookie"
import type { ApiResponse } from "@/types/api"
import type {
  AuthResponseData,
  AuthUser,
  UserLoginCredentials,
} from "@/types/auth"

/**
 * Hook to retrieve currently authenticated user profile
 */
export function useMe() {
  const { setAuth, setInitialized } = useAuthStore()

  return useQuery<ApiResponse<AuthUser>, AxiosError<ApiResponse<AuthUser>>>({
    queryKey: ["auth", "me"],
    queryFn: async () => {
      try {
        const user = await authService.getMe()
        const currentToken =
          useAuthStore.getState().accessToken || getAccessTokenCookie()
        setAuth(currentToken, user)
        return {
          success: true,
          statusCode: 200,
          message: "Success",
          data: user,
          timestamp: new Date().toISOString(),
        }
      } catch (err) {
        setInitialized(true)
        throw err
      }
    },
    retry: false,
    staleTime: 5 * 60 * 1000,
  })
}

/**
 * Hook to execute user login mutation (ADMIN or USER)
 */
export function useLogin() {
  const queryClient = useQueryClient()
  const { setAuth } = useAuthStore()

  return useMutation<
    AuthResponseData,
    AxiosError<ApiResponse<unknown>>,
    UserLoginCredentials
  >({
    mutationFn: (credentials) => authService.login(credentials),
    onSuccess: (data) => {
      if (data) {
        setAccessTokenCookie(data.accessToken, data.expiresIn)
        setAccessToken(data.accessToken)
        setAuth(data.accessToken, data.admin)
      }
      queryClient.invalidateQueries({ queryKey: ["auth", "me"] })
    },
  })
}

/**
 * Hook to execute admin logout mutation
 */
export function useLogout() {
  const queryClient = useQueryClient()
  const { logout: clearAuth } = useAuthStore()

  return useMutation<void, AxiosError<ApiResponse<unknown>>>({
    mutationFn: () => authService.logout(),
    onSuccess: () => {
      removeAccessTokenCookie()
      setAccessToken(null)
      clearAuth()
      queryClient.clear()
    },
    onError: () => {
      removeAccessTokenCookie()
      setAccessToken(null)
      clearAuth()
      queryClient.clear()
    },
    onSettled: () => {
      removeAccessTokenCookie()
      setAccessToken(null)
      clearAuth()
      queryClient.clear()
    },
  })
}

/**
 * In-flight promise singleton to deduplicate concurrent session initialization calls
 */
let initSessionPromise: Promise<boolean> | null = null

/**
 * Initialize session by checking access token cookie first,
 * and only falling back to HTTP-only cookie refresh if access token is absent/expired.
 */
export async function initializeAuthSession(): Promise<boolean> {
  if (initSessionPromise) {
    return initSessionPromise
  }

  initSessionPromise = (async () => {
    const { setAuth, setInitialized } = useAuthStore.getState()
    try {
      // 1. Check if a valid, unexpired access token is already preserved in cookie
      const cookieToken = getAccessTokenCookie()
      if (cookieToken) {
        try {
          const admin = await authService.getMe()
          setAuth(cookieToken, admin)
          return true
        } catch {
          // Access token might be invalid or revoked on server; clear cookie and proceed to refresh
          removeAccessTokenCookie()
        }
      }

      // 2. Only if no valid token exists in cookie, attempt /api/auth/refresh
      const refreshData = await authService.refreshAccessToken()
      if (refreshData?.accessToken) {
        setAccessTokenCookie(refreshData.accessToken, refreshData.expiresIn)
        setAccessToken(refreshData.accessToken)
        const admin = await authService.getMe()
        setAuth(refreshData.accessToken, admin)
        return true
      }
    } catch {
      // Session is unauthenticated or cookie missing/expired
    } finally {
      setInitialized(true)
      initSessionPromise = null
    }
    return false
  })()

  return initSessionPromise
}
