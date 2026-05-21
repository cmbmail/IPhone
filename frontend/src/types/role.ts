export interface SysRole {
  id: number
  name: string
  code: string
  description?: string
  status: 'active' | 'inactive'
  is_system: boolean
  created_by: string
  created_at: string
  updated_by: string
  updated_at: string
}

export interface SysPermission {
  id: number
  code: string
  name: string
  module: string
  sort_order: number
  created_at: string
}

export interface CreateRoleDTO {
  name: string
  code: string
  description?: string
  permission_ids?: number[]
}

export interface UpdateRoleDTO {
  name?: string
  description?: string
  status?: string
  permission_ids?: number[]
}
