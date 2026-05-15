export interface SysUser {
  id: number
  username: string
  employeeNo: string
  role: UserRole
  scopeOrgId?: number
  status: 'active' | 'inactive'
  loginFailCount: number
  lockedUntil?: string
  passwordChangedAt?: string
  lastLoginAt?: string
  createdAt: string
  updatedAt: string
}

export type UserRole = 'admin' | 'ops' | 'finance' | 'boss'

export interface CreateUserRequest {
  username: string
  password: string
  employeeNo: string
  role: UserRole
  scopeOrgId?: number
}

export interface UpdateUserRequest {
  role?: UserRole
  scopeOrgId?: number
  status?: 'active' | 'inactive'
}
