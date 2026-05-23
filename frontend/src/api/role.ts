import { ApiGet, ApiPost, ApiPut, ApiDelete } from './request'
import type { SysRole, SysPermission, CreateRoleDTO, UpdateRoleDTO } from '@/types/role'

export const roleApi = {
  getAll: () => ApiGet<SysRole[]>('/roles'),

  getActive: () => ApiGet<SysRole[]>('/roles/active'),

  getById: (id: number) => ApiGet<SysRole>(`/roles/${id}`),

  getPermissions: (roleId: number) => ApiGet<SysPermission[]>(`/roles/${roleId}/permissions`),

  getAllPermissions: () => ApiGet<SysPermission[]>('/roles/permissions/all'),

  getPermissionsByModule: () =>
    ApiGet<Record<string, SysPermission[]>>('/roles/permissions/modules'),

  getUserCount: (roleId: number) => ApiGet<number>(`/roles/${roleId}/user-count`),

  create: (data: CreateRoleDTO) => ApiPost<SysRole>('/roles', data),

  update: (id: number, data: UpdateRoleDTO) => ApiPut<SysRole>(`/roles/${id}`, data),

  delete: (id: number) => ApiDelete(`/roles/${id}`),
}
