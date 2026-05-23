import { ApiGet, ApiPut, ApiDelete } from './request'

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
  status: number
  updatedAt: string
}

export const userApi = {
  getByOrg: (orgId: number) => ApiGet<UserVO[]>(`/users/by-org/${orgId}`),
  getAll: () => ApiGet<UserVO[]>('/users'),
  updateUsername: (employeeId: number, username: string) =>
    ApiPut(`/users/${employeeId}/username`, { username }),
  updateDepartment: (employeeId: number, orgId: number) =>
    ApiPut(`/users/${employeeId}/department`, { orgId }),
  resetPassword: (employeeId: number) => ApiPut(`/users/${employeeId}/reset-password`),
  disable: (employeeId: number) => ApiPut(`/users/${employeeId}/disable`),
  enable: (employeeId: number) => ApiPut(`/users/${employeeId}/enable`),
  delete: (employeeId: number) => ApiDelete(`/users/${employeeId}`),
  updateRole: (employeeId: number, roleId: number) =>
    ApiPut(`/users/${employeeId}/role`, { roleId }),
}
