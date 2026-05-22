export interface SysUser {
  id: number
  username: string
  employeeNo: string
  role: number
  scopeOrgId?: number
  status: 0 | 1
  loginFailCount: number
  lockedUntil?: string
  passwordChangedAt?: string
  lastLoginAt?: string
  createdAt: string
  updatedAt: string
}

export type UserRole = 1 | 2 | 3 | 4

export interface CreateUserRequest {
  username: string
  password: string
  employeeNo: string
  role: number
  scopeOrgId?: number
}

export interface UpdateUserRequest {
  role?: UserRole
  scopeOrgId?: number
  status?: 0 | 1
}
