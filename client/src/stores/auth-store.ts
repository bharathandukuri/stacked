import { create } from "zustand"
import type { AdminUser } from "@/types/auth"

interface AuthState {
  admin: AdminUser | null
  accessToken: string | null
  isAuthenticated: boolean
  isLoading: boolean
  isInitialized: boolean
}

interface AuthActions {
  setAuth: (accessToken: string | null, admin: AdminUser | null) => void
  setAccessToken: (accessToken: string | null) => void
  setLoading: (isLoading: boolean) => void
  setInitialized: (isInitialized: boolean) => void
  logout: () => void
  clearAuth: () => void
}

export type AuthStore = AuthState &
  AuthActions & {
    actions: AuthActions
  }

export const useAuthStore = create<AuthStore>((set) => {
  const actions: AuthActions = {
    setAuth: (accessToken, admin) =>
      set({
        admin,
        accessToken,
        isAuthenticated: Boolean(accessToken && admin),
        isLoading: false,
        isInitialized: true,
      }),

    setAccessToken: (accessToken) =>
      set({
        accessToken,
        isAuthenticated: Boolean(accessToken),
      }),

    setLoading: (isLoading) =>
      set({
        isLoading,
      }),

    setInitialized: (isInitialized) =>
      set({
        isInitialized,
        isLoading: false,
      }),

    logout: () =>
      set({
        admin: null,
        accessToken: null,
        isAuthenticated: false,
        isLoading: false,
        isInitialized: true,
      }),

    clearAuth: () =>
      set({
        admin: null,
        accessToken: null,
        isAuthenticated: false,
        isLoading: false,
        isInitialized: true,
      }),
  }

  return {
    admin: null,
    accessToken: null,
    isAuthenticated: false,
    isLoading: true,
    isInitialized: false,
    ...actions,
    actions,
  }
})

// Standalone setter helper
export const setAccessToken = (token: string | null) => {
  useAuthStore.getState().setAccessToken(token)
}

// Atomic selectors for optimal re-render performance
export const useAdmin = () => useAuthStore((state) => state.admin)
export const useAccessToken = () => useAuthStore((state) => state.accessToken)
export const useIsAuthenticated = () =>
  useAuthStore((state) => state.isAuthenticated)
export const useAuthLoading = () => useAuthStore((state) => state.isLoading)
export const useIsInitialized = () =>
  useAuthStore((state) => state.isInitialized)
export const useAuthActions = () => useAuthStore((state) => state.actions)
