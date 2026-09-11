export type Role = "ADMIN"

export interface AdminUser {
  id: string
  name: string
  email: string
  role: Role
  createdAt: string
  updatedAt: string
}

export interface AdminLoginCredentials {
  email: string
  password: string
}

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
