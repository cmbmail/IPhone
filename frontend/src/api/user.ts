import { request } from './request'

export interface UserVO {
  id: number
  employeeId: number
  name: string
  username: string
  orgId: number
  orgName: string
  roleId: number | null
  roleName: string
  roleCode: string | null
  status: string
  updatedAt: string
}

export const userApi = {
  getByOrg: (orgId: number) => request.get<{ data: UserVO[] }>(`/users/by-org/${orgId}`),
  getAll: () => request.get<{ data: UserVO[] }>('/users'),
  updateUsername: (employeeId: number, username: string) => request.put(`/users/${employeeId}/username`, { username }),
  updateDepartment: (employeeId: number, orgId: number) => request.put(`/users/${employeeId}/department`, { orgId }),
  resetPassword: (employeeId: number) => request.put(`/users/${employeeId}/reset-password`),
  disable: (employeeId: number) => request.put(`/users/${employeeId}/disable`),
  enable: (employeeId: number) => request.put(`/users/${employeeId}/enable`),
  delete: (employeeId: number) => request.delete(`/users/${employeeId}`),
  updateRole: (employeeId: number, roleId: number) => request.put(`/users/${employeeId}/role`, { roleId }),
}
