import API from "@/lib/api-client"
import type { ApiResponse } from "@/types/api"
import type {
  AdminLoginCredentials,
  AuthResponseData,
  AuthUser,
  RefreshResponseData,
  UserLoginCredentials,
} from "@/types/auth"

export const authService = {
  /**
   * Log in any user (ADMIN or USER) with email and password.
   * On success, the backend sets the HTTP-only refresh token cookie and returns the access token.
   */
  async login(
    credentials: UserLoginCredentials
  ): Promise<AuthResponseData> {
    const response = await API.post<ApiResponse<AuthResponseData>>(
      "/auth/login",
      credentials
    )
    return response.data.data
  },

  /**
   * Log in an administrator with email and password.
   * On success, the backend sets the HTTP-only refresh token cookie and returns the access token.
   */
  async loginAdmin(
    credentials: AdminLoginCredentials
  ): Promise<AuthResponseData> {
    const response = await API.post<ApiResponse<AuthResponseData>>(
      "/auth/admin/login",
      credentials
    )
    return response.data.data
  },

  /**
   * Manually refresh the access token using the HTTP-only refresh token cookie.
   */
  async refreshAccessToken(): Promise<RefreshResponseData> {
    const response =
      await API.post<ApiResponse<RefreshResponseData>>("/auth/refresh")
    return response.data.data
  },

  /**
   * Log out the currently authenticated admin, invalidating the session in Redis
   * and clearing the HTTP-only cookie.
   */
  async logout(): Promise<void> {
    await API.post<ApiResponse<void>>("/auth/logout")
  },

  /**
   * Fetch the currently authenticated user profile.
   */
  async getMe(): Promise<AuthUser> {
    const response = await API.get<ApiResponse<AuthUser>>("/auth/me")
    return response.data.data
  },
}
