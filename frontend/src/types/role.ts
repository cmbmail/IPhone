export interface SysRole {
  id: number
  name: string
  code: string
  description?: string
  status: 'active' | 'inactive'
  isSystem: boolean
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
}

export interface SysPermission {
  id: number
  code: string
  name: string
  module: string
  sortOrder: number
  createdAt: string
}

export interface CreateRoleDTO {
  name: string
  code: string
  description?: string
  permissionIds?: number[]
}

export interface UpdateRoleDTO {
  name?: string
  description?: string
  status?: string
  permissionIds?: number[]
}
