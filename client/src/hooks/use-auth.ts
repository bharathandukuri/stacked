import type { AxiosError } from "axios"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { authService } from "@/services/auth-service"
import { setAccessToken, useAuthStore } from "@/stores/auth-store"
import type { ApiResponse } from "@/types/api"
import type {
  AdminLoginCredentials,
  AdminUser,
  AuthResponseData,
} from "@/types/auth"

/**
 * Hook to retrieve currently authenticated admin profile
 */
export function useMe() {
  const { setAuth, setInitialized } = useAuthStore()

  return useQuery<ApiResponse<AdminUser>, AxiosError<ApiResponse<AdminUser>>>({
    queryKey: ["auth", "me"],
    queryFn: async () => {
      try {
        const admin = await authService.getMe()
        const currentToken = useAuthStore.getState().accessToken
        setAuth(currentToken, admin)
        return {
          success: true,
          statusCode: 200,
          message: "Success",
          data: admin,
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
 * Hook to execute admin login mutation
 */
export function useLogin() {
  const queryClient = useQueryClient()
  const { setAuth } = useAuthStore()

  return useMutation<
    AuthResponseData,
    AxiosError<ApiResponse<unknown>>,
    AdminLoginCredentials
  >({
    mutationFn: (credentials) => authService.loginAdmin(credentials),
    onSuccess: (data) => {
      if (data) {
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
      setAccessToken(null)
      clearAuth()
      queryClient.clear()
    },
    onError: () => {
      setAccessToken(null)
      clearAuth()
      queryClient.clear()
    },
    onSettled: () => {
      setAccessToken(null)
      clearAuth()
      queryClient.clear()
    },
  })
}

/**
 * Initialize session by checking HTTP-only cookie refresh or me
 */
export async function initializeAuthSession(): Promise<boolean> {
  const { setAuth, setInitialized } = useAuthStore.getState()
  try {
    const refreshData = await authService.refreshAccessToken()
    if (refreshData?.accessToken) {
      setAccessToken(refreshData.accessToken)
      const admin = await authService.getMe()
      setAuth(refreshData.accessToken, admin)
      return true
    }
  } catch {
    // Session is unauthenticated or cookie missing/expired
  } finally {
    setInitialized(true)
  }
  return false
}
