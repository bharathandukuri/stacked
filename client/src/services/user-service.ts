import API from "@/lib/api-client"
import type { ApiResponse } from "@/types/api"
import type {
  CreateUserPayload,
  UpdateUserPayload,
  UserFilterParams,
  UserItem,
} from "@/types/auth"
import type { PaginatedResponse } from "@/services/coding-problem-service"

export const userService = {
  /**
   * Fetch paginated and filtered list of users.
   */
  async getUsers(
    params: UserFilterParams = {}
  ): Promise<PaginatedResponse<UserItem>> {
    const query = new URLSearchParams()
    if (params.search && params.search.trim()) {
      query.set("search", params.search.trim())
    }
    if (params.role) {
      query.set("role", params.role)
    }
    if (params.page !== undefined) {
      query.set("page", String(params.page))
    }
    if (params.size !== undefined) {
      query.set("size", String(params.size))
    }
    if (params.sortBy) {
      query.set("sortBy", params.sortBy)
    }
    if (params.sortDirection) {
      query.set("sortDirection", params.sortDirection)
    }

    const queryString = query.toString()
    const url = `/admin/users${queryString ? `?${queryString}` : ""}`
    const res = await API.get<ApiResponse<PaginatedResponse<UserItem>>>(url)
    return res.data.data
  },

  /**
   * Get single user by ID.
   */
  async getUser(id: string): Promise<UserItem> {
    const res = await API.get<ApiResponse<UserItem>>(
      `/admin/users/${encodeURIComponent(id)}`
    )
    return res.data.data
  },

  /**
   * Create a new user (admin or user).
   */
  async createUser(payload: CreateUserPayload): Promise<UserItem> {
    const res = await API.post<ApiResponse<UserItem>>("/admin/users", payload)
    return res.data.data
  },

  /**
   * Update existing user details.
   */
  async updateUser(id: string, payload: UpdateUserPayload): Promise<UserItem> {
    const res = await API.put<ApiResponse<UserItem>>(
      `/admin/users/${encodeURIComponent(id)}`,
      payload
    )
    return res.data.data
  },

  /**
   * Delete an existing user (cannot delete own account).
   */
  async deleteUser(id: string): Promise<void> {
    await API.delete<ApiResponse<void>>(`/admin/users/${encodeURIComponent(id)}`)
  },
}

