export type Role = "ADMIN" | "USER"

export interface AdminUser {
  id: string
  name: string
  email: string
  role: Role
  createdAt: string
  updatedAt: string
}

export interface UserItem {
  id: string
  name: string
  email: string
  role: Role
  createdAt: string
  updatedAt: string
}

export interface CreateUserPayload {
  name: string
  email: string
  password: string
  role: Role
}

export interface UpdateUserPayload {
  name: string
  email: string
  role: Role
  password?: string
}

export interface UserFilterParams {
  search?: string
  role?: Role | ""
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: "ASC" | "DESC"
}

export interface AdminLoginCredentials {
  email: string
  password: string
}

export type UserLoginCredentials = AdminLoginCredentials
export type AuthUser = AdminUser

export interface AuthResponseData {
  accessToken: string
  tokenType: string
  expiresIn: number
  admin: AdminUser
}

export interface RefreshResponseData {
  accessToken: string
  tokenType: string
  expiresIn: number
}
