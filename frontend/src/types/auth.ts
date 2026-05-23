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
