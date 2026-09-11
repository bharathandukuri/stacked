import API from "@/lib/api-client"
import type { ApiResponse } from "@/types/api"
import type {
  AdminLoginCredentials,
  AdminUser,
  AuthResponseData,
  RefreshResponseData,
} from "@/types/auth"

export const authService = {
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
   * Fetch the currently authenticated admin profile.
   */
  async getMe(): Promise<AdminUser> {
    const response = await API.get<ApiResponse<AdminUser>>("/auth/me")
    return response.data.data
  },
}
