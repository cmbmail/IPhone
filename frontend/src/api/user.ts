import { request } from './request'

export interface UserVO {
  id: number
  employee_id: number
  name: string
  username: string
  org_id: number
  org_name: string
  role_id: number | null
  role_name: string
  role_code: string | null
  status: string
  updated_at: string
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
