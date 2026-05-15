export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  expiresIn: number
  user: UserInfo
}

export interface UserInfo {
  id: number
  username: string
  employeeNo: string
  role: 'admin' | 'ops' | 'finance' | 'boss'
  scopeOrgId?: number
  lastLoginAt?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

export interface ResetPasswordRequest {
  employeeNo: string
  newPassword: string
}
