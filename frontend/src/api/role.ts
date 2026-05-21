import { request } from './request'
import type { SysRole, SysPermission, CreateRoleDTO, UpdateRoleDTO } from '@/types/role'

export const roleApi = {
  getAll: () =>
    request.get<SysRole[]>('/roles'),

  getActive: () =>
    request.get<SysRole[]>('/roles/active'),

  getById: (id: number) =>
    request.get<SysRole>(`/roles/${id}`),

  getPermissions: (roleId: number) =>
    request.get<SysPermission[]>(`/roles/${roleId}/permissions`),

  getAllPermissions: () =>
    request.get<SysPermission[]>('/roles/permissions/all'),

  getPermissionsByModule: () =>
    request.get<Record<string, SysPermission[]>>('/roles/permissions/modules'),

  getUserCount: (roleId: number) =>
    request.get<number>(`/roles/${roleId}/user-count`),

  create: (data: CreateRoleDTO) =>
    request.post<SysRole>('/roles', data),

  update: (id: number, data: UpdateRoleDTO) =>
    request.put<SysRole>(`/roles/${id}`, data),

  delete: (id: number) =>
    request.delete(`/roles/${id}`),
}
