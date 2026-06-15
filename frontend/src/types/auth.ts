export interface UserInfo {
  id: number
  username: string
  employeeNo: string
  role: 'admin' | 'ops' | 'finance' | 'boss'
  scopeOrgId?: number
  lastLoginAt?: string
  permissions?: string[]
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

export type Role = 'admin' | 'ops' | 'finance' | 'boss'

/** 路由所需的角色级别: admin=1, ops=2, finance=3, boss=4 */
export const ROLE_LEVEL: Record<Role, number> = {
  admin: 1,
  ops: 2,
  finance: 3,
  boss: 4,
}

/** 角色中文显示名 */
export const ROLE_LABEL: Record<Role, string> = {
  admin: '管理员',
  ops: '运维',
  finance: '财务',
  boss: '领导',
}
